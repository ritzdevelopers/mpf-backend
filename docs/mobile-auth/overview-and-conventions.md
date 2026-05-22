# Overview & conventions

## Base URL

- Production example: `https://apis.mypropertyfact.in/api/v1`
- Local example: `http://localhost:8005/api/v1`

All paths below are appended to this base (no trailing slash on the base).

## Path prefixes

| Prefix | Use |
|--------|-----|
| **`/app/auth`** | **Use this for consumer mobile apps.** |
| `/auth` | Legacy / shared; same behaviour for equivalent routes when documented. |

Example: **`POST /api/v1/app/auth/register/send-otp`**.

For the full registration → login → forgot-password sequence, see **[authentication-flow.md](./authentication-flow.md)**.

## Headers

- **`Content-Type: application/json`** on requests with a body.
- After login: **`Authorization: Bearer <access_token>`** for protected APIs.

## Tokens on native apps

Web may rely on HttpOnly cookies (`token`, `refreshToken`). **Mobile apps should read JWT fields from JSON responses** (`token`, `refreshToken`, `expiresIn`, `user`) and store them in **Keychain / EncryptedSharedPreferences** (or equivalent), not plain AsyncStorage-only storage.

See also: [Tokens & errors](./tokens-and-errors.md).
