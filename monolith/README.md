# AuraHealth Backend (monolith)

Spring Boot 3.3 service that serves both the REST API (`/api/**`) and authentication.
Runs on **`http://localhost:8060`** in dev (the origin the frontend proxies to).

## Authentication

Two login methods share **one** session mechanism — a signed JWT carried in the
`AURA_SESSION` HttpOnly cookie (`SameSite=Lax`, `Path=/`, 24h). `JwtTokenFilter`
reads that cookie (or an `Authorization: Bearer` header) on every request.

| Method | Endpoint | Notes |
| --- | --- | --- |
| Email / password | `POST /api/auth/register`, `POST /api/auth/login` | returns `{ "user": {...} }`, sets cookie |
| Session probe | `GET /api/auth/me` | returns the user object directly, `401` if no session |
| Logout | `POST /api/auth/logout` | clears the cookie |
| **Google** | `GET /oauth2/authorization/google` | full-page browser navigation |

The cookie is issued by a single `SessionCookieFactory`, so a session from Google
login is byte-for-byte identical to one from email/password login.

### Google OAuth2 flow

1. Frontend "Continue with Google" button → `window.location.assign('/oauth2/authorization/google')`.
2. Backend 302-redirects to `accounts.google.com`.
3. After consent, Google calls back `GET http://localhost:8060/login/oauth2/code/google`.
4. `OAuth2SuccessHandler` extracts `email` + `name`, **finds-or-creates** the user by
   email (new users get `Role.PATIENT`, provider `GOOGLE`), issues the `AURA_SESSION`
   cookie, and redirects to the frontend root (`http://localhost:5174/`).
5. Frontend mounts, calls `GET /api/auth/me`, and picks up the session.
6. On any failure, `OAuth2FailureHandler` redirects to `http://localhost:5174/?error=oauth`.

`/oauth2/authorization/google` and `/login/oauth2/code/google` are provided automatically
by Spring Security — they are not hand-written.

## Required environment variables

| Variable | Required | Default (dev) | Purpose |
| --- | --- | --- | --- |
| `GOOGLE_CLIENT_ID` | **yes** | — | Google OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | **yes** | — | Google OAuth client secret |
| `AURA_FRONTEND_URL` | no | `http://localhost:5174` | where OAuth redirects back to |
| `AURA_JWT_SECRET` | prod | dev fallback | JWT signing key (≥ 32 chars) |
| `AURA_COOKIE_SECURE` | prod | `false` | set `true` in prod (https) so the cookie is `Secure` |
| `SERVER_PORT` | no | `8060` | backend port |
| `AURA_MOCK_AUTH_ENABLED` | no | `true` | dev-only mock login bypass; **disable in prod** |

The client ID/secret are **never** committed and never sent to the frontend — they
are read from env vars only (`application.properties` references `${GOOGLE_CLIENT_ID}` /
`${GOOGLE_CLIENT_SECRET}`).

### Run

```bash
export GOOGLE_CLIENT_ID="<your-client-id>.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="<your-client-secret>"
./mvnw spring-boot:run
```

## Where the Client ID / Secret come from — Google Cloud Console

1. Google Cloud Console → **APIs & Services → Credentials → Create credentials → OAuth client ID**.
2. Application type: **Web application**.
3. **Authorized redirect URIs** (must match byte-for-byte, or Google returns
   `redirect_uri_mismatch`):
   - dev: `http://localhost:8060/login/oauth2/code/google`
   - prod: `https://<your-domain>/login/oauth2/code/google`
4. (Authorized JavaScript origins are not required — the redirect is server-side.)
5. Copy the generated **Client ID** and **Client secret** into the `GOOGLE_CLIENT_ID` /
   `GOOGLE_CLIENT_SECRET` env vars above.

> ⚠️ **Rotate the old secret.** A real Google client secret was previously committed to
> this repo (in `application.properties` and in the checked-in `gateway/target/` build
> output). Those have been replaced with env-var references and the build artifacts
> untracked, but the secret still exists in git **history** — treat it as leaked and
> rotate it in the Google Console.
