# Hood Up — live demonstration script

Plan approximately **10–12 minutes plus reboot time**, then rehearse against the instructor's actual limit. This is an execution plan, not a record of successful device tests. Use the speaking parts in [SLIDES.md](SLIDES.md), assigning every actual member at least one spoken step. A second device/emulator for the admin avoids repeated logout/login on the customer phone.

## Before entering the room

1. Install the **debug APK** on the customer test device if you want Database Inspector, `run-as` or debugger evidence. The demo release is not debuggable even though it is signed with a debug certificate. Install/build instructions are in [RELEASE.md](RELEASE.md).
2. Use a dedicated demo Firebase project, publish reviewed `firestore.rules` and the required indexes, enable email/password Auth and approve the admin UID through the trusted owner console. Do not attempt to register an admin merely by entering the APK invite.
3. Seed the catalog as admin, then stop seeding: it resets demo catalog edits. Cache workshops/services and add a car while online. Use demo-owned accounts/details and silence unrelated personal notifications. Never project `.env`, passwords, API keys or service-account files.
4. Start the customer without an active Plus subscription for the first booking. Use a separate fresh customer if necessary; the app has no “reset Plus” control. Enable appointment reminders in Profile and grant notifications on Android 13+ (or enable them in Android settings if previously denied).
5. Find an available **Engine Oil Change** at **Gulshan Auto Care** or another seeded workshop offering it. The seeded price is ৳3,500 and duration 40 minutes. Plan a future non-priority hourly slot within opening hours (Gulshan opens 09:00–20:00; a standard customer starts from 10:00 if free). Never choose a past date just to make a chart look populated.
6. Name the main queued request **A** in your rehearsal notes. A second booking **B** will remain CONFIRMED for reminder/reboot evidence; choose a different future slot whose reminder trigger is still ahead. Keep IDs in private notes so the admin can locate the correct rows.
7. Open code tabs: `AppExecutors`, `LocalDb`, `BookingsRepository`, `BookingSyncService`, `SyncCoordinator`, `BookingStatusReceiver`, `BootReceiver`, `GeminiProtocol`, `BusinessConfig`. Fold unrelated code and private values. Prepare Firestore's bookings view with only demo data.
8. On API 26+ with a debuggable app, prepare Android Studio Database Inspector for `hood_up.db`. Show counts/statuses rather than personal payloads. If using an API 24 phone, Database Inspector is not the fallback proof: use app offline behavior and show the SQLite DAO/schema source instead.
9. Rehearse Gemini before class using the configured, appropriately restricted demo key. If it fails, demonstrate the **labelled local fallback honestly** and show the Gemini integration source; do not call fallback output a live Gemini result.
10. Record one genuine rehearsal clip of a brief active-upload notification and one of reboot reminder restoration if timing is uncertain. Label clips “Recorded rehearsal.” They are supporting evidence, never a substitute falsely described as live behavior.

## Live order

| Step / presenter | Actions and words to say | Visible evidence / requirement |
| --- | --- | --- |
| 1 — Part A: entry and navigation | Launch the customer app. Explain that Splash checks Auth/session and routes by role. Point to the greeting, search and bottom bar; open a workshop and return. | Activities, Intents and basic layouts. Home/list/detail are distinct Activities. |
| 2 — Part A: catalog and external Intent | Search Gulshan, toggle Featured and All, open a workshop, show service price/duration. Open Call to the dialer, then Back **without placing a call**. Optionally open Directions. | Live ListView/filtering; Featured badge and ordering; ACTION_DIAL/ACTION_VIEW. Do not imply a real partnership or place an unsolicited call. |
| 3 — Part A: profile/car persistence | Open Profile → My Cars. Show or add the demo car, then return. Explain that contact email is not a change to sign-in email. | CarForm Intent, validated form, Firestore owner car and SQLite mirror. If time permits, reopen Cars offline to show cached data. |
| 4 — Part D: AI-to-booking path | Open Ask the Mechanic, choose “Strange noise when braking,” wait for a real response, show a catalog suggestion and urgency. Tap a card → Pick a workshop → Booking and point out the prefilled note. Return without submitting this draft. | AI integration and Intent extras. Identify whether the reply is Gemini or **offline suggestion**. Catalog prices come from ServiceItem, not AI. Mention inspection is needed. |
| 5 — Part A/B: request A offline | Back at a cached Oil Change booking, select the saved car, future date and free non-priority slot. Turn airplane mode on, disable Wi-Fi if it remains connected, return and confirm once. Show the queued success message, then My Bookings → A. | Local storage and provisional offline booking. “Waiting to sync” is the key evidence; say that the slot is not yet accepted remotely. Home/My Bookings offline banner should agree. |
| 6 — Part B: local/remote contrast and threads | Show the local queue count with `SELECT COUNT(*) FROM pending_bookings;` in Database Inspector, and verify A's exact ID is absent from Firestore. Briefly show AppExecutors and LocalDb's worker-thread guard; optionally show a debugger worker thread during a DAO read. | Local + remote storage and threads. A responsive UI is supporting evidence, not proof of thread placement. Do not show secrets or claim a SQL inspection happened if only source is available. |
| 7 — Part C: reconnect and started Service | Restore internet and explicitly return to visible Home. Show the short Sync notification while the queue uploads if visible. Inspect `adb shell dumpsys activity services com.example.carworkshopbookingapp` promptly if useful. After acknowledgement, show A's Firestore document, no duplicate ID, queue removal and cleared waiting label. | BookingSyncService, background execution, remote persistence, network-restored custom broadcast. The Service may finish before inspection: show actual rehearsal evidence and its source instead of pretending it is still running. |
| 8 — Part C: admin, status broadcast and notification | On the admin device open Bookings, filter by workshop/PENDING and open A. Move PENDING → CONFIRMED, confirming the dialog. Keep customer My Bookings visible and show its update. Explain the package-only status action and `BookingStatusReceiver.onReceive → reload()`. If needed, demonstrate a debugger breakpoint at that receiver during the real change. | Admin authorization, Firestore transaction, custom broadcast, dynamic receiver and status notification. Live listeners also refresh data; a changed row alone does not isolate which mechanism delivered it. |
| 9 — Part C: READY and deep link | Admin advances A through IN_PROGRESS → READY. Open the customer notification shade and tap “Your car is ready.” Show the correct booking/timeline and use Back to Home. | Notification/deep-link PendingIntent and proper task stack. Keep the customer app visible while waiting for a status update; background-only checking is not guaranteed instant. |
| 10 — Part C/B: completion, rating and chart | Admin moves READY → COMPLETED. Customer opens History, rates A and saves. Show the stored rating/updated workshop aggregate. Admin refreshes Dashboard and shows commission/statistics/Canvas bars. | Bounded rating transaction and custom chart. A standard Oil Change stores ৳350 commission. Compare dashboard changes against its baseline; future appointment dates do not appear in the chart's past-seven-day bins. Monthly totals use appointment month. |
| 11 — Part D: business model and B | Profile → Hood Up Plus: read “Demo mode — no real payment,” Subscribe, then open another Oil Change booking. Show ৳3,325 total, first-hour eligibility if still free and the optional pickup request checkbox. Confirm B at a different future slot; admin leaves B CONFIRMED. | Actual 5% discount, commission on discounted total (৳332.50), simulated ৳199/month entitlement, priority eligibility and pickup **flag**. No payment/dispatch is claimed. Featured placement was shown in step 2. |
| 12 — Part C: reminder and genuine system broadcast | With B CONFIRMED and its four-hour trigger still future, inspect `adb shell dumpsys alarm` for this package. Reboot the customer device, unlock it and inspect the restored eligible alarm. Explain BootReceiver's BOOT_COMPLETED path and SQLite reload. | System broadcast, AlarmManager/reminder persistence and executor work. Reboot is the genuine unrooted-device route; do not assert an ordinary shell can inject a protected BOOT_COMPLETED broadcast. Keep B confirmed; completing/cancelling it removes the reminder. |
| 13 — All presenters: close and logout | Reopen the app after reboot. Show B still present and Profile's reminder setting. Log out, press Back and show that admin/customer screens cannot be reopened through that task. Each member states their actual contribution in one sentence. | Session cleanup, listener/service/job/notification/known-alarm cleanup and member participation. Finish with implemented limits and test evidence, not claims of production readiness. |

