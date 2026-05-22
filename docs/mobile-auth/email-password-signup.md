# Email & password — login and registration

Paths use **`/app/auth`** (recommended). Legacy: **`/auth`** where mirrored.

---

## Sign in (after registration)

**`POST /app/auth/login`** — equivalent: **`POST /auth/login`**

| Field | Type | Required |
|-------|------|----------|
| `email` | string | yes |
| `password` | string | yes |

**omit** `dashboardUsername` for normal consumer users (admin dashboard only).

```json
{
  "email": "test@realestate.com",
  "password": "Test@123"
}
```

**200 OK** — `token`, `refreshToken`, `expiresIn`, `user`.

**401** — invalid credentials or blocked states (e.g. pending portal approval for staff accounts).

---

## Create account (email + password + email OTP)

Mobile registration is **two steps** (OTP verifies email before the account is stored).

### Step A — send registration OTP

**`POST /app/auth/register/send-otp`**

| Field | Required | Notes |
|-------|----------|--------|
| `email` | yes | Unique — must not already be registered |
| `password` | yes | **8–128** characters (server-enforced); bcrypt stored **after** OTP success |
| `fullName` | yes | Trimmed server-side |

```json
{
  "fullName": "Aryan Tomar",
  "email": "user@example.com",
  "password": "Str0ng!Pass"
}
```

**200 OK** — `{ "success": true, "message": "OTP sent successfully", "expiresIn": 300 }`

**400** — validation / duplicate email (`message` explains).

---

### Step B — verify OTP and complete registration

**`POST /app/auth/register/verify-otp`**

| Field | Required |
|-------|----------|
| `email` | yes |
| `otp` | yes — six-digit code |

```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**200 OK** — same JSON shape as **`/login`** (`token`, `refreshToken`, `expiresIn`, `user`) plus optional cookies on web.

**400** — **`Incorrect OTP. Registration failed.`** (wrong code); or expired pending registration session (`message` explains).

---

## Legacy instant signup (non-app controller only)

**`POST /auth/signup`** on **`/api/v1/auth`** still creates an account **without** email OTP (older integrations).

**`POST /app/auth/signup`** is **not** exposed — consumer/mobile apps must use **`register/send-otp`** + **`register/verify-otp`**.
