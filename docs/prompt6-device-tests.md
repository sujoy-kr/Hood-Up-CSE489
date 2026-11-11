# Prompt 6 device checks

## Required setup and build

The new server query uses `bookings`, `userId == current UID`, ordered by `dateTimeMillis` ascending. Create or verify this **collection-scope composite index** in the same Firebase project as the app:

- Collection: `bookings`
- `userId`: Ascending
- `dateTimeMillis`: Ascending

Wait until the index is enabled. `firestore.indexes.json` contains the definition. With Firebase CLI authenticated and the correct project selected, `firebase deploy --only firestore:indexes --project YOUR_PROJECT_ID` can deploy the prepared index configuration. Index deployment was not performed by the runner.

Run outside the runner sandbox:

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest
```

Install `app/build/outputs/apk/debug/app-debug.apk` **over the previous build without clearing app data** to exercise the database upgrade. Human Firebase and device checks below remain unperformed.

## SQLite upgrade and list

1. Before upgrading, keep a saved car, catalogue cache, session, chat record if present, and queued booking. After upgrading, confirm those records still exist. Database version 2 adds only `bookings_cache` and its user index.
2. Sign in as a customer with multiple bookings and open My Bookings. Rows must show service, workshop, car, Dhaka date/time, BDT price and the correct coloured status pill.
3. Upcoming contains PENDING, CONFIRMED, IN_PROGRESS and READY bookings, ordered earliest first. An overdue appointment stays Upcoming until it reaches a terminal status. History contains COMPLETED/CANCELLED, newest first.
4. Rotate with History selected; the selected chip must persist. Repeatedly scroll to exercise row recycling and check status colours and Waiting to sync labels do not leak between rows.
5. Create an offline booking through the previous prompt's flow. It must appear with “Waiting to sync”, and opening it must show the same label and details. There must be one row per booking ID even if a queued request has also reached the server.
6. Load the list online, close it, disable connectivity, and reopen it. The SQLite copy and unsent queue must remain visible. Reconnect while the list stays open; authoritative server snapshots should refresh the cache and UI.
7. Sign out and use another customer account. Neither account should see another user's cached bookings. Refresh errors must preserve saved rows and allow retry.

## Detail, live status and timeline

1. Open each status. Confirm full booking details, note, duration, created date, reference, and price; no customer screen should show commission.
2. Check the vertical Booked → Confirmed → In progress → Ready → Completed timeline. The current step must have the teal border/highlight and “current step” label. Cancelled bookings show a cancellation notice with no active progress step.
3. In a controlled test project, change a booking status while the list is visible and while its detail screen is visible. It must update without manually refreshing. Completion/cancellation should move the booking between the tabs. Close/reopen or background/return to verify the listener is detached and reattached correctly.
4. Tap Call workshop and verify ACTION_DIAL opens with the workshop's phone, without placing a call or requesting call permission. Demo phone values remain non-working placeholders.
5. If the workshop itself has been removed, saved booking details must still be readable; Call may be unavailable.

Both screens inherit a public `reload()` method for the future Prompt 9 broadcast receiver. Live updates currently use a lifecycle-bound Firestore listener. No receiver or notification feature was added in this prompt.

## Cancellation

1. PENDING and CONFIRMED bookings must show Cancel booking. Other statuses must not show it.
2. Open the confirmation and choose Cancel/dismiss: the booking must remain unchanged. Confirm the destructive action: the status must become CANCELLED, the booking must move to History, and only slot locks still owned by that booking must be released atomically.
3. Race a customer cancellation against a workshop status change to IN_PROGRESS. The transaction must reject the cancellation if progression wins; it must never cancel an already in-progress booking based only on stale UI data.
4. Cancellation requires connectivity, including cancellation of a queued request. Offline attempts must show an error and leave the request/data intact.
5. Cancel a queued booking online. If the booking has not reached the server, the transaction creates a CANCELLED tombstone under the same ID. It then removes the local queue record after acknowledgement and caches the cancelled record. A delayed create retry must not create a new live reservation.
6. Rotate during cancellation and double-tap the confirmation button. Verify a single effective cancellation and a usable UI afterward.

## Rating

1. A rating row with five selectable stars and Save rating must appear only for COMPLETED bookings.
2. Choose a rating, save it, and reopen the detail screen. Verify the selected stars persist remotely and offline after the cache is populated.
3. Inspect Firestore: `bookings/{id}.rating` and the workshop's `ratingCount`, `ratingSum` and `rating` must update together in one transaction.
4. Submit the same rating again or retry after a connection interruption. The count must not grow for the same booking. Change an existing rating: update the sum/average without incrementing the count.
5. Rate two completed bookings concurrently from different customers. The final sum/count/average must include both ratings correctly. Non-owners and non-completed bookings must be rejected by the repository transaction as well as the UI.
6. Ratings require connectivity; offline failure must retain the user's selection so it can be retried. Rotate before saving and during saving to verify selection and request state survive.

Demo workshop averages initially have no real review count; the first real review starts the actual aggregate. Repeating the debug seed preserves nonzero real rating aggregates. The live Firestore rules must authorize the intended owner cancellation/rating transaction and workshop aggregate update; the runner has not changed or deployed security rules.

The automatic queue-upload Service remains for its later prompt. This screen includes queued requests now, but reconnecting does not itself upload them yet.
