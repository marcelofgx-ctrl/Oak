# Oak & Ember

Oak & Ember is the shared service platform for the family chimney and fireplace business in the greater Atlanta, Georgia area.

The repository now keeps the customer-facing website, private operator web/PWA experience, live Supabase integration and the native Android administration app together while preserving a single production data source.

## Product surfaces

### Public customer website

Route: `/`

Stack: React + Vite.

Capabilities:
- Mobile-first Oak & Ember website.
- Real supplied project photographs and before/after presentation.
- Service selection and a guided four-step request flow.
- Real live intake through the production Supabase Edge Function.
- Secure photo submission.
- Clear consent, validation, retry-safe request reference and no automatic appointment booking.
- Accessibility, responsive navigation, offline warning, FAQ, SEO metadata and Cloudflare security headers.

### Team web / iPhone PWA

Route: `/operator`

The authenticated team panel uses the same production Supabase project as the website and Android app. On iPhone it can be added to the Home Screen and use Web Push on supported iOS versions. It is a PWA, not an IPA/App Store binary.

### Native Android administration app

Directory: `android/`

Stack: Kotlin + Jetpack Compose.

Current line: `OakAndEmber_Admin_Native_Live_v0.2.1`.

Capabilities include authenticated live requests, search/filtering, customer grouping, collapsible scheduled-day calendar, private photos, calls/email/maps, status and internal-note updates, encrypted session storage, live refresh and Android insets handling.

This native app is the canonical Android operator experience. Earlier TWA/Capacitor experiments remain in Git history but are not the current Android architecture.

## Backend

Production Supabase project: `Oak & Ember`

Project ref: `ttroukmpyerhtkskpdeg`

Shared production components include:
- `oak_requests`
- `oak_operators`
- `oak-request-photos`
- Supabase Auth + RLS
- `oak-intake` Edge Function
- `oak-push` Edge Function and Web Push queue

The public site never contains a service-role key. Customer intake uses the public client credentials intended for browser use, while privileged writes are enforced server-side.

## Delivery

- Public web: Cloudflare Pages from GitHub.
- Web verification: `.github/workflows/web.yml`.
- Android APK verification/build: `.github/workflows/android-native.yml`.
- GitHub `main` is the source of truth.

## Development rules

Preserve working production behavior and make incremental changes. For Android, keep the distinction between:

- **IMPLEMENTED IN CODE**
- **BUILD VERIFIED**
- **DEVICE VERIFIED**

Do not reintroduce demo customer records into production-facing code.
