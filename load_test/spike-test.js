import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";
import {
  BASE_URL,
  SHOW_ID,
  ALL_SEAT_IDS,
  getUserForVU,
  login,
} from "./config.js";

// ─── Custom Metrics ───────────────────────────────────────────────────────────
const checkoutLatency = new Trend("seatora_checkout_duration", true);
const lockLatency = new Trend("seatora_lock_duration", true);
const loginLatency = new Trend("seatora_login_duration", true);
const errorRate = new Rate("seatora_error_rate");
const successfulBookings = new Counter("seatora_successful_bookings");
const lockConflicts = new Counter("seatora_lock_conflicts");
const checkoutErrors = new Counter("seatora_checkout_errors");

export const options = {
  stages: [
    { duration: "5s", target: 50 },   // Warm up: ramp to 50 VUs
    { duration: "15s", target: 100 }, // Ramp to 100 VUs
    { duration: "20s", target: 100 }, // Hold at 100 VUs
    { duration: "15s", target: 200 }, // Spike to full 200 concurrent VUs!
    { duration: "30s", target: 200 }, // Hold peak at 200 VUs
    { duration: "15s", target: 0 },   // Cool down ramp to 0
  ],
  thresholds: {
    seatora_checkout_duration: ["p(95)<3000"], // 95% of checkouts under 3s
    seatora_lock_duration: ["p(95)<1000"],     // 95% of locks under 1s
    seatora_error_rate: ["rate<0.05"],         // Max 5% actual server errors (5xx)
  },
};

// ─── VU State ─────────────────────────────────────────────────────────────────
let session = null;

export default function () {
  const user = getUserForVU(__VU);

  // Distribute seats evenly across the 500-seat theater so all 200 VUs book unique seats
  const startIndex = ((__VU - 1) * 2 + __ITER * 4) % (ALL_SEAT_IDS.length - 1);
  const seats = [
    ALL_SEAT_IDS[startIndex],
    ALL_SEAT_IDS[startIndex + 1],
  ];

  // Login on the first iteration of each VU only
  if (!session) {
    const loginRes = login(user);
    loginLatency.add(loginRes.timings.duration);

    const loggedIn =
      loginRes.status === 200 && loginRes.json("success") === true;
    if (!loggedIn) {
      if (__ITER === 0 && __VU <= 3) {
        console.error(`[VU ${__VU}] Login failed for ${user.email}: status ${loginRes.status}, body: ${loginRes.body}`);
      }
      errorRate.add(1);
      sleep(1);
      return;
    }
    session = { userId: loginRes.json("user").id };
    errorRate.add(0);
    sleep(0.5);
  }

  // ── Step 1: Lock Seats ──────────────────────────────────────────────────
  const lockRes = http.post(
    `${BASE_URL}/api/show/${SHOW_ID}/seats/lock`,
    JSON.stringify({ userId: session.userId, seatIds: seats }),
    {
      headers: { "Content-Type": "application/json" },
    },
  );
  lockLatency.add(lockRes.timings.duration);

  const lockOk = lockRes.status === 200;
  const isConflict = lockRes.status === 400 || lockRes.status === 409 || lockRes.status === 422;
  const is5xx = lockRes.status >= 500;

  check(lockRes, {
    "lock: not a server error": (r) => r.status < 500,
  });

  if (is5xx) {
    errorRate.add(1);
    sleep(1);
    return;
  }

  if (isConflict || !lockOk) {
    lockConflicts.add(1); // Normal seat contention under high concurrency
    errorRate.add(0);
    sleep(1);
    return; // Do not attempt checkout if lock was not acquired
  }

  // Lock acquired successfully!
  errorRate.add(0);
  sleep(0.5);

  // ── Step 2: Initiate Checkout ───────────────────────────────────────────
  const checkoutRes = http.post(
    `${BASE_URL}/api/booking/initiate`,
    JSON.stringify({ showId: SHOW_ID, seatIds: seats }),
    {
      headers: { "Content-Type": "application/json" },
      timeout: "30s",
    },
  );
  checkoutLatency.add(checkoutRes.timings.duration);

  const checkoutOk = checkoutRes.status === 201;
  const checkout5xx = checkoutRes.status >= 500;

  check(checkoutRes, {
    "initiate: status 201": (r) => r.status === 201,
    "initiate: not a server crash": (r) => r.status !== 500,
  });

  if (checkoutOk) {
    successfulBookings.add(1);
    errorRate.add(0);
  } else if (checkout5xx) {
    checkoutErrors.add(1);
    errorRate.add(1);
    console.error(`[VU ${__VU}] Checkout 5xx: ${checkoutRes.status} | ${checkoutRes.body.substring(0, 200)}`);
  } else {
    errorRate.add(0);
  }

  sleep(2);
}

// ─── Summary Report ───────────────────────────────────────────────────────────
export function handleSummary(data) {
  const successCount = data.metrics["seatora_successful_bookings"]?.values?.count ?? 0;
  const lockConflictCount = data.metrics["seatora_lock_conflicts"]?.values?.count ?? 0;
  const checkoutErrorCount = data.metrics["seatora_checkout_errors"]?.values?.count ?? 0;
  const p50 = data.metrics["seatora_checkout_duration"]?.values?.["p(50)"] ?? 0;
  const p95 = data.metrics["seatora_checkout_duration"]?.values?.["p(95)"] ?? 0;
  const p99 = data.metrics["seatora_checkout_duration"]?.values?.["p(99)"] ?? 0;

  const report = `
┌───────────────────────────────────────────────────────────┐
│            SEATORA SPIKE TEST — SUMMARY REPORT            │
├───────────────────────────────────────────────────────────┤
│  Successful Bookings Initiated:             ${String(successCount).padEnd(8)}      │
│  Seat Lock Conflicts (contention):          ${String(lockConflictCount).padEnd(8)}      │
│  Server Crashes (500s):                     ${String(checkoutErrorCount).padEnd(8)}      │
├───────────────────────────────────────────────────────────┤
│  Checkout Latency  p50: ${String(p50.toFixed(0)).padEnd(6)}ms                      │
│                    p95: ${String(p95.toFixed(0)).padEnd(6)}ms                      │
│                    p99: ${String(p99.toFixed(0)).padEnd(6)}ms                      │
└───────────────────────────────────────────────────────────┘
`;
  console.log(report);
  return {
    "results/spike-test-summary.json": JSON.stringify(data, null, 2),
  };
}
