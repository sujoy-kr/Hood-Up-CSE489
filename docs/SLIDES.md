# Hood Up — 13-slide presentation outline

This is a slide-and-speaker-notes plan, not a rendered deck. Use actual app captures with demo accounts; captions must identify local fallback and simulated subscription behavior. Do not include credentials, personal phone numbers or private console details.

## Speaking split

Four **planning parts** are shown below; this does not assert the group's size or actual authorship. Replace slots with the real roster. If there are fewer members, combine adjacent parts; if there are more, split slide notes/live-demo steps between additional named speakers while keeping **13 slides**. Every actual member must have an audible speaking segment, not just operate a phone silently. Fill the contribution table in REPORT.md separately from this presentation assignment.

| Planning part | Speaker placeholder | Slides | Live-demo handoff |
| --- | --- | --- | --- |
| A — problem and customer flow | [member name / ID] | 1–3 | Customer navigation and first booking |
| B — architecture and data integrity | [member name / ID] | 4–6 | SQLite queue, Firestore document and transaction explanation |
| C — Android platform and administration | [member name / ID] | 7–9 | Sync, broadcasts, notifications and admin status changes |
| D — AI, business model and evaluation | [member name / ID] | 10–13 | Assistant/fallback, Plus calculation and closing checks |
| Additional actual member(s), if any | [name / ID] | [specific slide notes or co-presented section] | [specific spoken demonstration step] |

## Slide 1 — Hood Up: from a symptom to a workshop booking

**Speaker:** Part A — [name]

**On slide:** app logo/title, Java Android/API 24+, customer/admin roles, real member names/IDs. One Home screenshot.

**Speaker notes:** “Hood Up connects choosing a service, finding a workshop and tracking a booking. We used the course's basic layouts and plain Activities. Our catalog uses Dhaka demo workshops and BDT prices. Today we will distinguish what the app implements from the external setup and phone checks still needed.”

**Evidence:** `Project Info.txt`, `HomeActivity`, manifest/build configuration.

## Slide 2 — The problem and intended users

**Speaker:** Part A — [name]

**On slide:** three short problems: uncertain service vocabulary; disconnected booking/status steps; interrupted connectivity. Customer/admin columns with their tasks.

**Speaker notes:** “A driver may describe a noise but not know which service to choose. The customer needs prices, a suitable time and progress updates; the admin needs a manageable booking list and catalog. We designed an offline request queue so losing connectivity does not erase the request. These are intended benefits, not survey findings.”

**Visual:** three labelled blocks; no invented usage statistics.

## Slide 3 — Customer journey and basic-layout UI

**Speaker:** Part A — [name]

**On slide:** Home → Workshop Detail → Booking → My Bookings/Detail; a second arrow Chat → Pick a workshop → Booking. Small screen captures/wireframes.

**Speaker notes:** “Home provides filters and a ListView. Intents carry the workshop/service IDs into booking. A Spinner selects a saved car and a GridLayout holds time chips. The bottom bar is a LinearLayout. Rounded XML shapes provide the cards and buttons; we did not use RecyclerView or Material. Call and Directions open external apps.”

**Handoff:** “Next, [Part B name] will show what preserves and checks this flow.”

## Slide 4 — Architecture and threads

**Speaker:** Part B — [name]

**On slide:** Activities → stores/repositories → SQLite/Firebase; separate AI branch. Label IO executor, HTTP pool and main Handler.

**Speaker notes:** “AppExecutors separates ordered local work from blocking Gemini HTTP, and posts UI updates through a main Handler. RemoteTasks wraps Firebase requests and result mapping. LocalDb rejects main-thread database opening. We can point to those classes for thread evidence; a smooth-looking screen alone is not proof. UI observers and listeners detach with their lifecycle.”

**Evidence:** `AppExecutors`, `RemoteTasks`, `LocalDb`; use README's ASCII diagram as a source.

## Slide 5 — Local cache, remote storage and offline queue

**Speaker:** Part B — [name]

**On slide:** cache first → fresh server result; offline request → `pending_bookings` → acknowledged `bookings` document → queue removal. Show Waiting to sync.

**Speaker notes:** “Workshops and services appear from SQLite before a Firestore refresh. Cars, chat and booking history also have local persistence. An offline booking is a saved request, not a confirmed slot. Once connected, sync checks it online and only removes the queue entry after local acknowledgement. Changed prices or conflicts can still require rebooking.”

**Evidence:** `CachedRepository`, `BookingSubmission`, `SyncRunner`, `LocalDb`.

## Slide 6 — Preventing duplicate and conflicting bookings

**Speaker:** Part B — [name]

**On slide:** stable booking ID; one transaction for booking plus hourly locks; owner-only rules; cancellation marker; a 90-minute service spans two locks.

**Speaker notes:** “A retry reuses the same document ID. The transaction rereads the service, car, membership and slot locks. A second customer cannot overwrite an occupied lock. Rules also validate the write, so bypassing a disabled button is not enough. A separate marker prevents an offline cancellation from being recreated by a delayed transaction.”

**Handoff:** “Now [Part C name] will show the Android components around that transaction.”

## Slide 7 — Started Service under Android restrictions

**Speaker:** Part C — [name]

**On slide:** visible Home → BookingSyncService → active-upload notification → bounded listener → stop; background network return → JobScheduler.

