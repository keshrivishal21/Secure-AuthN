# Auth App (Spring Boot)

A simple Spring Boot 3 + Spring Security + JWT project that demonstrates:

- **Authentication** with **Access Token + Refresh Token**
- **Refresh token stored as a server-side “Session” record** (so we can revoke it)
- **Logout (refresh token revocation)**
- **Subscription plans** (**FREE / BASIC / PREMIUM**) that control **how many concurrent sessions** a user can have
- A request/response **LoggingFilter**

> Base path: `server.servlet.context-path=/api/v1`

---

## 1) Authentication vs Authorization

### Authentication (AuthN)
Authentication answers: **“Who are you?”**

In this app:
- A user proves identity by calling `POST /api/v1/auth/login` with email + password.
- On success the server issues:
  - a **short-lived Access Token** (JWT) returned in the response body
  - a **long-lived Refresh Token** (JWT) stored in an **HttpOnly cookie**

### Authorization (AuthZ)
Authorization answers: **“What are you allowed to do?”**

In this app:
- The **Access Token** contains claims (like `role`).
- `JWTAuthFilter` reads the `Authorization: Bearer <accessToken>` header, loads the `User`, and sets Spring Security’s `Authentication` in the `SecurityContext`.
- Method-level security checks run via annotations like:
  - `@PreAuthorize("hasRole('ADMIN')")`
  - `@PreAuthorize("hasAnyRole('ADMIN', 'USER')")`

---

## 2) Architecture (high level)

### Key modules

- **Controllers**
  - `AuthController`: signup, login, refresh, logout
  - `UserController`: user APIs and subscription endpoint

- **Security**
  - `WebSecurityConfig`: stateless security filter chain + public routes
  - `JWTAuthFilter`: validates Access Token from `Authorization` header
  - `JWTService`: generates/parses JWTs
  - `AuthService`: orchestrates login/refresh/logout

- **Domain / Persistence**
  - `User` entity (implements `UserDetails`)
  - `Session` entity (stores issued refresh tokens)
  - `Subscription` entity (plan + validity)
  - JPA repositories: `UserRepository`, `SessionRepository`, `SubscriptionRepository`

- **Services**
  - `SessionService`: issues/validates/deletes sessions; enforces concurrent session limit
  - `SubscriptionService`: manages subscriptions and resolves active plan

- **Filter**
  - `LoggingFilter`: logs inbound/outbound requests/responses with masking & allowlisted headers

### Stateless API, stateful refresh
The API is configured as **stateless** (`SessionCreationPolicy.STATELESS`).

However, refresh tokens are tracked server-side via the `Session` table:
- Access token is **stateless JWT** (not stored in DB)
- Refresh token is **stateful** (present in DB as a `Session` record)

This is a common hybrid design:
- fast access-token auth
- server-side revocation for refresh tokens

---

## 3) Data model

### `users`
`User` includes:
- `email`, `password`, `role`, `isActive`

### `session`
`Session` includes:
- `refreshToken` (string)
- `lastUsedAt` (timestamp)
- `user` (many sessions per user)

**Meaning:** a `Session` row represents a *refresh token that is currently valid on the server*.

### `subscription`
`Subscription` includes:
- `planType`: `FREE | BASIC | PREMIUM`
- `isActive`
- `startAt`, `endAt`

Active means:
- `isActive = true`, and
- `endAt` is null **or** `endAt > now`

(Implemented in `SubscriptionService#getActivePlan`.)

---

## 4) End-to-end authentication flow (current implementation)

### 4.1 Signup
**Endpoint:** `POST /api/v1/auth/signup`

- Validates request
- Creates `User`
- Hashes password
- Sets default role: `USER`

### 4.2 Login (Access + Refresh)
**Endpoint:** `POST /api/v1/auth/login`

1. `AuthenticationManager` authenticates email/password.
2. `JWTService.generateAccessToken(user)` creates Access Token JWT.
3. `JWTService.generateRefreshToken(user)` creates Refresh Token JWT.
4. `SessionService.generateNewSession(user, refreshToken)` stores refresh token in DB.
   - Before inserting, it enforces a **max concurrent session limit** for the user.
5. Controller sets `refreshToken` as an **HttpOnly cookie** (path `/api/v1/auth`).
6. Response body returns `accessToken`.

**Client responsibilities:**
- Store `accessToken` (typically in memory) and send it as `Authorization: Bearer ...`
- Keep the refresh cookie (browser/HTTP client handles it automatically)

### 4.3 Calling protected APIs
1. Client calls a protected endpoint with `Authorization: Bearer <accessToken>`.
2. `JWTAuthFilter` parses and validates access token.
3. Filter loads the `User` by id and sets authentication in the `SecurityContext`.
4. Controllers/services use `@PreAuthorize(...)` and `SecurityContext` to enforce rules.

### 4.4 Refresh
**Endpoint:** `POST /api/v1/auth/refresh`

1. Controller reads refresh token from cookie `refreshToken`.
2. `AuthService.refreshToken(refreshToken)`:
   - Extracts userId from refresh token
   - Calls `SessionService.validateSession(refreshToken)`:
     - checks the token exists in DB (session not revoked)
     - updates `lastUsedAt`
   - Generates a **new Access Token**
3. Response returns the new access token.

> Important: in the current code, refresh token is **not rotated**. The same refresh token stays valid until it expires or is deleted at logout.

