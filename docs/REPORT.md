# Hood Up — project report

**Course:** [course title/code]  
**Group:** [group identifier]  
**Members:** [names and student IDs]  
**Submission date:** [date]

This report describes the checked-in implementation. Member attribution is intentionally unfinished. Automated results are distinguished from device/cloud behavior that still needs demonstration; no user study, production deployment or revenue collection is claimed.

## 1. Overview / Introduction

### 1.1 Purpose and motivation

Hood Up is a Java Android car workshop booking app for customers and administrators. A customer can browse workshops, select a service and car, request an appointment and follow its status. Someone who cannot name the required service can describe symptoms to Ask the Mechanic and move from a suggestion to an eligible workshop and booking.

The main design problem is continuity: finding a service, choosing a time and tracking work should remain connected even if the network fails. The solution combines cached browsing, a local booking queue, server-side reservation checks and clear pending/sync/status labels. The demo catalog uses Dhaka areas and BDT. Its seeded workshop records illustrate the flow; they are not verified partnerships.

The package is `com.example.carworkshopbookingapp`, minimum SDK 24, compile/target SDK 37. Application code is Java; Kotlin DSL files configure Gradle only. Screens use plain Activities, Intents, basic layout containers, ListView and Spinner. A dark AppCompat theme and rounded XML shapes provide the visual design without Material, RecyclerView, CardView, Compose or Navigation.

### 1.2 Development challenges and responses

“Observed” below refers to build history or repository changes. “Handled” means the code addresses a platform/provider condition; it does not claim that we reproduced it on a phone.

| Challenge and evidence | Response in the real implementation | Verification boundary |
| --- | --- | --- |
| **Observed build failure:** Android JDK-image transformation/configuration-cache errors around `core-for-system-modules.jar`; the earlier language-server runtime lacked the complete tooling needed for the build | `gradle/gradle-daemon-jvm.properties` selects a complete JetBrains JDK 25; `gradle.properties` disables configuration caching; later debug and release builds completed | Recorded build success does not establish API 24 runtime behavior |
| **Observed architecture constraint:** the starter's Material/ConstraintLayout choices conflicted with the course's basic-layout requirement | AppCompat, Java Activities, XML shapes, ListView BaseAdapters/ViewHolders and a LinearLayout bottom bar replaced the starter approach | Current source/dependencies and successful builds provide evidence; no claim of cross-group uniqueness research |
| **Handled Android 12+ foreground-start restrictions** | `SyncCoordinator` permits foreground-service starts while activities are visible, catches restricted starts and schedules `BookingSyncJob` for background-safe work; the Service is foreground only during active uploads and releases itself after bounded work | Code implements the restriction handling; real-device timing remains to be rehearsed |
| **Handled broadcast/export restrictions** | `ConnectivityMonitor` uses NetworkCallback and emits package-restricted custom broadcasts; booking screens register non-exported receivers only while started; boot/alarm receivers have explicit ownership checks | Normal shell broadcasts cannot be assumed to reach private receivers; use the legitimate app flow or authorized test environment |
| **Observed design conflict during hardening:** querying all workshop bookings exposed data beyond customer ownership rules | Availability now reads minimal `workshops/{id}/slots` records; customer booking queries filter by current UID | Firestore emulator allow/deny tests passed; publishing the policy to the real project is separate |
| **Offline retries and simultaneous slot selection** | Stable booking IDs, hourly occupancy locks and `createAtomic` transactions protect against duplicate creation and overlapping reservations; cancellation markers prevent a delayed queued creation from resurrecting a cancelled request | Offline selection is provisional; a conflicting or changed quote may require cancellation/rebooking |
| **Handled Gemini failures:** missing key, 429, timeout, unavailable model, malformed/blocked output | Explicit error paths, bounded HTTP response parsing, catalog-ID validation, local `FallbackAdvisor`, visible offline label and customer-initiated Retry | No live quota exhaustion or model availability test is claimed; these are implemented failure paths |
| **Observed polish issues:** legacy Back overrides and incomplete dynamic editor lifecycle handling | AndroidX Back dispatcher, generation/destruction guards, detached operation listeners and saved admin editor drafts | Lint and unit checks passed; rotation/process recreation on a device remains a manual check |
| **Observed security conflict:** an APK invite cannot safely grant an admin role | Hardened registration cannot create an admin profile; a trusted project owner provisions the role, and Firestore rules reject self-promotion | The invite is an extractable local UX check, not an authorization secret |

