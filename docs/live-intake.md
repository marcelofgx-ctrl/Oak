# Live Oak & Ember intake

Dedicated Supabase project: ttroukmpyerhtkskpdeg, us-east-1. No changes to Traslados. Project creation reported USD 0/month; ongoing usage remains subject to the provider's quotas.

Public intake uses a JWT-verified Edge Function. It validates all steps, enforces five JPEG/PNG/WebP images of up to 8 MiB each, checks image signatures, caps the body at 43 MiB and limits submissions to five per contact per hour and 100 globally per hour. These controls are not a substitute for CAPTCHA if targeted abuse develops. The anon JWT is a public compatibility credential, not an operator authorization token. Only the server runtime has the service role key.

Requests become visible after private photo storage succeeds. Failed uploads are cleaned up. A stable UUID supports retry without duplicate requests. The public response includes only a receipt, never customer data. No automatic appointment, payment, email or SMS is created.

`/operator` requires a Supabase Auth account and a matching `oak_operators.user_id` membership. Signing in alone grants no access. The inbox reads persisted requests and saves statuses, internal notes and internal quote drafts. Photo links are signed for five minutes. The previous local demonstration is retained as unused source code, not routed publicly.

The owner account has been enabled with an explicit operator membership; login and logout were verified. Additional team accounts must be provisioned through Supabase Auth and explicitly added to `oak_operators`. Never put a password or service key in the repository. There is no public signup UI. Password recovery, automated operator provisioning, automatic notifications, visit scheduling, formal retention/deletion policy remain future work. The signed Android operator app is now available; see docs/android-app.md. A receipt means the request was saved; it does not mean the operator has reviewed it.

Verification: lint, build and 11 model tests; live Edge Function request with private photo; stable-UUID retry; invalid ZIP rejected; anonymous table read denied; browser submission with photo on mobile; operator login gate; no horizontal overflow at 320/390/1440. Owner login and persisted status, notes and quote editing were subsequently verified against the backend as part of the Android interface checks.

Schema source is `backend/schema.sql`; deployed function source is `backend/intake.ts`, paired with `src/requestModel.js`. The environment's CLI invocation failed, so this schema was applied through the Supabase SQL connector. The sole security advisor notice is intentional: `oak_intake_limits` has RLS with no public policy, because only service_role may access it. Reference: https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy
