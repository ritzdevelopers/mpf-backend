# Social login — Google & Apple

---

## Google (implemented)

**`POST /app/auth/google`** — equivalent: **`POST /auth/google`**

| Field | Required | Notes |
|-------|----------|--------|
| `token` | yes | Google **ID token** from mobile Sign-In |

```json
{
  "token": "<google_id_token_jwt>"
}
```

**200 OK** — `token`, `refreshToken`, `expiresIn`, `user`. New users are created without a password.

---

## Sign in with Apple (not implemented yet — intended contract)

**`POST /auth/apple`** (to be added under **`/app/auth/apple`** when implemented).

| Field | Required | Notes |
|-------|----------|--------|
| `identityToken` | yes | JWT from Apple |
| `authorizationCode` | optional | Server-side exchange if used |
| `user` | optional | Only on first sign-in — Apple may send `fullName`, `email` once |

**200 OK** — same JWT envelope as Google after validating Apple JWT (JWKS, `iss`, `aud` = Services ID) and upserting by Apple `sub`.
