# Oak & Ember architecture

## Shared production flow

```text
Customer browser
      |
      v
Public React/Vite site
      |
      v
Supabase Edge Function: oak-intake
      |
      +--> oak_requests
      +--> oak-request-photos
      +--> push queue
               |
               +--> operator PWA / iPhone Web Push

Authorized team
      |
      +--> Native Android app (Kotlin / Compose)
      +--> Web/PWA operator panel (/operator)
               |
               v
        Supabase Auth + RLS
               |
               v
          same oak_requests
```

## Source of truth

- GitHub `marcelofgx-ctrl/Oak`, branch `main`: application source.
- Supabase project `ttroukmpyerhtkskpdeg`: production data/backend.
- Cloudflare Pages: public web delivery.
- GitHub Actions: reproducible verification/builds.

## Product boundaries

The public website is for customers and must never expose private operator data.

The `/operator` route and native Android app require an authorized Supabase account and RLS-protected membership.

The current Android operator app is genuinely native. Historical TWA/Capacitor Android experiments are retained only in Git history and should not be used as the canonical Android product.

The current iPhone path is the installable operator PWA. It is not represented as a native iOS binary.

## Security principles

- No Supabase service-role key in browser or Android client code.
- Public request validation is repeated server-side.
- Customer photos are private and retrieved only by authorized operators.
- Push notifications contain no customer-identifying content.
- Operator routes are excluded from search indexing.
- Production web security headers are versioned in `public/_headers`.

## Release discipline

A feature can be described as implemented only after it exists in source. Builds and device behavior are tracked separately. Android release signing must use a stable private signing key outside Git before the app is treated as an updateable production release.
