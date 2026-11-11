# Workshop and booking update

## Current behavior

- Removed the Plus Activity, subscription mutation, profile entry, pickup control, discount and early/priority benefits. Standard two-hour reminders apply to all bookings.
- New reservations charge the complete listed service price. All available hours, including the first opening hour, are open to every customer. Existing remote booking amounts remain unchanged. Old queued quotes with retired benefits must be cancelled and rebooked.
- Home has All, Open now and Top rated filters. Featured badges and featured-first sorting are removed. Stored featured flags and the existing admin editor remain for compatibility; Home does not use them.
- Workshop hours, slot labels, booking details, notifications and confirmation screens display AM/PM. The admin workshop editor uses an explicit 12-hour picker; equal opening/closing times mean 24-hour service.
- Bundled day/night workshop photos appear on workshop rows, workshop picker rows and detail headers. An image choice is stored in Firestore and SQLite; missing image metadata falls back to the day photo. Admins can choose either image.
- Added Tejgaon 24 Hour Auto Care and Badda Night & Day Motors to the live demo database. Both use opening/closing hour zero and the night image. The six older demo workshop documents received image metadata without replacing their other fields. The seeder now includes eight workshops and ten services.

## Executed checks

- Debug build and 48 JVM tests passed, including full prices/commission, first-hour access, midnight/noon formatting, 24-hour appointments crossing midnight, closed days, photo-cache round trips and historical booking preservation.
- All 68 Firestore emulator checks passed. Removed benefits are rejected, historical subscribers pay full price, and 24-hour reservations and image updates are validated.
- Android lint passed with zero errors and 70 warnings. Warnings remain; they are not claimed fixed by this update.
- Published `firestore.rules` to `cse489-project-8cf39` successfully. Confirmed eight demo workshop documents, including both new all-day schedules and image metadata.
- Installed the debug APK on the connected emulator without clearing data. Checked Home has no Featured filter or badge, the new workshop opens with its photo and “Open 24 hours (Dhaka time)”, and Engine Oil Change shows the full ৳3,500 price with AM/PM slots. Confirmed Profile no longer contains a Plus entry. No live booking was submitted during this smoke check.

## Remaining human device tests

1. Open Home and refresh. Search Tejgaon or Badda and inspect the image and 24-hour label. Open a daytime workshop and check its opening/closing AM/PM labels.
2. Book a service at the first opening hour, inspect the full price and submit using a dedicated demo account. Open My Bookings and confirm AM/PM labels; as admin inspect the 10% commission, then exercise the status flow.
3. In a 24-hour workshop, choose tomorrow and a service lasting more than an hour. Reserve 11:00 PM and verify it can finish after midnight. Cancel afterwards to release all locks. Check that a workshop closed on the following day disallows that crossing.
4. In Admin → Workshops, change opening/closing times using the 12-hour picker. Use the same opening and closing time for 24-hour operation, choose an image, save and rotate/reopen to verify persistence. Select minutes 00 because slots are hourly.
5. After browsing online, disconnect networking and reopen workshop lists/details. Photos and cached hours must remain visible. Restore networking.
6. Upgrade an installation with an old discounted booking and queued request. Existing saved booking amounts must stay unchanged; cancel/rebook retired queued quotes at the displayed full price. Historical cancellation/rating should still work.

## Image assets and prompts

Generated with the built-in imagegen tool, inspected, and copied unchanged into the app. They are fictional illustrative workshop photographs, not images of real businesses.

- `app/src/main/res/drawable-nodpi/workshop_day.png`
- `app/src/main/res/drawable-nodpi/workshop_night.png`

Day prompt:

> Generate a photorealistic editorial photograph for a fictional car-workshop booking app, wide landscape composition 1536x1024. A clean independent automotive service workshop in Dhaka Bangladesh in daylight, open service bays, a modern silver sedan on the ground, tools and a car lift visible naturally, warm orange details and teal accent, realistic concrete and metal textures, welcoming trustworthy professional environment. No text, no logos, no license plate letters, no people closeup. Frame so it crops beautifully to a wide 2:1 banner. This is a fictional illustrative workshop photograph, not a real business.

Night prompt:

> Generate a photorealistic editorial photograph for a fictional 24-hour car-workshop booking app, wide landscape composition 1536x1024. An independent automotive service workshop in Dhaka Bangladesh at blue hour, brightly illuminated open service bays, dark blue sedan parked outside, warm orange work lights with subtle teal accents, realistic tools and car lift, welcoming and professional. No text, no logos, no license plate letters, no people closeup. Frame so it crops beautifully to a wide 2:1 banner. Fictional illustrative workshop photograph, not a real business.
