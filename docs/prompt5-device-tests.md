# Prompt 5 device checks

## Build and install

Run outside the runner sandbox:

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest
```

Install `app/build/outputs/apk/debug/app-debug.apk`. Use the demo catalogue from Prompt 4 and a customer account. Firebase console and device actions have not been performed by the runner.

## Cars

1. Open Profile → Manage cars. An account with no cars should show an empty state and Add a car.
2. Add a Toyota Corolla with a valid year and registration plate. Empty make/model, a short plate, and a year outside 1900–next year must show inline errors. Submit twice quickly; only one car should be created.
3. Verify the record in `users/{uid}/cars/{carId}` and the same ID in the SQLite `cars` table using Android Studio Database Inspector. Edits must update that ID rather than creating a second car.
4. Add another car, edit each, then delete one using the confirmation dialog. Confirm deletion from both remote and local storage. Cancel a deletion and verify nothing changes.
5. Relaunch offline after loading cars: cached cars should remain listed. Saving/deleting cars requires a successful online transaction; failure should show an error and retain the form/data.
6. Sign in as another customer. Neither account should see the other's cached or remote cars. Rotate during save/delete and verify a single mutation and a usable result screen.

## Booking form

1. Choose a workshop and Book this service. Confirm the summary shows the correct workshop, service, BDT price and duration. The price breakdown must show the service price/total and Hood Up protection, with no commission shown to the customer.
2. With no cars, confirm booking stays disabled and Add a car opens the form. Save a car and return; the Spinner should refresh and allow selection. With multiple cars, select each and check that selection survives rotation.
3. Choose today or a future date. Past dates must be blocked. To test a closed day, add `closedDays` to a test workshop document as integer Calendar constants: Sunday=1, Monday=2, …, Saturday=7. Refresh the booking screen. Choosing a closed opening day must be rejected with an inline message.
4. Slots advance hourly in Dhaka time. A service must finish before closing. For a 90-minute service at a workshop closing at 20:00, 18:00 may be offered but 19:00 must not. Past start times must not be selectable.
5. For an overnight workshop (for example openHour=22, closeHour=2), check that next-day times are labelled with their weekday. `closedDays` applies to the opening day of that overnight session.
6. Add a note and confirm. Confirm controls disable and progress appears. Success must show the selected car, workshop, service, Dhaka date/time, total, booking reference and a My Bookings button. The booking list remains the Prompt 4 stub until Prompt 6.
7. Inspect the Firestore booking: status PENDING, correct IDs/car label/note/duration, `synced=true`, and commission equal to 10% of price rounded to two decimals. The customer screen must not show that commission.
8. Rotate or background during submission, then return. Confirm one result and one booking ID. Pressing Back from success must not expose a still-submittable copy of the same form.

## Availability and concurrent reservation

1. Open the same workshop/date on two devices or customer sessions. Confirm the same available start time nearly simultaneously. Exactly one transaction should succeed. The other should show “That time is no longer available”, refresh availability, and grey out the unavailable start time.
2. Repeat with a 90-minute service at 09:00 and another request at 10:00. These overlap and must not both succeed. A request at 11:00 is adjacent and may succeed if the workshop is open.
3. Reopen the screen after a booking. Occupied and overlapping slots must be disabled and greyed out. The availability query reads workshop bookings, and local pending requests also block overlaps on this device.
4. Change a service price/duration in the test project after the form has loaded, then confirm without refreshing. It must reject the stale quote and request a refresh rather than silently booking a different price.
5. Delete the selected remote car or remove the offered service before confirmation. The transaction must reject the invalid selection.

Reservations use `workshops/{workshopId}/slots/{hourStartEpochMillis}` documents containing `bookingId` and `userId`, written atomically with `bookings/{bookingId}`. A service reserves all hourly blocks it overlaps. Existing rules must allow the intended customer transaction and availability reads; a permission error is not treated as an offline booking. Do not bypass a permission failure by opening public database access.

All booking creation, including the later sync service, must call `BookingsRepository.createAtomic` (the repository's `save` delegates to it). Stable request IDs make retries idempotent. Cancellation through `BookingsRepository.updateStatus` releases only slot locks still owned by that booking in the same transaction; direct console edits to booking status do not release locks automatically.

## Offline queue

1. Load catalogue and cars online, open a booking form, then enable airplane mode and tap Refresh. The form should explain that live availability is unavailable and the request will be checked when it syncs.
2. Select a future time and confirm. Success must say exactly “Saved — we'll send it as soon as you're back online” and identify this as a queued request rather than a confirmed appointment.
3. Inspect SQLite `pending_bookings`: same stable booking ID, status PENDING, `synced=false`, duration, car ID, note, price and commission. It must exist before the success screen is shown. Repeated taps must not duplicate the record.
4. Reopen the same workshop/date offline. The pending request's overlapping slots should be unavailable. Data should remain after restarting the app.
5. Reconnecting alone does not upload the queue in this build: the sync Service is a later prompt. That Service must use the same atomic reservation API and handle a slot taken before synchronization; an offline request cannot guarantee availability on another device.

Also verify the optional chatbot note via Intent extra `note` (or `prefilledNote`), long notes capped at 1000 characters, landscape/large-text scrolling, keyboard reachability, and dark appearance in both system theme modes.
