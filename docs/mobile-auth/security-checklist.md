# Security checklist (mobile)

- Use **HTTPS** in production only.
- **Rate-limit** sensitive endpoints (`login`, `signup`, `send-otp`, `forgot-password`) on the API gateway or backend.
- Store **`refreshToken`** (and **`token`** if persisted) in **secure hardware-backed storage**, not plaintext preferences alone.
- Rotate refresh tokens according to your refresh API behaviour.
- Native apps do **not** hit browser CORS; WebViews pointing at web origins still need correct CORS on the API.

Cross-reference: [Overview](./overview-and-conventions.md), [Email OTP](./email-otp.md).
