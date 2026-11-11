# Prompt 12 audit

## Changes and verification scope

Modern Back gestures use the lifecycle-aware AndroidX dispatcher, including busy-operation guards and Home exit behavior. Booking notifications build Home → Booking Detail; the synthetic My Bookings step was removed. Logout cleanup now lives in AuthRepository for both customer and admin paths, cancelling known cached alarms before clearing the session.

Customer forms retain their request controllers and Android view state. Admin callbacks reject destroyed/finishing activities and obsolete screen generations; read callbacks have a 30-second UI deadline, and their Handler callbacks are removed when content is replaced or the activity is destroyed. Admin catalog drafts save text, document ID, offered-service IDs and featured state. ListView headers now carry Cars/admin controls instead of fixed-height lists nested in ScrollViews. Rating aggregates and closed days survive workshop edits, including edits restored after process death.

Inputs are trimmed at submission except passwords, whose spaces are intentional credentials. Field lengths are bounded; phone/name rules are shared. Permission, session, connection and timeout errors use readable messages. List refresh failures preserve cached rows. Booking snapshot loading has a 30-second UI deadline and continues listening for subsequent recovery until the subscription closes.

Rounded shape resources remain the visual foundation. Buttons/chips and rows have ripple/pressed feedback; row adapters fade each stable item once per adapter lifetime; booking confirmation uses platform haptic feedback. Touch targets, visible labels, text sizes, edge/keyboard insets and dark system-bar appearance were checked in source/resources. Phone appearance and feel still need the manual QA table.

## Threading and lifecycle inspection

| Path | Finding |
| --- | --- |
| Firestore repositories | All request factories and result mapping go through RemoteTasks on AppExecutors.io(). Firestore transactions run asynchronously; no Tasks.await(), blocking Task.getResult() wait, or UI-thread transaction work was added. getResult() is read only inside completed-task callbacks on the IO executor. |
| Live booking reads | BookingWatch attaches/removes its snapshot listener on the IO executor, maps documents there and posts results to main. BookingFeedActivity closes subscriptions in onStop; SyncCoordinator closes the visible listener when no activities remain and on logout. |
| SQLite | LocalDb DAO calls use AppExecutors.execute. Database opening explicitly rejects the main Looper; cursors use try-with-resources and transactions have finally/endTransaction. Session, chat, cache and queue callers use these DAOs. |
| Gemini HTTP | HttpURLConnection is confined to AppExecutors.network(), has 15s connect/30s read limits and disconnects in finally. This executor is separate from ordered SQLite work. |
| Dashboard | Aggregation runs through AppExecutors.execute; Canvas drawing only uses computed counts. |
| UI callbacks | Catalogue generations and destruction guards protect customer reads; retained operation listeners detach in onStop; admin ScreenCallback adds generation/destruction guards. No new static Activity field was introduced. |
| Timers/receivers | Home/Booking timers remove callbacks in onPause. Splash removes callbacks on stop/destroy. Status/offline receivers unregister in onStop. Admin read deadlines are removed on reset/destroy; booking subscriptions cancel their deadline on close. |
| Long-lived helpers | SyncCoordinator, ConnectivityMonitor and ChatSession hold application context. The chat UI observer detaches onStop. Service/Job destruction closes SyncRunner, whose bounded work cleans up its listener/timers. |

The audit is static inspection plus compilation, JVM tests and lint; no connected device was available for runtime profiling, gesture testing, Firebase access or memory measurements. Read deadlines finish UI waiting; they do not cancel or claim to cancel a server transaction. Write operations retain SDK transaction behavior and stable IDs, and an interrupted request can still commit remotely.

Lint is run without a baseline or disabled error checks. Remaining non-error warnings concern English-only strings/localization, intentional dark backgrounds/card wrappers, native Switch styling, and dependency/catalog suggestions; broad dependency upgrades and Material migration are outside this polish pass. The generated report is at `app/build/reports/lint-results-debug.html`. No TODO/FIXME markers or lint-reported unused resources should remain in the final run.

The full pre-demo table is in [MANUAL_QA.md](MANUAL_QA.md). Earlier per-prompt device guides remain useful for detailed flows, particularly broadcasts and boot alarms.

## Final automated results

Executed with Android Studio's JDK:

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest lint --no-daemon
```

The final invocation passed: 30 JVM tests, zero test failures/errors, both APKs compiled, and lint completed with zero errors and 88 non-blocking warnings. Lint reports zero unused resources. Source scans found no TODO/FIXME or legacy onBackPressed overrides in `app/src/main`. The full command output is saved in `build/prompt12-checks.log`.

`/home/hellfire/Android/Sdk/platform-tools/adb devices` reported no connected devices. Android instrumentation tests were compiled, not executed; live device/Firebase/Gemini and notification/alarm checks remain in the manual checklist.
