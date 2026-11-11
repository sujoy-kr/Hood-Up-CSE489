# Prompt 8: Service and offline synchronization

## Implementation and limits

`BookingSyncService` is a private **started**, unbound Service (`onBind` returns null). Home starts it from `onStart`; the shared Activity lifecycle also permits retries after returning from other screens. Every run verifies the Firebase user, reads only that UID's pending queue on the background executor, and sends stable booking IDs through `BookingsRepository.createAtomic`. The existing transaction rechecks slot locks, workshop/service/car validity and the quote. Successful acknowledgement marks the cached copy synced and deletes the queue record atomically in SQLite.

A shared in-process guard prevents concurrent Service/job queue drains. If a process dies or a callback times out after a server commit, a subsequent attempt uses the same document ID and returns the existing booking instead of creating another. Queue entries are never removed merely because a request was attempted. Rejected slots/expired appointments/changed quotes remain queued; the customer can cancel the queued request online and rebook with a valid selection. This build does not silently change the appointment or charge a new price.

The Service promotes to `dataSync` foreground mode only for active queue upload. Its real low-importance ongoing notification is removed when upload work ends. The manifest declares `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` and `POST_NOTIFICATIONS`. Runtime notification-permission UI and user-visible status notifications remain for Prompt 9. Denying notification permission does not waive the foreground-service notification requirement; Android may show the active service through system task management rather than the notification drawer.

The Service attaches a bounded Firestore listener, persists authoritative snapshots and detects status changes against a separate SQLite session-key ledger. Initial data establishes a baseline; unchanged statuses do not repeatedly emit events. It calls `NotificationHelper.bookingStatusChanged` (an intentional no-op stub for Prompt 9) and sends the package-restricted `com.example.carworkshopbookingapp.BOOKING_STATUS_CHANGED` broadcast, with bookingId/userId/status extras. No status notification should be expected yet. Existing UI listeners continue to update the screens independently until Prompt 9 connects broadcast receivers.

A Service cannot both stop itself when idle and keep its own listener alive forever. After its short observation window it detaches and stops, with a two-minute overall watchdog. While Activities are visible, a lifecycle-scoped coordinator listener continues to capture changes without keeping a foreground service alive. In the background, a network-constrained periodic JobScheduler job checks for updates; this is best-effort periodic detection, not instant push delivery. Closing all Activities detaches the visible listener. Logout detaches listeners, stops the Service, unregisters the network callback and cancels jobs; unsent records remain scoped to their owner for their next sign-in.

Connectivity returning while the app is visible restarts the Service. Connectivity returning in the background schedules/awakens a network-constrained `BookingSyncJob`, which runs the same finite sync engine directly rather than attempting an illegal background foreground-service launch. Pending queue creation schedules this fallback even if the app process later dies. Job execution can be delayed by Android scheduling, Doze and quotas; reopening Home provides the visible-Activity path. This is deliberate compliance with [Android's background FGS-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start). The Service also implements `onTimeout(int,int)` and stops on the platform deadline; see [data-sync service timeouts](https://developer.android.com/develop/background-work/services/fgs/timeout).

## Automated checks

Build and test commands:

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest --no-daemon --no-configuration-cache
```

The runner checks the build logs and unit test reports. Android runtime, Firebase transactions, connectivity and foreground-service behavior still require a device or emulator signed into a test account. Do not treat compilation as proof of live synchronization.

## Main airplane-mode test

1. Install `app/build/outputs/apk/debug/app-debug.apk`. Sign in as a customer and load the service/workshop catalogue and at least one car online.
2. Enable airplane mode. Book a future appointment using cached data. Success should say it was saved offline. Open My Bookings and confirm one row labelled **Waiting to sync**.
3. Use Database Inspector to verify a `pending_bookings` row exists with `synced=false` in its payload. Note its booking ID.
4. Keep the app visible and restore connectivity. Confirm the sync notification appears while queued requests are uploading, then disappears. It may be brief for a fast upload.
5. In Firestore, verify `bookings/{same-booking-ID}` exists with the correct user/workshop/service/car/note, status PENDING and synced=true. Verify reservation slot locks were written. In SQLite, verify the cached copy is synced and the pending queue row has been removed. The Waiting to sync label should disappear after the normal screen refresh/listener event.
6. Toggle airplane mode repeatedly, reopen Home, and restart the app. There must still be only one Firestore booking document with that ID.

## Background, failures and isolation

- Queue another booking offline, background the app, then reconnect. Inspect JobScheduler and Firebase for eventual upload. Do not expect an immediate job under Doze/quota restrictions. Reopening Home must also trigger a retry. No ForegroundServiceStartNotAllowedException should crash the app on API 31+.
- Test API 24/25, API 26+, API 31+ and a current API 35+ device when available. On API 33+, test with notification permission denied and granted. Service startup should not crash merely because drawer notifications are denied.
- Drop connectivity during a write. The queue row must survive unless the authoritative server acknowledgement has been cached. Reconnect and verify retries do not create duplicates, including an uncertain first commit.
- Reserve the same slot from another device before reconnecting the queued device. The losing queued request must remain unsynced; it must not silently move to another time or disappear. Cancel it online and rebook. Repeat with a changed service price/duration and an expired appointment.
- Observe a PENDING booking, change it to CONFIRMED in a test project, and verify the local cache and one status broadcast update. Repeat the same snapshot/status; it must not repeatedly broadcast. Status notifications are stubbed until Prompt 9.
- Send another status change while the app is backgrounded. Allow for periodic JobScheduler latency. Confirm a subsequent pass updates the cached copy and records the new status baseline.
- Sign out during a pending request, then sign in as a different customer. The old user's listener/jobs must stop, and the new user must never see or upload the old user's queue. An already committed old-user transaction may still exist under its original ID; returning to that user should acknowledge it idempotently.
- Verify an idle, empty-queue run stops and removes the ongoing notification, without a permanently running foreground service. The periodic job is scheduled work, not an always-running Service.
- Background/foreground repeatedly and rotate. Check listener cleanup and ensure foreground notifications and queue writes are not duplicated.

Diagnostic commands (replace UID/job parameters as appropriate): `adb shell dumpsys activity services com.example.carworkshopbookingapp` and `adb shell dumpsys jobscheduler`. No secrets, Firebase tokens or `.env` contents belong in logs or reports.
