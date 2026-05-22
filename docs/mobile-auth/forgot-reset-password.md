# Forgot password — OTP flow (recommended for mobile)

Paths **`/app/auth/forgot-password/otp/*`**.

---

## Step 1 — request OTP (email must exist)

**`POST /app/auth/forgot-password/otp/send`**

```json
{ "email": "user@example.com" }
```

| Response | When |
|----------|------|
| **200 OK** | `{ "success": true, "message": "OTP sent successfully", "expiresIn": 300 }` — OTP emailed |
| **404 NOT FOUND** | **`{ "message": "Email ID does not exist." }`** — show this exact message in the app |
| **400** | Invalid email format / missing email |

OTP is **six digits**, valid **5 minutes** (same behaviour as registration OTP).

---

## Step 2 — verify OTP and set new password

**`POST /app/auth/forgot-password/otp/complete`**

```json
{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "NewStr0ng!Pass"
}
```

| Field | Rules |
|-------|--------|
| `newPassword` | **8–128** characters |

**200 OK** — `{ "ok": true, "message": "Password updated successfully. You can sign in with your new password." }`

**400** — incorrect/expired OTP (`message` explains).

Then sign in with **`POST /app/auth/login`**.

---

# Legacy — reset link by email (no “email exists” hint)

Some web flows avoid enumeration:

**`POST /app/auth/forgot-password`** — always **`200`** `{ "ok": true }`; if the account exists, user receives a link with JWT.

**`POST /app/auth/reset-password`** — `{ "token": "<from link>", "newPassword": "…" }`

Configure TTL via **`app.auth.password-reset-token-ms`** (default **15 minutes**) and **`app.auth.password-reset-frontend-base-url`**.