## 2. Benefit / Usability

Customers see workshop area, rating, opening state and starting price before opening details. Search and All/Featured/Open now/Top rated chips narrow the list. Call and Directions hand work to the phone's dialer/maps handler rather than requesting unnecessary call/location access for those actions.

Cars can be reused across bookings. Date/slot selection checks hours, closed days where configured, duration and current occupancy. The summary and price breakdown precede confirmation. A saved offline request is labelled “Waiting to sync,” which separates local persistence from server acceptance. Upcoming/History, the status timeline, cancellation confirmation and five-star completion rating give a consistent follow-up path.

The assistant reduces the vocabulary needed to choose a service, while keeping an inspection caveat. Quick starts and local matching provide a usable path when Gemini cannot answer. This is service navigation, not a mechanical diagnosis or an emergency service.

Admins can maintain services, prices, keywords, workshop hours/offerings and Featured placement; filter bookings by status/workshop; view a customer phone dial action; and move work through permitted stages. They also get calculated statistics and a small chart without adding a chart library.

The interface includes scrollable forms/list headers, inline validation, loading/empty/error states, retry actions, dark colors, ripple/pressed feedback and booking-confirm haptics. These are implemented usability provisions, not measured improvements from a participant study. Profile contact-email editing does not change Firebase's sign-in email.

## 3. Implementation Details

### 3.1 Layers and execution

`ui/` contains Activities and list adapters. `data/` contains domain logic and coordinating stores; `data/local/` contains SQLite persistence; `data/remote/` wraps Firebase callbacks. `service/`, `receiver/`, `ai/` and `util/` separate background, platform and shared behavior. The [README architecture diagram](../README.md#architecture) shows the connections.

`AppExecutors` supplies a shared single-thread IO executor, a separate two-thread pool for blocking AI HTTP, and a main-thread Handler. `RemoteTasks` starts Firebase operations and maps completed results on IO before posting callbacks. LocalDb's DAOs use the executor and its database-opening methods explicitly reject the main Looper. Admin statistics also run on the executor. The separate HTTP pool prevents a slow model request from holding up ordered SQLite work.

UI requests use generation/lifecycle checks or retained controllers. Activity listeners detach in `onStop`; subscriptions close, receivers unregister and timers remove callbacks at the appropriate lifecycle boundaries. Rotation remains enabled. Saved view state and admin drafts preserve form input; SQLite preserves chat and queued bookings. A running request cannot survive process death as an in-memory callback, so recovery uses stable IDs, persisted state and explicit retries.

### 3.2 Local and remote storage

| SQLite table in `LocalDb` | Purpose |
| --- | --- |
| `workshops_cache`, `services_cache` | Immediate catalog reads while a server refresh runs |
| `cars` | Owner-scoped mirror of saved cars |
| `pending_bookings` | Offline queue, using stable booking IDs |
| `bookings_cache` | Owner-scoped server booking copy plus retained terminal local cancellations |
| `chat_history` | Conversation, suggestion, urgency and retry metadata, scoped to UID |
| `session` | Key/value session state and sync-status tracking |

Firestore holds `users`, per-user `cars`, `workshops`, `services` and `bookings`. Workshop `slots` subcollections hold minimal occupancy; `booking_cancellations` holds immutable markers for requests cancelled before remote creation. Firebase Auth provides sign-in identity. SessionStore supplies locally persisted profile/session information; splash restoration also checks the current Firebase user.

Catalog repositories deliver cached data first, refresh from the server and update SQLite before delivering fresh results. The callback can therefore fire more than once. BookingStore merges the queue with cached bookings, then applies authoritative server snapshots. User-owned caches remain on the device across logout but are scoped by UID; logout clears the active session and stops sync/alarms/notifications.

### 3.3 Booking integrity, queue and started Service

`BookingActivity` passes a stable request ID to `BookingSubmission`. It calculates commission off the main thread and either submits online or writes to `pending_bookings`. The success screen distinguishes queued requests from acknowledged submissions.

`BookingsRepository.createAtomic` rereads the workshop, service, account and car. It checks current price/entitlement, hours, duration and reservation locks. Services that occupy part of a second hour reserve that second hourly block too. A repeated request with the same ID returns the existing compatible booking rather than creating another document.

`BookingSyncService` is a **started**, unbound Service (`onBind` returns null). `SyncRunner` uploads queued requests, commits local acknowledgement and sends status/queue updates. It also observes a bounded booking snapshot pass and closes its listener/timers. A shared run lock limits concurrent sync passes. Foreground notification state exists only during active uploads; idle work does not keep a permanent foreground Service alive.

While the UI is visible, SyncCoordinator maintains a live booking listener. In the background, JobScheduler retries network work and performs best-effort periodic checks. This is not Firebase Cloud Messaging or guaranteed instant background push. Failed connectivity keeps the request local; permanent slot/quote rejection is not falsely marked synced and requires customer action.

Hardening replaced an earlier CANCELLED booking tombstone with a separate cancellation marker. The rules permit only PENDING booking creation. A delayed transaction checks that marker, while SQLite can retain the cancelled local history entry.

### 3.4 Status, broadcasts and notifications

AdminLogic and the server policy enforce:

```text
PENDING -> CONFIRMED -> IN_PROGRESS -> READY -> COMPLETED
   |           |
   v           v
CANCELLED   CANCELLED
```

Only PENDING or CONFIRMED can be cancelled. The exact permitted edge table is:

| Current | Allowed next |
| --- | --- |
| PENDING | CONFIRMED or CANCELLED |
| CONFIRMED | IN_PROGRESS or CANCELLED |
| IN_PROGRESS | READY |
| READY | COMPLETED |
| COMPLETED / CANCELLED | None; cancellation retries are idempotent, not a new forward transition |

NetworkCallback detects validated connectivity and emits `ACTION_NETWORK_RESTORED`; the coordinator selects a permitted sync path. `BOOKING_STATUS_CHANGED` is package-restricted. My Bookings and Booking Detail receive it while started and call `reload()`. These coexist with the booking listener and local cache updates.

NotificationHelper creates Booking updates, Reminders and Sync channels on supported Android versions. It asks once for Android 13+ notification permission, provides submitted/status/reminder messages and uses immutable PendingIntents with a Home → Booking Detail stack. A READY update reads “Your car is ready.”

ReminderScheduler uses inexact AlarmManager alarms: two hours before a CONFIRMED appointment, or four hours for a booking made with Plus. An already-past trigger is skipped. Cancellation or disabling reminders removes the alarm. ReminderReceiver rereads local data and validates the current owner/status; BootReceiver restores eligible future reminders after boot/package replacement. Android can delay inexact alarms and background jobs.

### 3.5 Admin tools, Canvas chart and rating transaction

AdminActivity verifies the current user's server profile role before enabling admin content. AdminRepository repeats the role check inside write transactions. Status updates compare the expected current state to prevent a stale admin screen overwriting another update. Workshop/service editors validate input, offer confirmation before deletion and preserve editor drafts.

AdminDashboardActivity calculates today's appointments, all pending bookings, completed appointments dated in the current month and their stored commission sum. `BookingsChart` draws seven appointment-day counts using Canvas. The dates use Asia/Dhaka; the chart includes cancelled appointments and zero-count days. “Completed this month” uses the appointment date, not an unimplemented completion timestamp, and commission is an estimate rather than settled cash.

For a completed booking, `BookingActionsRepository.rate` updates its 1–5 rating and workshop count/sum/average in one transaction. Editing an existing review adjusts the sum without adding another count. `ratingBookingId` and getAfter checks bind the server aggregate change to the owner's completed booking; an aggregate-only forgery is denied.

### 3.6 Security and limits

Root `firestore.rules` denies unknown paths, enforces owner-only cars and private customer bookings, permits signed-in catalog reads, and restricts privileged writes by the role stored in `users/{uid}`. It validates prices, entitlement, slot proof and bounded rating changes. Rules only take effect after publication; trusted console/Admin SDK access is governed by IAM and bypasses client rules.

Client registration always creates a customer. Even a correct APK invite only produces an owner-approval explanation. A trusted project owner must verify and provision an admin UID. Gemini/API invite values come from ignored root `.env` into BuildConfig, but the APK can expose them. No backup of session data is intended: the manifest disables backup and transfer exclusions remain in XML.

Plus is a demo entitlement, not payment verification. Service duration is capped at 300 minutes to fit the atomic rule proof within document-access limits. All seeded services fit this limit. [SECURITY_NOTES.md](SECURITY_NOTES.md) explains deployment, certificate/API restrictions, quota limits, migration and a production Cloud Function proxy alternative. The proxy is proposed, not implemented.

## 4. Business Model

Three revenue paths are represented in application behavior:

1. **Booking commission:** `BusinessConfig.COMMISSION_RATE` is 10%. BookingPricing uses decimal HALF_UP rounding; the booking stores the calculated commission. The customer sees the service/discounted total, not commission. Admin estimates sum stored commissions for completed appointments.
2. **Featured placement:** featured workshops sort first and carry a badge. Admins can change the Featured flag. This models paid visibility; no featured-placement price, invoicing or actual charge collection is implemented.
3. **Hood Up Plus:** the screen offers ৳199/month and explicitly states “Demo mode — no real payment.” Subscribe writes a bounded 30-day entitlement. Benefits are a 5% service-price discount, the first opening-hour slots if still available, a pickup request flag and a four-hour reminder rather than two hours. Plus never overrides an occupied slot or dispatches a pickup vehicle.

The seeded Engine Oil Change has a base price of ৳3,500 (`DemoSeeder`). Its example calculation is:

| Case | Customer service total | Stored 10% commission | Illustrative workshop share |
| --- | ---: | ---: | ---: |
| Standard | ৳3,500.00 | ৳350.00 | ৳3,150.00 |
| Active Plus: 5% off | ৳3,325.00 | ৳332.50 | ৳2,992.50 |

The ৳199 simulated subscription is separate; the workshop share is explanatory arithmetic, not a payout feature. Real pricing, settlement, payment receipts and subscription verification would need a backend/payment integration. See [BUSINESS_MODEL.md](BUSINESS_MODEL.md).

## 5. AI — Where / Why / How

**Where:** ChatbotActivity presents Ask the Mechanic; ChatSession manages history/retry/persistence; GeminiClient performs HTTPS; GeminiProtocol builds/validates the request/response. FallbackAdvisor and ReplySafety provide local matching and conservative presentation. A suggestion opens PickWorkshopActivity, which passes workshop/service IDs and the editable symptom note to BookingActivity.

**Why:** a symptom such as “Strange noise when braking” is easier for a customer to express than a service taxonomy. The assistant narrows the request to services already offered in the catalog and asks a follow-up when information is vague. It does not replace a workshop inspection.

**How:** the model name is one GeminiClient constant, currently `gemini-2.5-flash`. HttpURLConnection posts generateContent JSON with an API-key header, 15-second connect and 30-second read timeouts. Up to about eight recent conversation turns are represented. The system instruction calls for friendly plain language, roughly 70 words, no invented prices/certainty, one short follow-up when vague and HIGH urgency/stop-driving advice for dangerous symptoms.

The request specifies `application/json` and a schema for `reply`, `urgency` and at most three `serviceIds`. Those IDs are temporary `service_0`-style aliases enumerated in the schema. Parsing still rejects malformed/incomplete output, wrong field types, unknown IDs/urgency, oversized replies and common price/overconfidence claims. Aliases map back to actual cached service IDs locally; displayed prices come from the catalog.

To avoid sending personal details, SymptomPrivacy converts raw user/assistant turns and catalog titles/categories to a fixed allowlist of mechanical concepts. Arbitrary text becomes a fixed follow-up placeholder; original names, addresses, plates, account data and raw IDs are not copied into the request. This reduces language detail and recognition of unlisted/non-English symptoms. Full local text still supports keyword fallback and can become a booking note if the user chooses to submit it. Network transport necessarily exposes metadata such as the connecting IP to Google.

FallbackAdvisor matches normalized words/phrases against `ServiceItem.keywords`, handles a small set of variants and returns only existing IDs. Missing key, HTTP/429/model errors and unusable replies fall back locally with an “offline suggestion” label and Retry. The label means local advice, not necessarily that internet is absent. Local English danger checks can upgrade urgency but do not detect every hazard. Clear chat deletes local history; it does not revoke data already sent to a provider. More detail is in [AI_USAGE.md](AI_USAGE.md).

## 6. Requirement-to-code mapping

All Java paths below are relative to [the application package](../app/src/main/java/com/example/carworkshopbookingapp/).

| Required feature | Files/classes | Demonstration evidence |
| --- | --- | --- |
| Activities, Intents, basic layouts | `ui/SplashActivity.java`, `HomeActivity.java`, `WorkshopDetailActivity.java`, `BookingActivity.java`; `ui/adapter/WorkshopAdapter.java`; `res/layout/` under `app/src/main/` | Navigate Home → detail → booking; open dialer/maps without placing a call |
| Local storage | `data/local/LocalDb.java`, `SessionStore.java`, `ModelJson.java` | Cached rows/chat survive reopening; pending queue row exists offline |
| Remote storage | `data/remote/AuthRepository.java`, `UsersRepository.java`, `CarsRepository.java`, `BookingsRepository.java`, `WorkshopsRepository.java`, `ServicesRepository.java` | Auth identity and acknowledged booking/catalog documents in the demo project |
| Threads | `util/AppExecutors.java`, `data/remote/RemoteTasks.java`, `ai/GeminiClient.java` | Show executor/Handler code or a debugger thread; UI responsiveness alone is supporting, not conclusive, evidence |
| Service | `service/BookingSyncService.java`, `SyncRunner.java`, `BookingSyncJob.java`, `SyncCoordinator.java` | Upload an offline queue from visible Home; brief foreground notification/service lifecycle |
| Broadcasting | `receiver/ConnectivityMonitor.java`, `BookingStatusReceiver.java`, `BootReceiver.java`, `ReminderReceiver.java` | Network restore/status reload; genuine reboot reminder restoration |
| Notifications | `util/NotificationHelper.java`, `ReminderScheduler.java`; `data/ReminderPolicy.java` | Submitted/READY notification, deep link, scheduled reminder evidence |
| Business model | `data/BusinessConfig.java`, `BookingPricing.java`, `WorkshopBrowser.java`; `ui/PlusActivity.java`, `AdminDashboardActivity.java` | Featured ordering, Plus price change, stored/admin-visible commission |
| AI integration | `ai/GeminiClient.java`, `GeminiProtocol.java`, `SymptomPrivacy.java`, `FallbackAdvisor.java`, `ChatSession.java`; `ui/ChatbotActivity.java`, `PickWorkshopActivity.java` | Symptom → catalog suggestion → eligible workshop → prefilled booking note; labelled local fallback |

## 7. Testing, release evidence and remaining work

The prior verification record in `build/prompt14-checks.log` shows `test`, `assembleDebug`, `assembleRelease` and `lint` completed successfully. The configured JVM suite contains **41 tests**, with zero failures/errors; lint had zero errors and 90 non-blocking warnings. Tests cover rounding, all status pairs, keyword ranking, malformed/provider JSON, privacy/alias mapping, slots and other pure logic. `build/prompt14-rules.log` records **56 passing Firestore emulator cases**. These are local automated results, not real-user or real-provider trials.

The debug APK and a release variant signed with the local debug certificate were produced. APK signature/alignment checks and merged manifest inspection support static API 24 compatibility. [RELEASE.md](RELEASE.md) gives commands, artifact locations and exact verification boundaries. No owner-controlled production signing key was invented or used.

At preparation of these documents there was no connected phone and no API 24 runtime result. Rule publication, real-project API restrictions/quotas, Firebase/Gemini behavior, reminder timing, reboot and release UI still need the [manual QA checklist](MANUAL_QA.md). Add actual device/date/results before submitting an experimental evaluation; do not turn intended behavior into a claimed observation. Recording/editing the video, rendering slides/wireframes, filling member details and packaging the project remain group work.

## 8. Contribution of each member

Fill these rows from the group's actual work; the areas are an attribution worksheet, not inferred authorship. Repeat a person's name for multiple areas or add rows so **every actual member** is represented. Replace placeholders before submission.

| Member name | Student ID | Actual responsibility / contribution | Evidence to add | Slides presented |
| --- | --- | --- | --- | --- |
| [name] | [ID] | [customer screens, navigation or design work actually completed] | [files/commits/review record] | [slide numbers] |
| [name] | [ID] | [SQLite/Firebase/booking work actually completed] | [files/commits/review record] | [slide numbers] |
| [name] | [ID] | [Service/broadcast/notification work actually completed] | [files/commits/review record] | [slide numbers] |
| [name] | [ID] | [AI/admin/business-model work actually completed] | [files/commits/review record] | [slide numbers] |
| [name or repeat] | [ID] | [testing, integration, documentation, video or presentation work actually completed] | [test/rehearsal/artifact evidence] | [slide numbers] |

## 9. Conclusion

Hood Up connects a customer booking flow to local persistence, transactional remote state, Android background/platform components and catalog-based AI guidance. Its main implementation choices make failures visible: queued is different from submitted, AI failure has a labelled fallback, and APK secrets do not grant admin authority. The remaining release checks concern actual devices and external deployment, not additional claims of completed cloud or field testing.
