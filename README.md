# Oak & Ember

Native Android administration app for Oak & Ember.

## Native Android v0.2.0 — live database

This is a real Android application built with Kotlin + Jetpack Compose. It does **not** use WebView and does not wrap a website.

The app now connects directly to the production Oak & Ember Supabase project:

- Authenticated operator sign-in
- RLS-protected live requests from `oak_requests`
- Automatic refresh every 30 seconds while the app is open
- Manual refresh
- Search and status filtering
- Dashboard based on real records
- Clients derived from real requests
- Calendar / unscheduled work view
- Native call, email and map actions
- Private customer photos loaded from `oak-request-photos`
- Operator-only status and internal note updates written back to Supabase
- Session tokens stored with Android encrypted preferences

No demo customer records remain in the native source.

Package: `com.oakandember.admin`

Version: `0.2.0` (`versionCode 2`)

## Backend

Supabase project: `Oak & Ember`

Project ref: `ttroukmpyerhtkskpdeg`

The publishable key in the Android app is intentionally a client-side publishable key. Database access remains protected by Supabase Auth + Row Level Security. No service-role key is stored in the APK.

## Build

GitHub Actions workflow:

`.github/workflows/android-native.yml`

Artifact:

`OakAndEmber_Admin_Native_Live_v0.2.0.apk`
