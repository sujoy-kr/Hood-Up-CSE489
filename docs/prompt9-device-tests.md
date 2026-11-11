# Prompt 9: broadcasts and notifications

## Build and runtime prerequisites

Run `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest --no-daemon --no-configuration-cache`. Install the debug APK over the previous build. Runtime tests require a signed-in test account, Firebase access and a device/emulator. Compilation does not establish notification delivery or alarm timing.

Channels on Android 8+: Booking updates (default), Reminders (high), Sync (min). Existing channel importance remains subject to Android/user settings. Notification taps use immutable/update-current PendingIntents with unique per-user/per-booking data and a Home → Booking Detail back stack. Updates replace that booking's previous update notification; reminders use a separate notification tag.

## Permission and notification types

1. On a clean install/data reset on API 33+, sign in and open Home. Confirm a short explanation appears before the system permission dialog. Continue and allow; then test booking submission, Confirmed, In progress, Ready, Completed and Cancelled. Ready must read “Your car is ready”. Tap each notification and verify the correct booking detail and sensible Back navigation.
2. Repeat on a disposable install choosing Not now, or denying the system dialog. Relaunch Home and sign out/in: the app must not nag. Enable notifications later through Android app settings to test delivery. Do not clear data on your normal project account just to reset this test.
3. Submit a booking online and sync a queued booking offline→online. Both should produce Booking submitted after server acknowledgement; merely saving an offline request must not claim it was submitted remotely.
4. Test two bookings: notification/alarm PendingIntents must not overwrite each other's IDs. Sign out before an alarm fires; the receiver must not post another user's cached booking. Logout removes existing notifications; stale alarm intents validate current Firebase ownership before posting.
5. Sync channel notifications remain ongoing only during active foreground uploads. Android may hide drawer notifications when permission is denied; the Service still follows the platform foreground-notification contract.

## Connectivity and offline banners

Home and My Bookings have a thin offline banner. Toggle airplane mode while each is visible: it should appear, then disappear when validated internet returns. ConnectivityMonitor uses NetworkCallback rather than a manifest CONNECTIVITY_ACTION receiver. Restored connectivity emits a package-restricted ACTION_NETWORK_RESTORED; its private receiver requests the started sync Service when visible, otherwise the background-safe network job from Prompt 8. Network-change broadcasts also refresh banner visibility.

Queue a booking offline, restore internet and verify upload. Background retries remain subject to JobScheduler/Doze latency. No foreground service is started illegally from a background connectivity callback.

## Status broadcasts

Both My Bookings and Booking Detail dynamically register non-exported receivers in onStart and unregister in onStop. A package-only BOOKING_STATUS_CHANGED event calls reload(). Change a booking status through your test backend and verify both views refresh. Existing live listeners remain as well. Repeatedly rotate/background/return and check there are no leaked receiver registrations.

## Inexact reminders and reboot

1. Confirm a future booking at least two hours away. Inspect `adb shell dumpsys alarm` for this package and verify its reminder trigger is appointment time minus two hours. Alarms use inexact `setAndAllowWhileIdle`, not exact-alarm permissions; actual delivery can be delayed by Android.
2. If the computed reminder is already past or exactly now, it must be skipped. Cancel the booking and verify the old PendingIntent alarm is cancelled. Changing the appointment/status replaces or removes that same booking's alarm.
3. When an alarm fires, it rereads SQLite and checks current Firebase UID, matching booking ID, CONFIRMED status and future appointment time before notifying. The notification must use the high-importance Reminders channel.
4. Reboot after caching a confirmed future appointment. BOOT_COMPLETED reloads cached bookings on the executor and restores eligible reminders. MY_PACKAGE_REPLACED does the same after an app upgrade. No foreground service is launched by boot.
5. With multiple accounts, only the currently signed-in account's future cached reminders are restored. A real reboot followed by unlocking is the most representative test; force-stopped apps do not receive normal background delivery until reopened.

## adb commands

Launch/sign in/open Home first. Production receivers and dynamic status receivers intentionally are NOT exported; an ordinary shell UID cannot be assumed to send to them. Do not export receivers just to make an adb test pass.

For a debuggable app, try sending as its own UID (user 0 shown):

```sh
adb shell run-as com.example.carworkshopbookingapp /system/bin/am broadcast --user 0 -a com.example.carworkshopbookingapp.ACTION_NETWORK_RESTORED -p com.example.carworkshopbookingapp
adb shell run-as com.example.carworkshopbookingapp /system/bin/am broadcast --user 0 -a com.example.carworkshopbookingapp.BOOKING_STATUS_CHANGED -p com.example.carworkshopbookingapp
```

If the device restricts `am`/shell-command delegation under run-as, use a root-capable test emulator instead:

```sh
adb root
adb shell am broadcast --user 0 -a com.example.carworkshopbookingapp.ACTION_NETWORK_RESTORED -p com.example.carworkshopbookingapp
adb shell am broadcast --user 0 -a com.example.carworkshopbookingapp.BOOKING_STATUS_CHANGED -p com.example.carworkshopbookingapp
adb shell am broadcast --user 0 -a android.intent.action.BOOT_COMPLETED -n com.example.carworkshopbookingapp/.receiver.BootReceiver
```

BOOT_COMPLETED is a protected system broadcast. On an unrooted physical device, use an actual reboot rather than expecting an ordinary adb shell broadcast to be authorized:

```sh
adb reboot
```

For an immediate reminder receiver test on a root emulator, substitute the signed-in UID and a cached CONFIRMED future booking ID:

```sh
adb shell am broadcast --user 0 -n com.example.carworkshopbookingapp/.receiver.ReminderReceiver -a com.example.carworkshopbookingapp.APPOINTMENT_REMINDER --es uid TEST_UID --es bookingId TEST_BOOKING_ID
```

This tests receiver validation/delivery only, not the two-hour alarm calculation. Use `adb shell dumpsys alarm`, `adb shell dumpsys notification`, and `adb shell dumpsys activity broadcasts` for inspection. Do not include secrets or .env contents in diagnostic logs.
