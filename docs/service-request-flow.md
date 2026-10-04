# Guided service requests

The public website now offers a four-step local demo: service selection, hearth details and photos, contact/preferences, and final review. Mobile quick navigation provides Home, Services and Request links. Service-card links preselect the appropriate service. This is a browser experience, not an installed Android application or PWA.

## Business rules

Cleaning and inspections create service/inspection requests. Repairs create evaluation requests. Fireplace and stove installations create project consultation requests. No prices, service eligibility, inspection level or appointment availability are inferred. A ZIP code is collected, but coverage still needs confirmation. Date and time are preferences, not reservations.

Demo completion makes no HTTP request and clears entered customer details and photo previews from React state. Photos use temporary blob URLs, revoked on removal, completion and unmount. No customer data is stored in local/session storage. Client photo checks are only UX checks, not production security: a real backend must inspect content, validate requests and restrict private storage.

## Next production phases

1. Confirm service area, contact channels, services and privacy policy.
2. Implement a server-backed request adapter with validation, spam controls and private photo storage. Separate request receipt from appointment confirmation. Offer secure request-tracking links without mandatory accounts.
3. Add operator workflow: review, request more information, propose assessment/quote, confirm a visit, complete a job. Internal notes must never be sent to clients.
4. Add customer quote acceptance, rescheduling and reminders. Introduce payment/deposit only after business policies and a provider are agreed.
5. Build the operator Android app against the shared authenticated backend. Do not collect payment data directly.

No backend, quotes, payments, accounts, booking engine, notifications or Android app is connected by this change.

## Verification

`npm ci`, `npm test` (6 unit tests), `npm run lint`, `npm run build`, and `git diff --check` were run. Chromium browser checks covered mobile navigation, step errors, local photo preview/removal/type rejection, past-date validation, review edits preserving state, demo acknowledgement, completion/reset, service-card preselection, no external data requests, and no horizontal overflow at 320, 390, 768 and 1440 pixels. Mobile and desktop screenshots were inspected. This does not replace a real-device camera/keyboard check or a production backend end-to-end test.
