# Oak & Ember Android 1.1.1

Download: https://oak-79f.pages.dev/downloads/oak-ember.apk (redirects to version 1.1.1).
Versioned file: https://oak-79f.pages.dev/downloads/oak-ember-1.1.1.apk.

## Update and activate alerts

1. Download and install over the existing Oak & Ember app. Do not uninstall first. Package com.oakandember.operator and the release certificate are unchanged; versionCode is 3 / versionName 1.1.1.
2. Keep Chrome updated. Open the Oak & Ember icon; sign in if requested.
3. App y alertas → Activar alertas. Accept the notification permission.
4. Probar alerta; check the notification on the phone. Android notification settings, battery restrictions and Do Not Disturb can affect delivery or banners.

Minimum Android 7 / API 24. The APK uses Android Browser Helper 2.7.3 to open the verified Oak HTTPS origin as a Trusted Web Activity through Chrome. It displays the same live operator workspace, with the refined paper texture and forest/copper palette, instead of the old bundled Capacitor WebView. Future workspace improvements arrive through the same site. Internet is required; no private offline cache is created.

The signed package/domain association is published at /.well-known/assetlinks.json; the matching web-origin statement is included in Android resources. If browser verification cannot complete, a browser toolbar can appear rather than an unverified fullscreen origin. The activity ignores arbitrary incoming URLs and always launches the private operator route.

## Notifications and privacy

Web Push uses the existing authenticated oak-push endpoint, server-side VAPID signing and a transactionally queued event on real intake. Chrome provides the background push transport. The Trusted Web Activity delegation service verifies the browser token, exposes a white notification icon, requests POST_NOTIFICATIONS on Android 13+, and enables a high-priority channel for popup notifications where OS settings allow. The app never embeds an FCM server key or a password and does not poll in a background service.

Notification payloads reveal no customer details. An authorized login is required to read the private inbox. Statuses, notes and internal quotes remain on the same Oak backend used by iPhone and the web panel. Closing the session/desactivating alerts cancels the browser subscription. Authentication is now in Chrome's origin storage, so the former WebView session is not migrated; a new login can be required, but stored inquiries and quotes are unchanged.

See docs/ios-app.md for server delivery, retry limits and subscription ownership checks. Version 1.0 remains archived and has no push integration. Version 1.1 had a startup defect and its public download redirects to the corrected 1.1.1.

## Build and signing

Node 22.12+, complete JDK 21, Android SDK 36 and build-tools 36 are required.

```sh
npm ci
npm run build
cd android
./gradlew assembleRelease lintRelease
```

Do not run cap sync for this version: the app is a TWA. The android:sync script is retained as a compatibility alias for the web build. Native source settings do not include Capacitor modules or obsolete WebView assets.

Sign using scripts/sign-android.sh <private-keystore> <password-file> <output-apk>. Signing material remains outside Git; retain the same private backup for future updates. APK v2/v3 signatures and the unchanged certificate were checked. Public asset links contain only the certificate fingerprint, never the private key.

## Verification and remaining phone checks

- Release compilation and Android lint; frontend lint, build and 14 tests.
- APK package/version/permissions, v2/v3 signature and certificate equality with 1.0; domain association matches the same fingerprint.
- Chromium with Android viewport/user agent: real authorized login, Android-specific settings, activation control available, app context preserved across navigation, logout and no overflow at 320/390/1440.
- Previous server checks verified subscription registration and ownership restrictions, private-key protection and generic notification handling.
- Physical Android installation, fullscreen verification, native delegated permission prompts and push receipt are not verified by Chromium emulation. Use Probar alerta on the intended phone. The environment's prior Android emulator had an unrelated WebView crash and no compatible Chrome installed.

No Play Store publication, automatic appointment, payment, email or SMS is introduced.

## Startup correction in 1.1.1

Android Browser Helper invokes ManageDataLauncherActivity.addSiteSettingsShortcut during launch. The 1.1 manifest omitted that activity, causing IllegalArgumentException: Component class ...ManageDataLauncherActivity does not exist. The crash was reproduced in the Android 15 emulator. Version 1.1.1 declares the management activity, its private-panel URL and application manageSpaceActivity. The same signing key and a higher versionCode preserve update compatibility.

The regression script scripts/check-android-startup.py checks the packaged manifest, installs with -r on a disposable emulator, performs two cold starts and rejects any Oak crash in logcat. It refuses physical devices. The complete Chrome workspace and actual background push still require the phone; no compatible Chrome is installed in this emulator.
