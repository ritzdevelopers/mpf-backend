# Tokens, session, logout, errors

All routes below support **`/app/auth`** with **`/auth`** equivalents unless noted.

---

## Refresh tokens

**`POST /app/auth/refresh`**

```json
{ "refreshToken": "…" }
```

Returns new **`token`** and **`refreshToken`** (same envelope as login when successful).

---

## Validate access token

**`POST /app/auth/verify`**

Header: **`Authorization: Bearer <token>`**

---

## Logout

**`POST /app/auth/logout`**

Clears cookies for web. **Mobile:** delete stored tokens locally after calling this if you use it; otherwise clear local secure storage only.

---

## Session (optional)

**`GET /app/auth/session`** — same JWT/session shape as web when cookies are used; native apps usually rely on **`verify`** + stored JWT instead.

---

## Error shapes

Often:

```json
{ "message": "Human-readable message" }
```

or:

```json
{ "error": "code_or_snippet", "message": "Human-readable detail" }
```

Typical mapping: **400** validation / bad OTP, **401** auth / expired refresh, **403** forbidden.
