# Mobile app authentication — documentation index

Consumer-facing auth uses **`/api/v1/app/auth`** (recommended). Legacy **`/api/v1/auth`** exposes some equivalent handlers where noted.

| Doc | Contents |
|-----|----------|
| **[Authentication flow](./authentication-flow.md)** | **Start here** — registration (email + password + OTP), login, forgot-password OTP |
| [Overview & conventions](./overview-and-conventions.md) | Base URL, headers, `/app/auth` vs `/auth`, storing tokens on mobile |
| [Email OTP](./email-otp.md) | OTP timing, digit length, legacy vs registration-specific OTP |
| [Email & password](./email-password-signup.md) | Login + registration OTP endpoints |
| [Social login](./social-login.md) | Google today; Apple (planned contract) |
| [Forgot / reset password](./forgot-reset-password.md) | **OTP forgot flow** + legacy email-link reset |
| [Tokens & errors](./tokens-and-errors.md) | Refresh, verify token, logout, typical error shapes |
| [Security checklist](./security-checklist.md) | HTTPS, rate limits, secure storage |
| [UI field checklist](./field-checklist.md) | Maps screens to endpoints + staging demo login |

Open **[authentication-flow.md](./authentication-flow.md)** for the step-by-step product flow.
