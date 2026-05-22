# UI ↔ API quick map & demo accounts

## Registration (password + OTP)

| Screen field | API |
|--------------|-----|
| Full name | `POST /app/auth/register/send-otp` → `fullName` |
| Email | `email` |
| Password | `password` |
| OTP | `POST /app/auth/register/verify-otp` → `otp` |

---

## Login

| Screen field | API |
|--------------|-----|
| Email | `POST /app/auth/login` → `email` |
| Password | `password` |

---

## Forgot password (OTP)

| Screen step | API |
|-------------|-----|
| Email | `POST /app/auth/forgot-password/otp/send` → `{ email }` |
| Show error if unknown | **404** → **`Email ID does not exist.`** |
| OTP + new password | `POST /app/auth/forgot-password/otp/complete` → `email`, `otp`, `newPassword` |

---

## Other

| UI | API |
|----|-----|
| Google | `POST /app/auth/google` → `token` |
| Reset via email link (legacy web) | `POST /app/auth/forgot-password` then `POST /app/auth/reset-password` |
| Refresh token | `POST /app/auth/refresh` |
| Legacy passwordless OTP | `send-otp` / `verify-otp` |

---

## Demo credentials (staging only)

If seeded in your environment:

- Email: `test@realestate.com`
- Password: `Test@123`

Do **not** ship demo credentials in production app builds.
