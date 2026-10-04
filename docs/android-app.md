# Oak & Ember Android operator app

An installable Capacitor Android application, package `com.oakandember.operator`, version 1.0 (code 1), minimum Android 7 / API 24. Its interface is bundled in the APK; it starts in the private operator workspace rather than loading the public website. Internet and a current Android System WebView are required for authentication and backend operations.

The application uses the dedicated Oak Supabase project and the same authorized account as the web panel. There is no password, server key or automatic login in the APK. Only approved operators can read requests and access signed private-photo links. It supports reviewing requests, calling/emailing clients using the device's installed handlers, changing status, saving notes and internal quote drafts. Quotes are not automatically sent. No appointment, payment or notification is created.

Mobile detail navigation includes a return-to-list action. Android Back returns from a selected request, then exits from the list/login screen. The native WebView disables debugging and mixed HTTP content. Application backup is disabled. Saved auth sessions remain in the app's private storage until logout; request records remain on the server. Offline mode blocks writes rather than reporting a false success.

## Build

Node 22.12+, a complete JDK 21, Android SDK platform 36 and build-tools 35/36 are required.

```
npm ci
npm run android:sync
cd android
./gradlew assembleRelease lintRelease
```

Signing keys are never committed. `scripts/sign-android.sh` accepts a private keystore and a separate password file. Keep the signing backup securely: the same key is required to install updates over version 1.0. The password file belongs outside the repository.

The Android build deliberately excludes `public/` except for the operator logo; downloadable APKs and website photographs are not nested inside later APK builds.

## Checks

ESLint, existing 11 model tests, website and Android interface builds; Android release compilation and lint (warnings only); APK signature verification. Browser testing of the packaged interface against the real backend confirmed authorized login, private inbox, notes/quote/status persistence after reload, offline-write blocking and logout. Layout checked at 320/390/1440 px. Temporary fixtures are removed after checking.

Installation and device-specific behavior require testing on the intended phone. Play Store publication, push notifications, automatic email/SMS, native customer app and persistent visit scheduling are outside this release.
