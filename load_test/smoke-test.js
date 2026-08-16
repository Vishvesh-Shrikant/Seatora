// load-tests/smoke-test.js
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";
import {
  BASE_URL,
  SHOW_ID,
  getUserForVU,
  getSeatsForVU,
  login,
} from "./config.js";

// ─── Custom Metrics ───────────────────────────────────────────────────────────
const loginLatency = new Trend("seatora_login_duration", true); // true = display in ms
const showFetchLatency = new Trend("seatora_show_fetch_duration", true);
const seatFetchLatency = new Trend("seatora_seat_fetch_duration", true);
const lockLatency = new Trend("seatora_seat_lock_duration", true);
const checkoutLatency = new Trend("seatora_checkout_duration", true);
const errorRate = new Rate("seatora_error_rate");
const totalRequests = new Counter("seatora_total_requests");

// ─── Test Configuration ───────────────────────────────────────────────────────
export const options = {
  vus: 1, // Single user
  iterations: 1, // Run exactly once

  thresholds: {
    // Smoke test SLA: Everything must work perfectly
    http_req_failed: ["rate==0"], // Zero failures allowed
    seatora_login_duration: ["p(99)<2000"], // Login < 2s (BCrypt cost 12 is slow)
    seatora_show_fetch_duration: ["p(99)<500"],
    seatora_seat_fetch_duration: ["p(99)<500"],
    seatora_seat_lock_duration: ["p(99)<1000"],
    seatora_checkout_duration: ["p(99)<3000"], // Razorpay API call included
    seatora_error_rate: ["rate==0"],
  },
};

// ─── Main Test Flow ───────────────────────────────────────────────────────────
export default function () {
  const user = { email: "vishvesh.shri1312@gmail.com", password: "Hello@123" };
  const seats = getSeatsForVU(__VU);

  // ── Step 1: Login ──────────────────────────────────────────────────────
  const loginStart = Date.now();
  const loginRes = login(user);
  loginLatency.add(Date.now() - loginStart);
  totalRequests.add(1);

  const loggedIn = loginRes.status === 200 && loginRes.json("success") === true;
  if (!loggedIn) {
    errorRate.add(1);
    console.error(`[VU ${__VU}] Login failed for ${user.email}`);
    return;
  }
  const userId = loginRes.json("user").id;
  errorRate.add(0);
  sleep(1);

  // ── Step 2: Fetch Available Shows ──────────────────────────────────────
  const showsRes = http.get(
    `${BASE_URL}/api/shows/availableShowsForMovie/7d17c6e4-38fb-45ae-b245-61ae843375dd?date=2026-06-05`,
  );
  showFetchLatency.add(showsRes.timings.duration);
  totalRequests.add(1);

  if (showsRes.status !== 200) {
    console.error(`getShows failed: status ${showsRes.status}, body: ${showsRes.body}`);
  }
  check(showsRes, {
    "getShows: status 200": (r) => r.status === 200,
    "getShows: has shows": (r) => r.json("shows") !== undefined,
  });
  errorRate.add(showsRes.status !== 200 ? 1 : 0);
  sleep(1);

  // ── Step 3: Fetch Seat Map for the Show ────────────────────────────────
  const seatsRes = http.get(`${BASE_URL}/api/show/${SHOW_ID}/seats/getSeats`);
  seatFetchLatency.add(seatsRes.timings.duration);
  totalRequests.add(1);

  check(seatsRes, {
    "getSeats: status 200": (r) => r.status === 200,
    "getSeats: has seats": (r) => r.json("seats") !== undefined,
  });
  errorRate.add(seatsRes.status !== 200 ? 1 : 0);
  sleep(2);

  // Dynamically pick 2 genuinely available seats from the live seat map
  const allSeats = seatsRes.json("seats") || [];
  const availableSeatIds = allSeats
    .filter((s) => s.seatStatus === "AVAILABLE")
    .map((s) => s.seat.seatId);
  const seatsToLock = availableSeatIds.length >= 2 ? availableSeatIds.slice(0, 2) : seats;

  // ── Step 4: Lock Seats ─────────────────────────────────────────────────
  const lockRes = http.post(
    `${BASE_URL}/api/show/${SHOW_ID}/seats/lock`,
    JSON.stringify({ userId: userId, seatIds: seatsToLock }),
    { headers: { "Content-Type": "application/json" } },
  );
  lockLatency.add(lockRes.timings.duration);
  totalRequests.add(1);

  if (lockRes.status !== 200) {
    console.error(`lockSeats failed: status ${lockRes.status}, body: ${lockRes.body}`);
  }
  check(lockRes, {
    "lockSeats: status 200": (r) => r.status === 200,
    "lockSeats: success=true": (r) => r.json("success") === true,
  });
  errorRate.add(lockRes.status !== 200 ? 1 : 0);
  sleep(1);

  // ── Step 5: Initiate Checkout ──────────────────────────────────────────
  const checkoutRes = http.post(
    `${BASE_URL}/api/booking/initiate`,
    JSON.stringify({ showId: SHOW_ID, seatIds: seatsToLock }),
    { headers: { "Content-Type": "application/json" } },
  );
  checkoutLatency.add(checkoutRes.timings.duration);
  totalRequests.add(1);

  if (checkoutRes.status !== 201) {
    console.error(`checkout initiate failed: status ${checkoutRes.status}, body: ${checkoutRes.body}`);
  }
  check(checkoutRes, {
    "initiate: status 201": (r) => r.status === 201,
    "initiate: has razorpay order": (r) => r.json("payment") !== null,
  });
  errorRate.add(checkoutRes.status !== 201 ? 1 : 0);

  // ── NOTE: /booking/verify is NOT called in smoke tests ─────────────────
  // Razorpay signature verification requires a real cryptographic signature
  // from Razorpay's servers. This cannot be faked in a load test.
  // The /verify endpoint is tested manually or via integration tests only.

  sleep(3);
}

// ─── Summary Report ───────────────────────────────────────────────────────────
export function handleSummary(data) {
  return {
    "results/smoke-test-summary.json": JSON.stringify(data, null, 2),
  };
}
