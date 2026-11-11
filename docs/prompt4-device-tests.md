# Prompt 4 device checks

## Build and install

Run outside the runner sandbox:

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest
```

Install `app/build/outputs/apk/debug/app-debug.apk`. Sign in as a customer. Live Firebase access and seeding were not exercised by the runner.

## Add the demo catalogue

Long-press the **Hood Up** title at the top of Home in a debug build. Wait for the success message; Home then refreshes automatically. This writes six fictional Dhaka workshops and ten services atomically using fixed `demo_workshop_*` and `demo_service_*` IDs. Repeating the gesture updates those same documents instead of duplicating them; it does not modify other IDs.

If your Firestore rules allow catalogue writes only for admins, sign in as an admin and long-press **Hood Up** on the admin dashboard placeholder instead. Then sign out, log in as a customer, and tap Refresh. No rule changes are needed to bypass permissions. A failed seed shows an error; it does not claim success.

The demo workshops cover Gulshan, Banani, Uttara, Dhanmondi, Mirpur, and Mohammadpur. Names, ratings, prices, addresses, and descriptions are demonstration data. Phone values are deliberately non-working placeholders. The seeding implementation exists only in `src/debug`; the release binding installs no gesture and has no demo-writing implementation. The full admin seed helper remains for Prompt 11.

## Home and live filters

1. Check **Hey, first-name**, the profile icon, mechanic-bot card, search, four filter chips, workshop cards, and bottom navigation. On a small device or with the keyboard open, scroll the header to reach all controls.
2. With all filters cleared, confirm featured workshops appear before non-featured workshops. Within each group, higher ratings sort first.
3. Type `BANANI`, clear it, then type `engine oil`. Confirm results update without submitting the keyboard. Search matches workshop name, area, address, and names of its offered services.
4. Try each chip with and without a search. Featured must show only featured workshops; Top rated means rating at least **4.5**. Open now uses the current time in **Asia/Dhaka**, regardless of device timezone. Opening is inclusive, closing exclusive; overnight hours are supported.
5. Check row name, area, rating, featured badge, open/closed pill, and starting BDT price. Starting price is the minimum of that workshop's known offered services, not the whole catalogue. Unknown prices show “Price unavailable”. Scroll repeatedly to check recycled rows do not retain another row's badge or open state.
6. Search for nonsense. Confirm the no-matches message appears while search, filters, and Refresh remain reachable. Clear the query and restore All; rows must return.
7. Rotate with a search/filter applied. Confirm the query and selected filter survive. Background and return; opening status must update. It also updates every minute while Home is visible.

## Cache, loading, and refresh

1. Before seeding an empty test catalogue, launch Home and check the skeleton/loading state, followed by the empty message after the refresh finishes.
2. After one successful online load, disconnect and relaunch. Saved rows should appear from SQLite. A failed refresh must preserve the cached rows and show a retry message. Refresh becomes available again when the request finishes.
3. Reconnect. Change a demo workshop rating or service price in your Firebase test project, tap Refresh, and verify updated data appears. Reopen offline to confirm the refreshed values were cached.
4. Open a detail screen offline after the catalogue has been loaded. Its cached details and services should remain available even if refresh fails.

## Workshop detail and navigation

1. Open several workshop cards, including after filtering. Confirm each opens the correct name, address, rating, Dhaka hours, description, and only services listed in its `serviceIds`.
2. Check BDT prices and durations on every service row. Tap different **Book this service** buttons; the Booking placeholder must display the matching `workshopId` and `serviceId`. Back returns to the detail screen without creating a booking.
3. Tap Call: a dialer must open with the demo phone value, without placing a call or requesting call permission. Do not call the placeholder number.
4. Tap Directions: Google Maps should open with the encoded workshop address. Without Google Maps, another geo-capable map app may handle it. Without any handler, a friendly message should appear instead of a crash.
5. Open the mechanic-bot card, My Bookings, and Profile. Confirm each reaches its own Activity. Chatbot, booking checkout, booking list, and full profile editing remain stubs for later prompts. The profile icon and bottom Profile item should open the same Profile Activity.
6. Navigate between bottom items repeatedly and press Back. Confirm Home is reused rather than endlessly stacked. Sign out through Profile and confirm the entry flow returns to Login with the old stack cleared.
7. Check light/dark system modes, large text, keyboard visibility, system bars, and landscape layout. The app must remain dark and controls must stay reachable.

These are recommended device checks, not tests already performed by the runner.
