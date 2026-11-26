# Admin sign-in with an OIDC provider (Pocket ID)

Issue #193.

## What, Why and Constraints

Admins can sign in to the admin pages with an OIDC provider, such as Pocket ID, instead of the admin password.

- **No provider configured** (`ADMIN_OIDC_ISSUER` unset): nothing changes. The page asks for the password, and the API wants it
  in `X-Admin-Token`.
- **Provider configured**: the page shows "Sign in with Pocket ID", and the API wants the provider's session. The
  password is refused.

Constraints:

- The provider is optional. Quarkus OIDC refuses to start without `quarkus.oidc.auth-server-url`, so it has to be off when
  no provider is configured.
- The admin page calls the API with `fetch`. An expired session must not answer with a redirect to the provider: fetch
  would follow it and fail on CORS.
- Behind the HTTPS reverse proxy (Dokploy/Traefik), the app sees plain http, but the callback URL sent to the provider
  must be https.
- Signing out of the admin page must not sign the user out of Pocket ID.
- Optionally, only the members of one provider group are admins. Pocket ID can also restrict the client to some groups.

## How

**Configuration** (`application.properties`)

- `quarkus-oidc` is a dependency now, in `web-app` mode (authorization code flow, encrypted session cookie `q_session`).
- `quarkus.oidc.auth-server-url`, `client-id` and `credentials.secret` come from `ADMIN_OIDC_ISSUER`,
  `ADMIN_OIDC_CLIENT_ID` and `ADMIN_OIDC_CLIENT_SECRET`, empty by default.
- The scopes add `groups`. Pocket ID then puts the user's groups in the ID token, and Quarkus makes them the user's roles.
- The session cookie's path is `/api/admin`, so the cookie goes to the admin API only.
- `java-script-auto-redirect=false`: a request with `X-Requested-With: JavaScript` and an expired session gets 499
  instead of a redirect.
- In prod, `force-redirect-https-scheme` and `cookie-force-secure` are set, for the reverse proxy.
- Keycloak dev services are off.
- New properties `admin.oidc.group` (`ADMIN_OIDC_GROUP`) and `admin.oidc.provider-name` (`ADMIN_OIDC_PROVIDER_NAME`,
  default `Pocket ID`).

**Switching OIDC on and off**: `admin/api/internal/OidcSwitch`

- A SmallRye `ConfigSourceFactory`, registered in `META-INF/services/io.smallrye.config.ConfigSourceFactory`.
- It sets `quarkus.oidc.tenant-enabled` to whether `quarkus.oidc.auth-server-url` has a value.
- Its ordinal is 100. That is above the "Runtime Values" source (0), where Quarkus records the value computed at build
  time, when no issuer is known. It is below `application.properties` (250) and the environment (300), so they can still
  set the switch.
- If the provider can't be reached at startup, the app still starts: Quarkus logs a warning and connects on the first
  sign-in. This was checked with the packaged app and an unreachable issuer.

**Backend**: module `admin`

- `AdminSignIn` gives the sign-in method (OIDC when the issuer is set), the optional group and the provider's name.
- `AdminTokenFilter` (`@AdminProtected`):
  - With OIDC, it checks the JAX-RS `SecurityContext`. With no user it answers 401. When a group is configured and the
    user isn't in it, it answers 403. The `X-Admin-Token` header is ignored.
  - Without OIDC, it checks the password as before.
- `AdminSessionResource` (`/api/admin/session`):
  - `GET`: `{"method": "PASSWORD"}` or `{"method": "OIDC", "provider": "Pocket ID"}`, for the page.
  - `GET /login` (`@Authenticated`): the provider's callback URL. Unauthenticated, Quarkus redirects to the provider.
    Back with the code, Quarkus creates the session, and the method sends the browser to `/admin` (303). Without OIDC it
    is never reached (401).
  - `POST /logout`: `OidcSession.logout()` removes the session cookie. The user stays signed in at Pocket ID.
- The module's `requires` add `io.quarkus.security.api`, `io.smallrye.config`, `io.smallrye.mutiny` and `quarkus.oidc`.

**Frontend**: `biz/admin/admin-page.js`

- On load, the page asks `/api/admin/session` how to sign in. The sign-in card stays hidden until it knows, so the wrong
  form never flashes.
- **Password**: unchanged.
- **OIDC**:
  - The card shows a "Sign in with Pocket ID" link to `/api/admin/session/login`. The page loads the stats right away,
    since the cookie may already be there.
  - API calls send `X-Requested-With: JavaScript` instead of the token. The module report fetch does too.
  - A 401 or 499 just shows the sign-in link. A 403 shows "This account is not an admin." and ends the session, so that
    another account can sign in.
  - Sign out posts to `/logout`.

**Docs**: the README lists the new variables and the Pocket ID setup. The callback URL to register is
`https://<host>/api/admin/session/login`.

## Tests

- `AdminTokenFilterTest` gets 4 more unit tests:
  - with OIDC, a signed-in user passes and the password alone is refused (401);
  - with a group, a user outside it gets 403 and a member passes.
- `AdminPasswordSessionTest`: without a provider, the method is `PASSWORD` and `/login` is refused.
- `AdminOidcProfile`: a test profile with Quarkus' WireMock OIDC provider (`quarkus-test-oidc-server`). Its login form
  signs in alice, in the groups `admin` and `user`, and the profile sets `admin.oidc.group=admin`.
- `AdminOidcResourceTest`:
  - the method is `OIDC` / `Pocket ID`;
  - the password is refused on the stats and the module report;
  - `/login` redirects to the provider with the `groups` scope and the callback URL.
- `AdminOidcPageTest` (Playwright), with the real code flow through WireMock's login form:
  - signing in shows the stats, which survive a reload; signing out shows the sign-in link again, also after a reload;
  - the module report opens with the provider's session;
  - an account that isn't an admin (stats stubbed to 403) sees the message, and its session is ended.
- The existing password tests (`AdminPageTest`, `ModuleReportResourceTest`) pass unchanged. Full build: 466 tests.
- Screenshots of the sign-in card at 390×844 and 1280×800.
