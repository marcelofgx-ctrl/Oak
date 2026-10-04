# iPhone / iOS operator access

Oak & Ember currently supports iPhone through the private web app at:

`https://oak-79f.pages.dev/operator`

This is a full-screen PWA, not a native IPA/App Store application.

## Install

1. Open the operator URL in Safari on an iPhone.
2. Share → Add to Home Screen.
3. Open the Oak & Ember icon.
4. Sign in with an authorized Oak & Ember operator account.
5. In **App y alertas**, enable notifications and accept the iOS permission when offered.

The PWA uses the same production Supabase records as the public website and the native Android app. Customer data is not cached for offline browsing.

## Notifications

Web Push notifications are intentionally generic and do not expose customer name, phone, address, email or photographs on the lock screen. Opening a notification leads to the authenticated operator workspace.

Push is a convenience layer rather than a delivery guarantee. The request itself is committed to Supabase independently of whether a notification reaches the phone.

## Current status

- Web/PWA code and backend integration are implemented.
- Browser-level behavior and production build are covered by repository tests/build checks.
- Physical iPhone installation and real Apple Web Push reception still require device verification.

A future native iOS app can consume the same Supabase backend without changing the customer website or Android architecture, but no native IPA is currently claimed as implemented.
