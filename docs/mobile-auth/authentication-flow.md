# End-to-end authentication flow (mobile app)

Use base URL **`/api/v1`** and prefix **`/app/auth`** unless noted.

---

## 1. User registration (email + password + email OTP)

1. User enters **email**, **password**, and **full name**.
2. App calls **`POST /app/auth/register/send-otp`** with `{ email, password, fullName }`.
3. Server emails a **six-digit OTP**. Delivery is usually within **seconds to a few minutes** (depends on mail provider). The code stays valid for **5 minutes** (`expiresIn: 300`).
4. User enters the OTP.
5. App calls **`POST /app/auth/register/verify-otp`** with `{ email, otp }`.
6. If the OTP is **correct**, the account is created with the password from step 1 and the API returns JWTs (**same shape as login**).
7. If the OTP is **incorrect**, registration fails (**HTTP 400**) with **`Incorrect OTP. Registration failed.`** (see API response `message`).

**Login immediately after registration:** use **`POST /app/auth/login`** with email + password (below).

> Legacy instant signup **`POST /app/auth/signup`** was removed from the app controller; use the registration OTP flow instead. **`POST /api/v1/auth/signup`** remains on the legacy controller for older integrations.

---

## 2. User login

After registration, sign in only with **email + password**:

- **`POST /app/auth/login`** — `{ "email", "password" }`
- Response: `token`, `refreshToken`, `expiresIn`, `user`.

(Optional) Legacy passwordless OTP **`send-otp` / `verify-otp`** still exists for older web flows; new mobile builds should use email + password login.

---

## 3. Forgot password (must exist + OTP + new password)

1. User enters **email**.
2. App calls **`POST /app/auth/forgot-password/otp/send`** with `{ email }`.
3. If the email **does not exist**, API returns **HTTP 404** with  
   **`{ "message": "Email ID does not exist." }`** — show this text to the user.
4. If it exists, a **six-digit OTP** is emailed (same delivery / **5-minute** validity behaviour as registration).
5. User enters OTP and **new password** (min **8** characters, max **128**).
6. App calls **`POST /app/auth/forgot-password/otp/complete`** with `{ email, otp, newPassword }`.
7. On success: **`{ "ok": true, "message": "..." }`**. User signs in with **`POST /app/auth/login`**.

> Separate legacy flow: **`POST /app/auth/forgot-password`** still sends a **reset link** by email without revealing whether the address exists (no enumeration). Use **`forgot-password/otp/send`** when you must match the product copy above.

---

## Token refresh

Use **`POST /app/auth/refresh`** with `{ "refreshToken" }` before access token expiry.

See also: [Email OTP details](./email-otp.md), [Tokens & errors](./tokens-and-errors.md).

---

## Frontend tester (Next.js)

The repo includes a minimal dev page at **`/mobile-auth-test`** (`my-property-fact/src/app/mobile-auth-test/page.jsx`). Run the Next app with **`NEXT_PUBLIC_API_URL`** pointing at your API (e.g. `http://localhost:8005/api/v1/`), then open **`http://localhost:3000/mobile-auth-test`** to exercise register / login / forgot-password OTP calls and inspect JSON responses.
