# Email OTP — behaviour & variants

All OTPs described here are **six-digit numeric codes**. After any successful **`…/send…`** call, delivery is typically **seconds to a few minutes** (mail provider dependent). Each code remains valid for **5 minutes** (`expiresIn: 300` where returned).

---

## Which OTP endpoint should I call?

| Product flow | Send | Verify / complete |
|--------------|------|-------------------|
| **Registration** (email + password + OTP) | **`POST /app/auth/register/send-otp`** | **`POST /app/auth/register/verify-otp`** |
| **Forgot password** | **`POST /app/auth/forgot-password/otp/send`** | **`POST /app/auth/forgot-password/otp/complete`** (includes `newPassword`) |
| **Legacy passwordless** (older web / portal) | **`POST /app/auth/send-otp`** | **`POST /app/auth/verify-otp`** |

Internally, registration and forgot-password OTPs use separate **purposes** so they never collide with legacy **`send-otp`**.

---

## End-to-end flows

See **[authentication-flow.md](./authentication-flow.md)** for registration, login, and forgot-password sequences in plain language.

---

## Legacy `send-otp` / `verify-otp` (passwordless)

Still supported for older clients:

**Send**

```json
{ "email": "user@example.com" }
```

**Verify** — existing user:

```json
{ "email": "user@example.com", "otp": "123456" }
```

**Verify** — **new** magic-link user (no password):

```json
{
  "email": "newuser@example.com",
  "otp": "123456",
  "fullName": "Jane Doe"
}
```

Prefer **`register/send-otp`** for new mobile apps that require a password.