### 4.5 Logout (revocation)
**Endpoint:** `POST /api/v1/auth/logout`

1. Controller reads refresh token from cookie.
2. If present: `AuthService.logout(refreshToken)` calls `SessionService.deleteSession(refreshToken)`.
   - If the session doesn’t exist, it silently succeeds.
3. Server clears refresh cookie.
4. Returns `204 No Content`.

**What logout accomplishes:**
- That specific refresh token can no longer be used to mint new access tokens.

**What logout does NOT do (by default):**
- It does **not** instantly invalidate already-issued access tokens, because access tokens are stateless JWTs.

---

## 5) Subscription-based concurrent session limits

Session limits are configured in `application.properties`:

- `app.session.limit.free=1`
- `app.session.limit.basic=3`
- `app.session.limit.premium=5`
- `app.session.limit=2` (fallback/default)

### How it’s enforced
On login, `SessionService.generateNewSession(user, refreshToken)`:

1. Calls `resolveSessionLimit(user)` which:
   - asks `SubscriptionService.getActivePlan(user)`
   - maps plan -> limit
2. Loads all sessions for that user: `sessionRepository.findByUser(user)`
3. If `sessions.size() >= limit`, it deletes the **least recently used sessions** (by `lastUsedAt`) until there’s room.
4. Saves new session row for the newly issued refresh token.

### Why `lastUsedAt` exists (and “null safety”)
- When a row is first created, `lastUsedAt` is set via `@CreationTimestamp`.
- During refresh, `validateSession()` updates `lastUsedAt = now`.
- The deletion logic sorts using `Comparator.nullsFirst(...)` as an extra safety measure in case a row has a null timestamp (e.g. data backfill, older rows, manual DB edits).

---

## 6) Refresh token rotation (what it is vs current state)

### What “refresh token rotation” means
Rotation means:
- Each time you call `/refresh`, you get a **new refresh token** (and the old one is revoked).

Benefits:
- Limits damage if a refresh token leaks.

### Current behavior in this repo
- `/refresh` returns a new **access token only**.
- The **refresh token stays the same**.
- Session record is only “touched” via `lastUsedAt`.

### Recommended rotation design (fits this codebase)
If you want real rotation with minimal added complexity:

1. In `AuthService.refreshToken(refreshToken)`:
   - validate old refresh token is present in DB
   - generate a **new refresh token**
   - delete old session row
   - save new session row
2. In `AuthController.refresh(...)`:
   - set the **new** refresh token cookie

This matches your earlier requirement:
> “after refresh we should also update the refresh token in cookie as well as in session”

---

## 7) Revocation and access-token logout

### Refresh token revocation (implemented)
Logout deletes the session row for the refresh token.

### Access token revocation (not implemented)
Because access tokens are JWTs and not stored server-side, you have three common options:

1. **Short access TTL** (already used)
   - Keep access tokens short-lived so the window after logout is small.

2. **Access token blacklist / denylist**
   - Store revoked access token IDs (e.g. `jti`) until expiry.
   - Filter checks blacklist on every request.
   - More complexity + storage.

3. **Token versioning**
   - Store `tokenVersion` on the user; include it in JWT.
   - On logout-from-all-devices, increment version.
   - All old tokens become invalid.
   - Requires DB lookup (you already do load user in filter).

This project currently chooses option (1): keep access tokens short-lived.

---

## 8) API endpoints (current)

### Auth
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

### Users
- `GET /api/v1/users` (ADMIN only)
- `GET /api/v1/users/{id}` (ADMIN/USER)
- `PATCH /api/v1/users/delete/{id}` (ADMIN/USER)
- `PATCH /api/v1/users/role` (ADMIN only)

### Subscription
- `POST /api/v1/users/subscribe`

> Note: `/users/subscribe` calls `AppUtils.getCurrentUser()`, so it expects the caller to already be authenticated.

---

## 9) Configuration

From `src/main/resources/application.properties`:

- Context path: `server.servlet.context-path=/api/v1`
- JWT
  - `jwt.secretKey=...`
  - `security.jwt.access-ttl-ms=...`
  - `security.jwt.refresh-ttl-ms=...`
- Session limits
  - `app.session.limit.*`

---

## 10) Logging

`LoggingFilter` is a `@Component`, ordered at highest precedence.

What it logs:
- Request line (method + URI)
- A safe subset of headers (allowlist)
- Response status
- Optionally bodies at DEBUG level for non-sensitive paths

Security precautions:
- Redacts `Authorization`, `Cookie`, `Set-Cookie` headers
- Skips body logging for sensitive endpoints (`/auth/login`, `/auth/refresh`, etc.)
- Applies best-effort masking for JSON fields like `password`, `token`, etc.

---

## 11) Run locally

This is a Maven project. You’ll need:
- Java 21
- PostgreSQL database `auth_db`

Typical commands:

```bat
mvnw.cmd test
mvnw.cmd spring-boot:run
```

---

## 12) Notes / Next improvements

If you want to evolve this into production-style auth:

- Implement **refresh token rotation** (recommended section above)
- Add `Secure=true` cookies when running behind HTTPS
- Add `SameSite` cookie attribute handling (Spring Boot cookie helpers or manual header)
- Consider device/session identification (store user-agent, IP hash, deviceId)
- Add “logout all devices” endpoint (delete all sessions for user)

