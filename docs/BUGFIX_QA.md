# App audit and regression checks

## Changes

- Keep a restored booking slot until account, catalogue and occupancy data are available to validate it.
- Isolate offline queue overlap checks by account while retaining the global booking ID collision guard.
- Make cancellation of an unsubmitted booking retry-safe by reading an existing cancellation marker before writing.
- Require customer and admin cancellation transactions to release every reserved slot in Firestore rules.
- Preserve explicit symptom negation in the fixed mechanical concepts sent to Gemini, without sending original user text.
- Retry a failed Clear chat operation; sending a new message abandons that failed clear operation.

## Automated verification

Run from the project root with Android Studio's complete JDK:

```sh
./gradlew -Dorg.gradle.java.installations.paths=/opt/android-studio/jbr assembleDebug testDebugUnitTest lintDebug
```

Run `npm test` from `security-tests` using the installed Node runtime and Java 21. The Firestore suite uses only `demo-hood-up`, not the live project.

The audit ran 43 JVM tests and 60 Firestore emulator checks successfully. Lint reported zero errors and 90 warnings, including hardcoded UI strings, overdraw and dependency update suggestions. These warnings remain.

## Device checks still required

No Android device or emulator was connected during this audit. Install the latest `app/build/outputs/apk/debug/app-debug.apk` on a connected device with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

1. Sign in as a customer, choose a workshop/service and future slot, rotate the device, and wait for loading. The same available slot must remain selected. Reserve that slot from another session and refresh; the selection must then clear.
2. Cache two customer accounts and their cars. As account A, queue a booking offline. Switch to account B, go offline and queue the same workshop/time. A's local queue must not block B; once connected, the remote transaction must still prevent double booking. Confirm each account sees only its own queue.
3. Cancel a queued booking while online. Repeating cancellation after a lost acknowledgement or local persistence failure must succeed without permission denied. Verify it cannot later sync as a new reservation. This failure path requires a debug session or controlled storage/network fault.
4. Create and cancel reservations as customer and admin, including a 300-minute service. After cancellation, every occupied hour must become bookable again. Publish the updated `firestore.rules` to the intended Firebase project before checking the live backend; emulator success does not deploy rules.
5. In chat, compare “smoke” with “no smoke, no fuel smell, not overheating” and check the response respects explicit absence. Confirm “engine not starting” still describes a starting problem. Live responses require the configured Gemini connection.
6. Send a chat message, clear chat, and verify persistence after leaving/reopening the screen. With a controlled local database write failure, Clear chat should show an error and Retry should repeat the clear. Sending a new message after a failed clear must not cause a later answer retry to erase history.
7. Run the broader customer/admin, reminders, notifications, offline sync and API 24 checks in `MANUAL_QA.md` before claiming device validation.

The Firebase rules have only been updated locally. No live rules deployment or live Firebase/Gemini test was performed.
