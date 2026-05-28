# 🎬 Seatora — Event Booking Backend

> A distributed, production-grade movie ticket booking REST API built with **Spring Boot 4**, **PostgreSQL**, **RabbitMQ**, and full **Optimistic Concurrency Control** across all critical domain entities.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-4-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Razorpay](https://img.shields.io/badge/Razorpay-Integrated-02042B?style=for-the-badge&logo=razorpay&logoColor=white)

---

## Quick Start

### Prerequisites

| Tool | Minimum Version |
|---|---|
| Java JDK | 21 |
| Apache Maven | 3.9 |
| Docker | Latest stable |
| Docker Compose | v2 |

### Option A: Docker Compose (Recommended)

Spins up **RabbitMQ** and the **Spring Boot backend** in an isolated Docker network.

```bash
# 1. Clone the repository
git clone <repository-url>
cd Seatora_Server

# 2. Configure your environment
# Edit .env with your DB URI, credentials, Razorpay keys, and JWT secret

# 3. Build and start all services
docker-compose up --build

# 4. Verify the stack is healthy
curl http://localhost:8080/api/health
```

**Running services:**

| Service | Port | Notes |
|---|---|---|
| Spring Boot API | `8080` | REST API base URL |
| RabbitMQ Broker | `5672` | AMQP connection port |
| RabbitMQ Management | `15672` | `http://localhost:15672` — login: guest / guest |

> **Note:** The Dockerized backend connects to the PostgreSQL instance specified in `DB_URI`. You need an externally accessible database (e.g., Supabase, Neon, or a locally running PostgreSQL container).

### Option B: Local Maven Run

```bash
# Requires a running PostgreSQL instance configured in .env
mvn spring-boot:run
```

The application loads the `.env` file automatically via `spring-dotenv`.

### Running Tests

```bash
mvn test
```

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Domain Model](#4-domain-model)
5. [Security Design](#5-security-design)
6. [Concurrency & Reliability Design](#6-concurrency--reliability-design)
7. [Environment Variables](#7-environment-variables)
8. [Project Structure](#8-project-structure)

---

## 1. Project Overview

**Seatora** is a full-stack, distributed event-ticketing system. This repository contains the **Spring Boot backend**, a stateless REST API responsible for movie browsing, seat reservation, payment processing, and QR-coded ticket delivery.

### Core User Flow

```
Browse Movies → Select Show → Lock Seats (10-min timer)
    → Initiate Checkout → Pay via Razorpay → Verify HMAC Signature
        → Booking Confirmed → Outbox Event Written (same DB transaction)
            → RabbitMQ delivers task → QR Code generated (ZXing)
                → Confirmation email dispatched → Admin scans QR at venue → Entry Approved
```

### Engineering Highlights

| Feature | Implementation |
|---|---|
| **Distributed seat locking** | Database-level, time-bounded locks with a 10-minute expiry; max 6 seats per request |
| **Optimistic Concurrency Control** | JPA `@Version` on all 8 write-critical entities; `ObjectOptimisticLockingFailureException` globally handled → HTTP 409 |
| **Transactional Outbox Pattern** | Email tasks written to DB atomically with payment confirmation; RabbitMQ carries only the UUID — no payload loss on crash |
| **Payment integrity** | Razorpay HMAC-SHA256 signature verification before any booking is confirmed |
| **QR code ticketing** | ZXing-generated QR codes embedding a signed JSON payload, delivered in HTML confirmation emails |
| **Abuse penalty system** | Users who abandon ≥3 seat locks are blocked for 15 minutes per show |
| **Dead Letter Queue** | Failed email deliveries are retried 3× by Spring AMQP, then routed to `email.dlq` for inspection |
| **Pessimistic scan lock** | `SELECT … FOR UPDATE` on QR scan prevents duplicate venue entry under concurrent requests |
| **Scheduled cleanup** | `@Scheduled` job (every 60 s) expires stale locks, marks bookings `FAILED`, and records abandonment penalties |

---

## 2. Architecture

### End-to-End Booking Sequence

```
Client                     Spring Boot API              PostgreSQL       RabbitMQ      SMTP
  │                               │                          │               │            │
  │── POST /seats/lock ──────────▶│                          │               │            │
  │                               │── LOCK seats (LOCKED) ──▶│               │            │
  │◀── 200 {lockExpiry: +10min} ──│                          │               │            │
  │                               │                          │               │            │
  │   [ User completes payment on Razorpay checkout UI ]     │               │            │
  │                               │                          │               │            │
  │── POST /booking/initiate ────▶│                          │               │            │
  │                               │── CREATE Booking(PENDING)▶│               │            │
  │                               │── Razorpay createOrder() ─[external HTTP]─│            │
  │                               │── CREATE Payment(PENDING)▶│               │            │
  │◀── 201 {razorpayOrderId} ─────│                          │               │            │
  │                               │                          │               │            │
  │── POST /booking/verify ──────▶│                          │               │            │
  │                               │── verifyHMAC(sig) ─── [Razorpay SDK]     │            │
  │                               │── UPDATE Booking → CONFIRMED ───────────▶│            │
  │                               │── UPDATE Payment → SUCCESS ─────────────▶│            │
  │                               │── INSERT EmailOutboxEvent ───────────────▶│            │
  │◀── 200 {bookingId} ───────────│                          │               │            │
  │                               │                          │               │            │
  │       [ EmailOutboxProcessor polls DB every 5 s ]        │               │            │
  │                               │── SELECT pending outbox ─▶│               │            │
  │                               │── publish(outboxId) ──────────────────────▶│            │
  │                               │                          │               │            │
  │       [ EmailMessageListener consumes ]                  │               │            │
  │                               │◀── consume(outboxId) ────────────────────│            │
  │                               │── FETCH Booking + Items ─▶│               │            │
  │                               │── generateQrCode(JSON) ───│               │            │
  │                               │── sendEmail(qrCode) ──────────────────────────────────▶│
  │                               │── UPDATE OutboxEvent → COMPLETED ────────▶│            │
```

---

## 3. Technology Stack

| Category | Technology | Version |
|---|---|---|
| **Runtime** | Java (OpenJDK) | 21 |
| **Framework** | Spring Boot | 4.0.2 |
| **Web** | Spring MVC (Embedded Tomcat) | — |
| **Security** | Spring Security + JJWT | 0.12.5 |
| **Password Hashing** | BCrypt | Strength 12 |
| **ORM** | Spring Data JPA / Hibernate | — |
| **Database** | PostgreSQL | 16 |
| **Messaging** | Spring AMQP / RabbitMQ | 4 |
| **Payments** | Razorpay Java SDK | 1.4.8 |
| **QR Codes** | Google ZXing | 3.5.2 |
| **Email** | Spring Mail (JavaMail / SMTP) | — |
| **Configuration** | spring-dotenv | 4.0.0 |
| **Containerisation** | Docker + Docker Compose | — |
| **Observability** | Spring Actuator + Micrometer + Prometheus | — |
| **Build Tool** | Apache Maven | 3.9 |
| **Boilerplate** | Lombok | — |

---

## 4. Domain Model

The system is modelled across **12 JPA entities**. Eight of them carry `@Version` for Optimistic Concurrency Control.

### Entity Relationships

```
Theatre ──< Screen ──< Seat
                │
                └──< Show ──< SeatAvailability >── Seat
                               │
                               └──< BookingItem >── Booking >── User
                                                         │
                                                         └──< Payment

User ──< UserBookingPenalty
EmailOutboxEvent  (standalone; linked to Booking via bookingId string field)
```

---

## 5. Security Design

### JWT Authentication
- JWT is issued on login and stored in an **`HttpOnly`** cookie (not the `Authorization` header).  
  Storing the token in a cookie rather than `localStorage` prevents **XSS-based token theft** — JavaScript running in the browser cannot read `HttpOnly` cookies.
- Sessions are **stateless** — `SessionCreationPolicy.STATELESS`. No server-side session state is stored.
- Token expiry is configurable via `JWT_EXPIRY` (default: 24 hours).
- The cookie is cleared server-side on logout.

### Authorization
- **Method-level security** via `@EnableMethodSecurity` + `@PreAuthorize("hasAuthority('ADMIN')")`.
- Two roles: `USER` (default) and `ADMIN`.
- Public routes: `/`, `/api/health`, `/api/auth/**`, `/api/movie/getMovies`.
- All other routes require authentication; admin-gated routes additionally assert the `ADMIN` role.

### Password Security
- Passwords are hashed with **BCrypt at strength 12** — a standard secure configuration balancing security and login latency.

### CORS Configuration
- Allowed origins: `http://localhost:3000`, `http://localhost:3001`
- Allowed methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
- Credentials (cookies) allowed
- `Set-Cookie` exposed in response headers

### IDOR Protection
All user-scoped endpoints (booking detail, QR code generation) verify server-side that the requesting user's ID matches the resource owner. Violations throw `AccessDeniedException` → HTTP 403.

### Payment Integrity
Every call to `POST /booking/verify` runs an **HMAC-SHA256 verification** over `razorpayOrderId + "|" + razorpayPaymentId` using the Razorpay secret. Bookings are never confirmed without a valid signature.

### Global Exception Handler

All exceptions are centralized in `GlobalExceptionHandler` (`@RestControllerAdvice`). Every error response follows a consistent `ApiErrorResponse` structure:

```json
{
  "timestamp": "2025-06-01T10:30:00Z",
  "status": 409,
  "error": "Please Try Again",
  "message": "Someone else just booked or modified this seat. Please refresh and try again.",
  "path": "/api/booking/initiate",
  "fieldErrors": null
}
```

---

## 6. Concurrency & Reliability Design

### Optimistic Concurrency Control (OCC)
- JPA `@Version` is applied to all 8 write-critical entities: `Movie`, `Theatre`, `Screen`, `Show`, `User`, `UserBookingPenalty`, `Booking`, and `Payment`.
- Hibernate appends the expected version to every `UPDATE` statement (`WHERE id = ? AND version = ?`). If another transaction already committed a change, the `WHERE` clause matches 0 rows and Hibernate throws `ObjectOptimisticLockingFailureException`.
- `GlobalExceptionHandler` catches this and returns **HTTP 409 Conflict**, prompting the client to reload and retry.
- Prevents the **Lost Update Anomaly** across disconnected HTTP sessions — no database locks are held between the `GET` (read) and the `PATCH` (write).

### Pessimistic Locking (QR Ticket Scan)
- `POST /booking/scanQR` uses `SELECT … FOR UPDATE` (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) to prevent two concurrent scan requests both reading `isScanned = false` and both granting entry.
- The lock is held for milliseconds (a single field update), so connection-pool starvation is not a risk.

### Transactional Outbox Pattern
- Payment confirmation (`UPDATE Payment → SUCCESS`, `UPDATE Booking → CONFIRMED`, `INSERT EmailOutboxEvent`) is a **single atomic DB transaction** — all three writes commit together or not at all.
- `EmailOutboxProcessor` polls the outbox table every 5 s and publishes only the UUID to RabbitMQ; the listener fetches the full payload from the DB, guaranteeing **no message-payload loss** on crash.
- Guarantees **at-least-once delivery**: if the app crashes after the DB commit but before the RabbitMQ publish, the next poll cycle picks it up.

### Dead Letter Queue (DLQ)
- Spring AMQP retries failed email deliveries **up to 3 times** via its retry interceptor.
- After all retries are exhausted the message is routed to `email.dlq` via the `email.dlx` dead-letter exchange.
- A dedicated `@RabbitListener` on `email.dlq` marks the `EmailOutboxEvent` row as `FAILED` for manual inspection and reprocessing.

### Seat Lock Expiry Scheduler
- `SeatLockCleanupService` runs every **60 seconds** (`@Scheduled(fixedRate = 60000)`).
- Finds all `LOCKED` seats whose `lockExpiry < NOW()`, records or increments a `UserBookingPenalty` per `userId + showId` pair, then releases the seats back to `AVAILABLE`.
- Also calls `BookingService.failExpiredPendingBookings()` to mark `PENDING` bookings older than 10 minutes as `FAILED`.

### Abuse Penalty System
- Each abandoned lock (lock expiry without payment) increments `UserBookingPenalty.failedLockCount` for that `userId + showId`.
- When `failedLockCount >= 3` and `penaltyExpiry` is still in the future, the next `POST /seats/lock` is rejected with HTTP 409.
- Penalty duration: **15 minutes** per show per user.
- `@Version` on `UserBookingPenalty` ensures two concurrent scheduler cycles cannot both increment the count and produce an incorrect result.

---

## 7. Environment Variables

Create a `.env` file in the project root (`Seatora_Server/.env`). All variables below are required.

```env
BASE_URL=http://localhost:8080
FRONTEND_URL=http://localhost:3000

DB_URI=jdbc:postgresql://localhost:5432/seatora
DB_USER=postgres
DB_PASSWORD=secret

JWT_SECRET=<64-hex-char random string — run: openssl rand -hex 64>
JWT_EXPIRY=24

EMAIL_USERNAME=you@gmail.com
EMAIL_PASSWORD=xxxx xxxx xxxx xxxx
EMAIL_TOKEN_EXPIRY=1

RAZORPAY_TEST_API_KEY=rzp_test_...
RAZORPAY_TEST_API_SECRET=...
```

---

## 8. Project Structure

```
Seatora_Server/
│
├── src/
│   └── main/
│       └── java/com/vishvesh/event_booking/
│           │
│           ├── TicketingSystemApplication.java      # @SpringBootApplication entry point
│           │
│           ├── config/
│           │   ├── RabbitMQConfig.java               # Queue, exchange, DLQ, DLX bean definitions
│           │   ├── RazorpayConfig.java                # Razorpay client singleton bean
│           │   └── SecurityConfig.java                # Filter chain, CORS, BCrypt, JWT filter wiring
│           │
│           ├── controller/                            # REST layer — 10 controllers
│           │   ├── AuthController.java                #  /api/auth  (5 endpoints)
│           │   ├── BookingController.java             #  /api/booking  (6 endpoints)
│           │   ├── HomeController.java                #  /api/health
│           │   ├── MovieController.java               #  /api/movie  (4 endpoints)
│           │   ├── ScreenController.java              #  /api/screen  (4 endpoints)
│           │   ├── SeatAvailabilityController.java    #  /api/show/{id}/seats  (2 endpoints)
│           │   ├── SeatController.java                #  /api/seats  (4 endpoints)
│           │   ├── ShowController.java                #  /api/shows  (9 endpoints)
│           │   ├── TheatreController.java             #  /api/theatre  (4 endpoints)
│           │   └── UserController.java                #  /api/user  (1 endpoint)
│           │
│           ├── dto/                                   # Request / Response DTOs (Lombok records)
│           │   ├── authdto/                           # LoginDto, SignupDto, AuthResponseDto, JwtDto
│           │   ├── checkout/                          # CheckoutRequestDto, PaymentVerificationRequestDto
│           │   ├── email/                             # EmailPayloadDto
│           │   ├── movie/                             # MovieRequestDto
│           │   ├── screen/                            # ScreenRequestDto
│           │   ├── seat/                              # SeatRequestDto
│           │   ├── seatavailability/                  # SeatLockRequestDto
│           │   ├── show/                              # ShowRequestDto
│           │   └── theatre/                           # TheatreRequestDto
│           │
│           ├── entity/                                # JPA entities — 12 classes
│           │   ├── Booking.java                       #  @Version ✅
│           │   ├── BookingItem.java
│           │   ├── EmailOutboxEvent.java
│           │   ├── Movie.java                         #  @Version ✅
│           │   ├── Payment.java                       #  @Version ✅
│           │   ├── Screen.java                        #  @Version ✅
│           │   ├── Seat.java
│           │   ├── SeatAvailability.java              #  idx_seat_availability_show_status index
│           │   ├── Show.java                          #  @Version ✅
│           │   ├── Theatre.java                       #  @Version ✅
│           │   ├── User.java                          #  @Version ✅
│           │   └── UserBookingPenalty.java            #  @Version ✅
│           │
│           ├── mapper/                                # Static entity → DTO mapping utilities
│           │   ├── SeatAvailabilityMapper.java
│           │   ├── SeatMapper.java
│           │   ├── ShowMapper.java
│           │   └── TheatreMapper.java
│           │
│           ├── repository/                            # Spring Data JPA repositories
│           │   ├── BookingRepository.java             #  includes findByIdWithLock (SELECT FOR UPDATE)
│           │   ├── EmailOutboxEventRepository.java
│           │   ├── MovieRepository.java
│           │   ├── PaymentRepository.java
│           │   ├── ScreenRepository.java
│           │   ├── SeatAvailabilityRepository.java
│           │   ├── SeatRepository.java
│           │   ├── ShowRepository.java
│           │   ├── TheatreRepository.java
│           │   ├── UserPenaltyRepository.java
│           │   └── UserRepository.java
│           │
│           ├── security/
│           │   ├── CustomUserDetailsService.java      # Loads User by email for Spring Security
│           │   ├── JwtFilter.java                     # Extracts + validates JWT from cookie per request
│           │   └── JwtService.java                    # Token minting, claims parsing, expiry validation
│           │
│           ├── service/                               # Business logic — 16 service beans
│           │   ├── AuthService.java                   # Signup, login, email verification, token resend
│           │   ├── BookingService.java                # Core booking + payment orchestration (388 lines)
│           │   ├── EmailMessageListener.java          # RabbitMQ consumer → QR generation → SMTP send
│           │   ├── EmailOutboxProcessor.java          # Polls outbox DB table → publishes UUIDs to RabbitMQ
│           │   ├── EmailRetryService.java             # Manual retry for FAILED outbox events
│           │   ├── EmailService.java                  # HTML email composition + JavaMail dispatch
│           │   ├── MovieService.java                  # Movie CRUD with OCC-aware updates
│           │   ├── PaymentGatewayService.java         # Razorpay order creation + HMAC-SHA256 verify
│           │   ├── QrCodeService.java                 # ZXing QR code byte[] generation
│           │   ├── ScreenService.java                 # Screen CRUD + seat availability provisioning
│           │   ├── SeatAvailabilityService.java       # Seat locking, penalty enforcement, seat release
│           │   ├── SeatLockCleanupService.java        # @Scheduled expiry job — runs every 60 s
│           │   ├── SeatService.java                   # Seat CRUD within screens
│           │   ├── ShowService.java                   # Show CRUD, conflict detection, status transitions
│           │   ├── TheatreService.java                # Theatre CRUD with city-based querying
│           │   └── UserService.java                   # User profile retrieval
│           │
│           └── utils/
│               ├── ApiErrorResponse.java              # Standard error response record (timestamp, status, etc.)
│               ├── BusinessRuleException.java         # Domain exception for 422 Unprocessable Content
│               ├── CookieUtil.java                    # addJwtCookie / clearJwtCookie helpers
│               ├── DateTimeUtil.java                  # OffsetDateTime formatting utilities
│               ├── GlobalExceptionHandler.java        # @RestControllerAdvice — 13 exception handlers
│               └── enums/
│                   ├── AuthProvider.java              # CREDENTIAL, GOOGLE
│                   ├── BookingStatus.java             # PENDING, CONFIRMED, FAILED
│                   ├── MovieFormat.java               # STANDARD, IMAX, DOLBY, etc.
│                   ├── OutboxStatus.java              # PENDING, COMPLETED, FAILED
│                   ├── PaymentStatus.java             # PENDING, SUCCESS, FAILED, REFUNDED
│                   ├── Role.java                      # USER, ADMIN
│                   ├── SeatStatus.java                # AVAILABLE, LOCKED, BOOKED
│                   ├── SeatType.java                  # SILVER, GOLD, PLATINUM
│                   └── ShowStatus.java                # SCHEDULED, COMPLETED, CANCELLED
│
├── Dockerfile                                         # Multi-stage: Maven builder → JRE 21 Alpine
├── docker-compose.yml                                 # RabbitMQ + backend services
├── pom.xml                                            # Maven dependency manifest
└── .env                                               # Environment variables — git-ignored
```

---

*Built by Vishvesh Shrikant*
