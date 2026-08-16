// load-tests/breakpoint-test.js (Coldplay Concert Ticket Drop Simulation)
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate, Counter, Gauge } from "k6/metrics";
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
const showFetchLatency = new Trend("seatora_show_fetch_duration", true);
const errorRate = new Rate("seatora_error_rate");
const successfulBookings = new Counter("seatora_successful_bookings");
const lockConflicts = new Counter("seatora_lock_conflicts");
const serverErrors = new Counter("seatora_5xx_errors");
const activeVUs = new Gauge("seatora_active_vus");

// ─── Test Profile: Concert Ticket Drop Surges (50 -> 200 -> 500 -> 800 VUs) ───
export const options = {
  stages: [
    { duration: "10s", target: 50 },  // T-10s: Pre-sale browsing traffic
    { duration: "15s", target: 200 }, // T=0s: The Drop! Rapid surge to 200 VUs
    { duration: "25s", target: 500 }, // T+15s: 500 concurrent users fighting for seats
    { duration: "30s", target: 800 }, // Peak Stampede: 800 concurrent users
    { duration: "15s", target: 0 },   // Cool down ramp to 0
  ],
  thresholds: {
    seatora_checkout_duration: ["p(95)<3000"],
    seatora_lock_duration: ["p(95)<1000"],
    seatora_error_rate: ["rate<0.05"], // Hard server error limit < 5%
  },
};

// ─── VU State ─────────────────────────────────────────────────────────────────
let session = null;

export default function () {
  activeVUs.add(__VU);

  const user = getUserForVU(__VU);

  // ── High-Contention Concert Seat Selection ──────────────────────────────
  // In a concert drop, users cluster on the most desirable seats (VIP/Front rows 0-100),
  // creating extreme hot-spot contention that stresses the Redis Lua Mutex Gate.
  const hotSpotPoolSize = Math.min(100, ALL_SEAT_IDS.length);
  const seatIndex = ((__VU * 3 + __ITER * 2) % (hotSpotPoolSize - 1));
  const seats = [
    ALL_SEAT_IDS[seatIndex],
    ALL_SEAT_IDS[seatIndex + 1],
  ];

  // ── Step 1: Login (First iteration only) ───────────────────────────────
  if (!session) {
    const loginRes = login(user);
    const loggedIn = loginRes.status === 200 && loginRes.json("success") === true;
    if (!loggedIn) {
      errorRate.add(1);
      serverErrors.add(1);
      sleep(0.5);
      return;
    }
    session = { userId: loginRes.json("user").id };
    errorRate.add(0);
    sleep(0.2);
  }

  // ── Step 2: Show Availability Fetch (Live Seat Map Browsing) ───────────
  const showRes = http.get(`${BASE_URL}/api/show/${SHOW_ID}/seats/getSeats`);
  showFetchLatency.add(showRes.timings.duration);

  const showOk = showRes.status === 200;
  check(showRes, { "seatMap: status 200": (r) => r.status === 200 });
  if (!showOk) {
    if (showRes.status >= 500) serverErrors.add(1);
    errorRate.add(1);
  } else {
    errorRate.add(0);
  }

  sleep(0.1);

  // ── Step 3: Hot-Spot Lock Attempt (Redis Atomic Lua Gate) ──────────────
  const lockRes = http.post(
    `${BASE_URL}/api/show/${SHOW_ID}/seats/lock`,
    JSON.stringify({ userId: session.userId, seatIds: seats }),
    { headers: { "Content-Type": "application/json" } },
  );
  lockLatency.add(lockRes.timings.duration);

  const lockOk = lockRes.status === 200;
  const isConflict = lockRes.status === 400 || lockRes.status === 409 || lockRes.status === 422;
  const is5xx = lockRes.status >= 500;

  check(lockRes, {
    "lock: not a server crash": (r) => r.status < 500,
  });

  if (is5xx) {
    serverErrors.add(1);
    errorRate.add(1);
    sleep(0.5);
    return;
  }

  if (isConflict || !lockOk) {
    lockConflicts.add(1); // Track concert contention (losing users)
    errorRate.add(0);
    sleep(0.2); // Retry browse/lock cycle
    return;
  }

  // ── Step 4: Checkout Initiation (For Lock Winners) ─────────────────────
  errorRate.add(0);
  sleep(0.2);

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
    "checkout: 201 Created": (r) => r.status === 201,
    "checkout: not a server crash": (r) => r.status !== 500,
  });

  if (checkoutOk) {
    successfulBookings.add(1);
    errorRate.add(0);
  } else if (checkout5xx) {
    checkoutErrors.add(1);
    errorRate.add(1);
    console.error(`[VU ${__VU}] Checkout 5xx: ${checkoutRes.status} | ${checkoutRes.body.substring(0, 150)}`);
  } else {
    errorRate.add(0);
  }

  sleep(1);
}

// ─── Summary Report ───────────────────────────────────────────────────────────
export function handleSummary(data) {
  const successCount = data.metrics["seatora_successful_bookings"]?.values?.count ?? 0;
  const lockConflictCount = data.metrics["seatora_lock_conflicts"]?.values?.count ?? 0;
  const serverErrorCount = data.metrics["seatora_5xx_errors"]?.values?.count ?? 0;
  const totalReqs = data.metrics["http_reqs"]?.values?.count ?? 0;
  const rps = data.metrics["http_reqs"]?.values?.rate ?? 0;
  const lockP95 = data.metrics["seatora_lock_duration"]?.values?.["p(95)"] ?? 0;
  const checkoutP95 = data.metrics["seatora_checkout_duration"]?.values?.["p(95)"] ?? 0;

  const report = `
╔═════════════════════════════════════════════════════════════════╗
║         COLDPLAY TICKET DROP BREAKPOINT TEST REPORT            ║
╠═════════════════════════════════════════════════════════════════╣
║  Peak Concurrency:                         800 Virtual Users    ║
║  Throughput (RPS):                         ${String(rps.toFixed(1)).padEnd(6)} req/sec         ║
║  Total HTTP Requests Processed:            ${String(totalReqs).padEnd(8)}             ║
╠═════════════════════════════════════════════════════════════════╣
║  Successful Bookings Completed:            ${String(successCount).padEnd(8)}             ║
║  Contention Conflicts Deflected by Redis:  ${String(lockConflictCount).padEnd(8)}             ║
║  Server Errors (5xx Crashes):              ${String(serverErrorCount).padEnd(8)}             ║
╠═════════════════════════════════════════════════════════════════╣
║  Redis Lock Gate Latency (p95):            ${String(lockP95.toFixed(1)).padEnd(6)} ms             ║
║  Checkout Initiation Latency (p95):        ${String(checkoutP95.toFixed(1)).padEnd(6)} ms             ║
╚═════════════════════════════════════════════════════════════════╝
`;
  console.log(report);
  return {
    "results/breakpoint-test-summary.json": JSON.stringify(data, null, 2),
  };
}