**Speaker notes:** “BookingSyncService is started, not bound. It performs finite work and uses foreground state during active uploads. We avoid treating a connectivity callback as permission to start a foreground service from the background. SyncCoordinator chooses the visible-activity path or a JobScheduler fallback. Visible listeners update promptly; background checking is best effort, not instant push.”

**Evidence:** `BookingSyncService`, `SyncCoordinator`, `BookingSyncJob`, `SyncRunner`.

## Slide 8 — Broadcasts, notifications and reminders

**Speaker:** Part C — [name]

**On slide:** NetworkCallback → NETWORK_RESTORED; booking status → private broadcast → reload; BOOT_COMPLETED → future alarms. Three channel names and a READY notification capture.

**Speaker notes:** “Custom broadcasts stay inside the package, and booking screens register receivers only while started. Notifications use a booking-specific immutable PendingIntent: detail then Back to Home. Confirmed bookings get inexact reminders two hours before, or four hours for a Plus booking. BootReceiver restores eligible cached reminders after reboot. Android may delay background jobs and alarms.”

**Evidence:** `ConnectivityMonitor`, `BookingStatusReceiver`, `BootReceiver`, `NotificationHelper`, `ReminderScheduler`.

## Slide 9 — Admin workflow and Canvas chart

**Speaker:** Part C — [name]

**On slide:** exact transition chain with cancellation only from the first two stages; admin catalog editors; dashboard screenshot and chart.

**Speaker notes:** “The approved admin filters bookings and moves them through the allowed transitions. Expected-state checks reject stale updates. The admin can edit offerings and Featured placement. Dashboard calculations run in the background, and the seven bars are drawn directly with Canvas. Monthly completed totals use appointment dates in Dhaka, and commission is estimated, not paid out.”

**Handoff:** “Next, [Part D name] will explain the assistant and business behavior.”

## Slide 10 — AI: where, why and how

**Speaker:** Part D — [name]

**On slide:** symptom → fixed mechanical concepts + catalog aliases → Gemini JSON → local validation → service cards → workshop picker. Response fields: reply, urgency, serviceIds.

**Speaker notes:** “Ask the Mechanic helps a customer name a service. GeminiClient uses HttpURLConnection and one model-name constant. The prompt requests plain language, a short follow-up when vague, catalog-only recommendations and an inspection caveat. JSON schema guides the output, but GeminiProtocol still validates it. Card prices are catalog prices, never generated prices.”

**Evidence:** `GeminiClient`, `GeminiProtocol`, `ChatbotActivity`, `PickWorkshopActivity`.

## Slide 11 — AI failure handling and privacy tradeoff

**Speaker:** Part D — [name]

**On slide:** missing key / 429 / timeout / malformed reply → keyword fallback + offline label + Retry. “No raw personal text in the request.” “Suggestions, not diagnosis.”

**Speaker notes:** “We implemented these failure paths; we do not claim a live rate-limit incident was measured. Local keywords return existing IDs, and conservative English danger checks can raise urgency. To keep personal information out of the request, an allowlist sends mechanical concepts and temporary aliases instead of raw conversation or catalog titles. That loses some language detail. A production key belongs behind an authenticated proxy.”

**Evidence:** `FallbackAdvisor`, `ReplySafety`, `SymptomPrivacy`, request-contract tests.

## Slide 12 — Business model implemented in the app

**Speaker:** Part D — [name]

**On slide:** commission / featured visibility / demo Plus. Seeded Oil Change: ৳3,500 → standard commission ৳350; Plus price ৳3,325 → commission ৳332.50. Plus ৳199/month, no real payment.

**Speaker notes:** “Commission is calculated on the payable price. Featured workshops sort first and have a badge. Plus adds a five-percent discount, first-hour slot eligibility, a pickup request flag and early reminders. It never overrides an occupied slot or dispatches a car. No payment or featured-placement billing is implemented; this demonstrates the model's behavior.”

**Evidence:** `BusinessConfig`, `BookingPricing`, `WorkshopBrowser`, `PlusActivity`.

## Slide 13 — Verification, limitations and demonstration

**Speaker:** Part D — [name]; each additional member may own a named test/demo segment here.

**On slide:** recorded 41 passing JVM tests, 56 passing rule-emulator cases, successful debug/release builds and lint with zero errors. Checklist: all eight course feature areas. Add actual phone results only after rehearsal.

**Speaker notes:** “The repository records these automated results. Static checks and APK signature verification support API 24 compatibility, but no API 24 runtime pass is claimed. Rules need publication, admins need owner approval, and direct APK API keys remain extractable. Our live sequence will show the queue, service, broadcasts, notification, business behavior and assistant; device outcomes must be reported honestly.”

**Transition:** open [DEMO_SCRIPT.md](DEMO_SCRIPT.md). Finish with each member's actual contribution and questions.

## Before rendering the deck

Replace every member placeholder and verify that every real member speaks. Capture the actual hardened Admin access UI, not the earlier self-registration design. Use [REPORT.md](REPORT.md) for precise limits and code mapping. The counts above come from `build/prompt14-checks.log` and `build/prompt14-rules.log`; update them if the suite changes. A prepared clip must be labelled as a recorded rehearsal, not presented as a live successful test.