## Course-feature completion gate

Use this as a presenter checklist, not as pre-filled test results.

| Required item | Must be seen or explained with concrete evidence | Done |
| --- | --- | --- |
| Activity / Intent / Layout | Home → detail → booking and external dialer; ListView/Spinner/basic XML layout | ☐ |
| Local storage | Offline rows/chat and queued A; SQLite count/schema | ☐ |
| Remote storage | Auth identity, owner car and acknowledged Firestore A | ☐ |
| Threads | Executor/Handler and main-thread SQLite guard; worker inspection if available | ☐ |
| Service | A upload through BookingSyncService; finite foreground state and cleanup | ☐ |
| Custom broadcast | Network restore and actual booking-status receiver/reload path | ☐ |
| System broadcast | Real reboot restoring B's eligible cached reminder | ☐ |
| Notification | Submitted/status message, READY tap and detail → Home Back navigation | ☐ |
| Business model | Featured badge/order, stored commission and Plus's real price/flag changes | ☐ |
| AI | Real Gemini or explicitly labelled fallback, service card and booking note path; show schema/privacy code | ☐ |

## Timing and fallback rules

- Do not wait hours for an appointment reminder during class. A status notification already demonstrates Notifications. Show the computed alarm/receiver and real reboot restoration; actual inexact reminder delivery is an additional rehearsal test, not a guaranteed minute-by-minute event.
- If a request stays queued, check validated internet, rule/index publication, current price, slot availability and account ownership. Do not delete the SQLite row or edit a Firestore status to conceal a failed demo. Explain the actual state and use a labelled successful rehearsal clip if necessary.
- If notification permission was denied, enable it through Android settings before retrying. The one-time in-app explanation intentionally does not nag. Avoid changing app code or clearing the real demo account merely to reset permission state.
- If no Gemini response arrives, demonstrate offline matching and Retry. Source plus request-contract tests establish the integration code; only a genuine provider response establishes a successful live call.
- Private receivers are not exported for convenience. Rehearse commands only in an authorized environment. [prompt9-device-tests.md](prompt9-device-tests.md) explains `run-as`/root-emulator restrictions; do not run a broad broadcast command and treat a shell “completed” message as proof of receiver execution.
- If there is only one phone, capture the admin update beforehand or use a second emulator/account session. Logging the customer out during the notification stage prevents the intended owner listener from receiving updates.

## Prepared evidence, not invented results

Keep the actual `build/prompt14-checks.log`, JUnit report and `build/prompt14-rules.log` ready: the recorded results are 41 passing JVM tests, 56 passing rule cases, successful debug/release builds and lint with zero errors. API 24 verification so far is static/manifest/signature checking, not a recorded API 24 runtime pass. Before presenting, fill a rehearsal sheet with device model/API, date, account roles, observed sync/notification/reboot results and any failures. See [MANUAL_QA.md](MANUAL_QA.md) for the full phone checklist.
