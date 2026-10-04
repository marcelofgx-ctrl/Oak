# Operator workspace — first demo

Open `/operator`. This is a mobile-friendly web workspace, not an Android APK. It has no authentication or server connection and must use fictitious data only. Two clearly identified sample consultations demonstrate the workflow. Refreshing clears all edits. No messages, quotes or confirmations are transmitted.

Public estimates now ask for service/details/photos, name, ZIP and the preferred contact channel. The alternate contact channel and full address are optional; no visit date is requested at intake. The supplied gallery is presented as a first selection with more photographs to come.

Operator capabilities:
- Search/filter consultations; manually add a sample phone inquiry.
- Update request status and internal notes in memory.
- Record a sample scope and USD quote. No quote is sent and there is no electronic acceptance or payment.
- Propose an evaluation or work visit with address, estimated duration and local-browser date/time.
- Separate tentative visits from explicitly recorded client agreement.
- Reject past/incomplete visits and overlaps, including a fixed 30-minute travel allowance. Cancelled visits release the interval.
- Show a chronological agenda; cancel or explicitly confirm a tentative demo visit.

The single shared agenda is appropriate to prototype the initial small-team workflow. Production needs team/resource assignment, service-area/route rules, America/New_York time-zone handling, adjustable buffers, rescheduling and database-level concurrency checks. A client accepting a quote must not automatically book a job.

Before real use: authenticated operators, private customer records/photos, server validation, persisted quotes and history, permission separation, privacy/retention policy, secure intake, notification delivery and confirmed business details. Then an Android client can use the same backend. Never turn this public demo into a real operator workspace simply by removing its banner.

Verification: eleven unit tests, lint, production build and git whitespace checks. Chromium exercises contact-only estimate completion, sample quotes, tentative visit creation, explicit confirmation/cancellation, manual inquiries, filtering, loss of demo state on reload and mobile/desktop overflow checks. Screenshots inspected. Portfolio image enlargement was retained.
