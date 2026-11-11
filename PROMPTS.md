# Hood Up — Build Prompts

Paste these into Codex **one at a time, in order**. Wait for each to finish, build/run the app, fix anything that breaks, then paste the next.

**App name:** Hood Up (car workshop booking). **Language:** Java. **Package:** `com.example.carworkshopbookingapp`.

---

## Before you start (do these yourself)

1. **Firebase console → Build → Authentication → Sign-in method:** enable **Email/Password**.
2. **Firebase console → Build → Firestore Database:** create it (test mode is fine for now; Prompt 15 locks it down).
3. Add one more line to `.env` (invent any code you like, it's the secret that lets someone register as admin):
    ```
    ADMIN_INVITE_CODE=pick-something-here
    ```
4. Make sure `.env` is in `.gitignore` and never goes in the zip you submit with a real key (or rotate the key after the demo).
5. Have a phone with USB debugging on, or an emulator with Google Play services, and internet access.

---

## Prompt 0 — Project rules (run once)

This prompt creates `AGENTS.md`; no existing project instruction file is required.

```text
Read "Project Info.txt" and the existing Android Studio project in this folder. Then create an AGENTS.md in the project root that records the rules for this whole build, so I don't have to repeat them in every prompt:

- App: "Hood Up", a car workshop booking app for customers and admins. Java only, package com.example.carworkshopbookingapp, minSdk 24.
- Course constraints (from Project Info.txt): use ONLY basic layouts: LinearLayout, RelativeLayout, FrameLayout, TableLayout, GridLayout, ScrollView, plus ListView and Spinner. NO Material Components, NO RecyclerView, NO CardView, NO ConstraintLayout, NO Jetpack Compose, NO Navigation component, NO Fragments-heavy architecture. Plain Activities + Intents.
- Required features and where they live: Activity+Intent+Layouts; local storage (SQLite) + remote storage (Firebase Auth + Cloud Firestore); Threads (ExecutorService + Handler, never network/DB on the main thread); a Service; Broadcasts (custom + system); Notifications; a Business Model implemented in-app; AI integration (Gemini symptom-to-service chatbot).
- Design: dark mode only. Palette (define in res/values/colors.xml and use only these names):
  bg #0E1116, surface #171B22, surface_alt #212733, stroke #2C3342, primary #FF8A3D (warm orange), primary_dark #E5701F, accent #2DD4BF (teal), text #ECEFF4, text_muted #8B93A5, success #4ADE80, warning #FBBF24, danger #FF5C6C.
  Build the "card / button / chip / input" look with XML shape drawables (rounded corners, 1dp stroke), not Material. Consistent 16dp spacing, rounded 14dp corners, readable 16sp body text.
- Secrets: GEMINI_API_KEY and ADMIN_INVITE_CODE are read from the root .env file at build time into BuildConfig fields. Never hardcode them in source.
- Code style: small classes, comments only where non-obvious, no unused code. Package layout: ui/ (activities), data/ (models, SQLite helper, Firestore repositories), service/, receiver/, ai/, util/.
- After each task, make sure the project compiles (./gradlew assembleDebug) and tell me exactly what to test on the device.

Don't change any app code yet. Only write AGENTS.md.
```

---

## Prompt 1 — Gradle, theme, design system

```text
Set up the project foundation per AGENTS.md.

1. app/build.gradle.kts: remove the material and constraintlayout dependencies (and the unused androidx.activity.ktx if nothing needs it). Keep appcompat. Add firebase-auth and firebase-firestore (use the existing BoM; drop firebase-analytics unless something uses it). Enable buildFeatures { buildConfig = true } and read GEMINI_API_KEY and ADMIN_INVITE_CODE from the root .env file (parse it in the Gradle script; fall back to an empty string if missing) into BuildConfig fields.
2. Replace themes (both values and values-night) with a single dark AppCompat theme (Theme.AppCompat.NoActionBar parent) using my palette: window background, status/navigation bar colors, text colors, no action bar.
3. Write colors.xml, dimens.xml, styles.xml and shape drawables: bg_card, bg_card_alt, bg_input, bg_input_focused, btn_primary (filled orange, pressed state darker), btn_secondary (stroke only), btn_danger, chip_default, chip_selected, status pills for each booking status.
4. Create reusable styles for: screen title, section header, body text, muted text, primary button, secondary button, text input (EditText), chip.
5. Create a simple vector app icon (a stylised open car hood / wrench in orange on dark) and replace the launcher icon.
6. Set app_name to "Hood Up".

Build with ./gradlew assembleDebug and confirm it compiles. Then replace activity_main.xml with a temporary screen that shows every style (title, card, both buttons, input, chips, status pills) so I can review the design on my phone.
```

---

## Prompt 2 — Models, SQLite, repositories, threading helper

```text
Build the data layer. No UI yet.

Models (plain Java classes in data/model): User (uid, name, phone, email, role "customer"|"admin"), Car (id, make, model, year, plate), Workshop (id, name, address, area, phone, rating, openHour, closeHour, featured, serviceIds list, description), ServiceItem (id, name, description, category, basePrice, durationMin, keywords list used for the AI fallback), Booking (id, userId, userName, workshopId, workshopName, serviceId, serviceName, carLabel, dateTimeMillis, price, commission, status, note, createdAt, synced flag). Booking status values: PENDING, CONFIRMED, IN_PROGRESS, READY, COMPLETED, CANCELLED.

Local storage: a SQLiteOpenHelper (data/local/LocalDb) with tables: workshops_cache, services_cache, cars, pending_bookings (offline queue), chat_history, session (key/value). Provide DAO-style methods.

Remote storage: Firestore repositories (data/remote) for users, workshops, services, cars (users/{uid}/cars) and bookings. Collections: users, workshops, services, bookings. Every method takes a callback interface (onSuccess / onError); never block the calling thread.

Threading: a util/AppExecutors class with a shared ExecutorService for disk/network work and a main-thread Handler. All SQLite calls go through the executor, and callbacks are posted back to the main thread.

Caching rule: workshops and services are read from local cache first (instant), refreshed from Firestore in the background, then cache is updated and the UI callback fires again with fresh data.

Add a small JUnit test or two for the pure logic you add (e.g. commission calculation). Make sure it compiles.
```

---

## Prompt 3 — Splash, login, registration (customer)

```text
Build the entry flow with plain Activities and Intents.

- SplashActivity (launcher): logo + app name on dark background, ~1s, then routes: signed-in customer -> HomeActivity, signed-in admin -> AdminDashboardActivity, else LoginActivity. Use the session table plus FirebaseAuth.getCurrentUser().
- LoginActivity: email + password inputs, "Log in" primary button, "Create account" link to RegisterActivity, and a small, tasteful "Admin login" text button at the bottom that opens AdminLoginActivity. Validate inputs inline (error text under the field in danger color), disable the button and show a progress indicator while the request runs. Show friendly error messages for wrong password / no network / unknown account.
- RegisterActivity: name, phone, email, password, confirm password. Creates the Firebase Auth user and a users/{uid} document with role "customer". Then goes to HomeActivity with the back stack cleared.
- Support the keyboard properly (ScrollView so fields stay reachable).
- AdminLoginActivity: email + password; after Firebase sign-in, read users/{uid} and only continue if role == "admin", otherwise sign out and show "This account isn't an admin". Include a collapsed "First time? Register as admin" section that asks for name, email, password and the invite code, compares the code to BuildConfig.ADMIN_INVITE_CODE, and creates the user with role "admin".

All Firebase calls via callbacks from the repository layer. Update AndroidManifest (launcher, activities, INTERNET permission). Make a placeholder HomeActivity and AdminDashboardActivity if they don't exist yet. Build and tell me how to test customer register/login and admin register/login.
```

---

## Prompt 4 — Home and workshop browsing

```text
Build the customer HomeActivity and workshop browsing.

HomeActivity layout: greeting ("Hey, <first name>") + a profile icon at top, a prominent "Not sure what's wrong? Ask the mechanic bot" card (opens ChatbotActivity, built later, stub it now), a search input, filter chips (All, Featured, Open now, Top rated), then the workshop list. Bottom bar built from a LinearLayout with 3 items: Home, My Bookings, Profile (each opens its own Activity, stub the ones not built yet).

Workshop list: use ListView with a custom BaseAdapter (ViewHolder pattern). NO RecyclerView. Each row is a card showing name, area, rating, open/closed pill, "Featured" badge, and starting price. Featured workshops sort first. The search box and chips filter the list live.

WorkshopDetailActivity: header with name, address, rating, hours; "Call" button (Intent ACTION_DIAL) and "Directions" button (Intent ACTION_VIEW geo: URI to Google Maps); a list of the services this workshop offers with price and duration; a "Book this service" button per service that opens BookingActivity (stub), passing workshopId and serviceId via Intent extras.

Data comes from the cache-first repositories (Prompt 2). Show a skeleton/empty state and a pull-to-refresh style "Refresh" button (no SwipeRefreshLayout). Since I have no data yet, also add a hidden dev helper in the admin area later (Prompt 11) to seed demo data. For now, add a temporary debug-only seed method that writes 6 realistic workshops and 10 services to Firestore, callable from a long-press on the app title, so I can see real rows.

Seed data: workshops in a realistic city (use Dhaka, Bangladesh areas like Gulshan, Banani, Uttara, Dhanmondi, Mirpur, Mohammadpur), prices in BDT (৳), services like Engine Oil Change, Brake Pad Replacement, AC Service, Battery Replacement, Tyre Rotation & Balancing, Wheel Alignment, Full Car Wash & Detailing, Engine Diagnostics, Suspension Repair, General Service.
```

---

## Prompt 5 — My cars and the booking flow

```text
Build the booking flow.

CarsActivity (from Profile): list of the user's cars (ListView), add/edit/delete via a simple form activity (make, model, year, plate). Store in users/{uid}/cars and mirror into the local cars table.

BookingActivity (receives workshopId, serviceId; optionally a prefilled note from the chatbot):
1. Summary card of workshop + service (price, duration).
2. Car selector: a Spinner of the user's cars, with "Add a car" if none.
3. Date: a button that opens a DatePickerDialog (block past dates and closed days if the workshop has them).
4. Time slot: a GridLayout/TableLayout of time-slot chips from openHour to closeHour at 1-hour steps; slots already taken for that workshop+date (query bookings) are disabled and greyed out.
5. Optional note EditText.
6. Price breakdown: service price, and a line showing the booking is protected by Hood Up (do not show the commission to the customer).
7. "Confirm booking" button.

On confirm, run on the background executor: compute commission (10% of price, put the rate in one constant), write the booking to Firestore with status PENDING. If there's no network, insert into pending_bookings (synced = false) and tell the user "Saved — we'll send it as soon as you're back online". Then show BookingSuccessActivity with a checkmark and details and a button to My Bookings.

Guard against double-tap and double-booking the same slot (check again inside the write).
```

---

## Prompt 6 — My bookings, status tracking, cancel

```text
Build MyBookingsActivity (ListView, no RecyclerView).

- Two chips at the top: Upcoming and History. Each row shows service, workshop, car, date/time, price and a coloured status pill.
- Tapping a row opens BookingDetailActivity: full details, a status timeline built with a vertical LinearLayout (Booked -> Confirmed -> In progress -> Ready -> Completed, current step highlighted), a Call workshop button, and "Cancel booking" (only for PENDING/CONFIRMED, with an AlertDialog confirmation).
- After a booking is COMPLETED, show a simple 1-5 star rating row and save the rating, then update that workshop's average rating (transaction).
- Include bookings that are still in the offline queue, marked with a small "Waiting to sync" label.
- Data: Firestore query by userId, ordered by dateTimeMillis, with an executor + callback. Keep a SQLite copy so the screen works offline.

Make the list auto-update when a booking's status changes (we'll hook that to a broadcast in Prompt 9, so structure the screen with a reload() method).
```

---

## Prompt 7 — AI: symptom-to-service chatbot (Gemini)

```text
Build the AI feature: a symptom-to-service chatbot called "Ask the Mechanic".

ai/GeminiClient.java
- Plain HttpURLConnection + org.json (no extra libraries). POST to the Gemini REST generateContent endpoint, API key in the x-goog-api-key header from BuildConfig.GEMINI_API_KEY. Put the model name in one constant (default "gemini-2.5-flash"; if that model is unavailable, say so clearly in the error so I can change it). Timeouts: 15s connect, 30s read.
- Always runs on the background executor; result delivered to the main thread by callback.
- Multi-turn: send the recent conversation history (last ~8 messages) with each request.
- System instruction: you are the Hood Up mechanic assistant. Talk like a friendly, honest mechanic: plain language, no jargon, max ~70 words. Ask one short follow-up question if the symptom is too vague. You MUST only recommend services from the catalog that I inject into the prompt (list of id + name + category, loaded from services_cache). Never invent prices. Never claim certainty; mention when a physical inspection is needed. If the problem sounds dangerous (brake failure, smoke, fuel smell, overheating), tell the user to stop driving and set urgency to HIGH.
- Ask for structured output: responseMimeType application/json with a responseSchema so the reply is always {"reply": string, "urgency": "LOW"|"MEDIUM"|"HIGH", "serviceIds": [string]}. Parse defensively.

ai/FallbackAdvisor.java
- A keyword matcher over ServiceItem.keywords so the bot still gives a useful answer if the API fails, the key is missing, or the rate limit is hit. Label those replies subtly ("offline suggestion").

ChatbotActivity
- Dark chat UI using a ScrollView + vertical LinearLayout of bubble TextViews (user right/orange-tinted, bot left/surface_alt). NO RecyclerView. Typing indicator ("Checking under the hood...") while waiting. Quick-start chips: "Strange noise when braking", "AC not cooling", "Car won't start", "Steering pulls to one side", "Warning light on".
- When the bot returns serviceIds, render under its message a tappable suggestion card per service (name, from-price, urgency pill). Tapping opens a "Pick a workshop" screen listing workshops that offer that service (reuse the workshop list), then BookingActivity with the symptom text prefilled as the note.
- Persist chat_history in SQLite, with a "Clear chat" action.
- Handle errors with a friendly in-chat message and a Retry button. Disable the send button while a request is running. Don't send an empty message.

Add a README section "AI usage (where / why / how)" to docs/AI_USAGE.md describing exactly what this feature does, why it exists, prompt design, structured output, fallback and privacy (what data is sent: only the typed symptoms and the service catalog, never name/email/phone). I'll reuse it in the report.
```

---

## Prompt 8 — Offline queue sync + Service

```text
Implement the required Service and the offline sync.

service/BookingSyncService (a started Service, not a bound one):
- Starts when the app opens for a signed-in user and when connectivity returns.
- Work on a background thread (not the main thread): (1) push everything in pending_bookings to Firestore, mark them synced, remove from the queue; (2) attach a Firestore snapshot listener on the user's bookings and, when a status changes, update the local copy, fire the status notification (Prompt 9 creates the notification helper, so for now call a NotificationHelper stub) and send the BOOKING_STATUS_CHANGED broadcast.
- Use a foreground service with a low-priority ongoing notification only while it is actively syncing, and stop itself when idle and the queue is empty. Declare the right foregroundServiceType for the target SDK and request the needed permissions in the manifest. Handle Android 12+ background-start restrictions properly (start it from a visible activity or via a receiver that is allowed to).
- Make it idempotent: restarting must not send duplicate bookings (use the booking id as the Firestore document id).

Start it from HomeActivity.onStart and stop the listener on logout. Test plan for me: turn on airplane mode, make a booking, confirm it shows "Waiting to sync", turn the network back on, and confirm it appears in Firestore.
```

---

## Prompt 9 — Broadcasts + Notifications

```text
Add Broadcasting and Notifications.

NotificationHelper (util):
- Create channels on Android 8+: "Booking updates" (default importance), "Reminders" (high), "Sync" (min).
- Request POST_NOTIFICATIONS at runtime on Android 13+ with a short in-app explanation before the system dialog (do it on first Home launch, don't nag if denied).
- Notification types: booking submitted, status changed (Confirmed / In progress / "Your car is ready" / Completed / Cancelled), and an appointment reminder. Tapping a notification deep-links via a PendingIntent to that BookingDetailActivity (set correct PendingIntent flags and a proper back stack with TaskStackBuilder).

Broadcasts (receiver/ package):
1. ConnectivityMonitor: use ConnectivityManager.NetworkCallback (the old CONNECTIVITY_ACTION manifest receiver no longer works) and, when the network returns, send a custom broadcast ACTION_NETWORK_RESTORED which starts BookingSyncService. Also show a thin "You're offline" banner in HomeActivity and MyBookingsActivity while offline.
2. Custom broadcast ACTION_BOOKING_STATUS_CHANGED sent by the service (package-restricted with setPackage); MyBookingsActivity and BookingDetailActivity register a dynamic receiver in onStart/onStop and call reload().
3. ReminderReceiver + AlarmManager: when a booking becomes CONFIRMED, schedule a reminder 2 hours before (use inexact alarms so no special permission is needed; if it is already past, skip). Reminder shows the Reminders-channel notification.
4. BootReceiver (BOOT_COMPLETED): reschedule all future reminders from SQLite after reboot.
5. Cancel the alarm if the booking is cancelled.

Set android:exported correctly on every receiver. Explain in a short comment block at the top of each receiver which broadcast it handles. Tell me how to test each one (including adb commands for boot and the custom broadcast).
```

---

## Prompt 10 — Profile, settings and the business model (customer side)

```text
Build ProfileActivity and the customer-facing parts of the business model.

Profile: avatar circle with initials, name/phone/email (editable), My Cars, My Bookings, Notification settings (toggle reminders; stored in SharedPreferences), Logout (clear session, stop service, cancel alarms, back to LoginActivity with cleared back stack), and an About screen.

Business model (implemented, not just described):
1. Commission: every booking stores a 10% commission for Hood Up (already computed in Prompt 5).
2. Featured placement: workshops with featured = true appear first with a "Featured" badge (admins mark a workshop as featured in Prompt 11; in the report this is the "pay for visibility" revenue).
3. Hood Up Plus (simulated subscription, no real payment): a PlusActivity that sells "Hood Up Plus — ৳199/month" with benefits: priority slots, free pickup request flag on bookings, 5% off service price, and early reminders. A "Subscribe" button just writes plus = true and plusUntil to the user's document and clearly says "Demo mode — no real payment". In BookingActivity apply the 5% discount for Plus members and show the discounted price (commission is calculated on the discounted price).
4. Keep all rates in one BusinessConfig class so it's easy to explain.

Add docs/BUSINESS_MODEL.md explaining the three revenue streams with an example calculation, so I can reuse it in the report.
```

---

## Prompt 11 — Admin dashboard and management

```text
Build the admin side. Everything here must check that the signed-in user is role "admin" and bounce to LoginActivity otherwise.

AdminDashboardActivity:
- Header "Admin", logout button.
- Stat cards in a TableLayout/GridLayout: today's bookings, pending bookings, completed this month, estimated commission earned this month (sum of commission on COMPLETED). Compute on a background thread.
- Menu buttons: Bookings, Workshops, Services, Seed demo data.

AdminBookingsActivity (ListView): filter chips by status and a workshop Spinner. Tap a booking -> AdminBookingDetailActivity with customer name/phone (tap to call), car, service, slot, note, and status buttons that move it forward through the allowed transitions only: PENDING -> CONFIRMED or CANCELLED, CONFIRMED -> IN_PROGRESS or CANCELLED, IN_PROGRESS -> READY, READY -> COMPLETED. Updating the status writes to Firestore; the customer's service listener then fires the notification automatically.

AdminWorkshopsActivity: list + add/edit/delete workshop (name, area, address, phone, hours, description, featured switch, and a multi-select dialog to choose which services it offers). Confirm before deleting.

AdminServicesActivity: list + add/edit/delete services (name, category, base price, duration, keywords as comma separated — the keywords feed the chatbot fallback and the catalog given to Gemini).

"Seed demo data" button: replaces the temporary seed from Prompt 4 with a proper confirm dialog; remove the long-press hack from the Home screen.

Add a small bar chart of bookings per day for the last 7 days drawn with a custom View (Canvas), no chart libraries.
```

---

## Prompt 12 — Polish: UX, edge cases, performance

```text
Do a polish and robustness pass across the whole app.

1. Consistency: every screen uses the design system from Prompt 1. Fix spacing, text sizes, touch targets (>=48dp), contrast, and status/nav bar colors.
2. States: every list has loading, empty and error states with a retry button.
3. Rotation and process death: no crashes when rotating; keep form input and chat history. (Or lock portrait if that is cleaner, and explain.)
4. Back stack: back from Home exits, logout clears everything, deep-link notifications return to Home on back.
5. Input safety: trim and validate every form; handle Firestore permission errors and timeouts with readable messages.
6. Threads audit: search the code for any Firestore/HTTP/SQLite call that could run on the main thread and fix it. Also make sure callbacks never touch the UI after the Activity is destroyed.
7. Memory/leaks: unregister receivers, remove listeners, no static Activity references.
8. Add subtle touches: ripple/pressed states on buttons and rows, a fade-in on list items, haptic feedback on confirming a booking.
9. Remove dead code, unused resources and TODOs; run ./gradlew lint and fix real errors.

Give me a manual QA checklist (as a table) I can run on the phone before the demo.
```

---

## Prompt 13 — Security rules and secrets

```text
Harden the app.

1. Write firestore.rules in the project root and explain how to publish it in the console:
   - users/{uid}: a user can read/write only their own document and cannot change their own role; admins can read all.
   - users/{uid}/cars: owner only.
   - workshops and services: anyone signed in can read; only admins can write (except the rating fields, which signed-in users may update only through a bounded rating change).
   - bookings: a customer can create bookings only for themselves with status PENDING, read/cancel only their own; admins can read/update all; nobody can delete.
   Admin check should use the role in users/{uid} via get().
2. Make sure the admin invite code check can't be abused in an obvious way, and note honestly in a comment what the limitation is (the code ships inside the APK).
3. Gemini key: confirm it only comes from .env via BuildConfig, add .env and local.properties to .gitignore, and add a docs/SECURITY_NOTES.md explaining: the key can be extracted from the APK, so I should restrict it to this package name/SHA-1 in Google AI Studio / Google Cloud and set a quota cap; and the production alternative is a Cloud Function proxy.
4. Disable allowBackup for data containing session info, and make sure no PII is sent to Gemini.
```

---

## Prompt 14 — Tests and release build

```text
Add a few meaningful tests and prepare a release build.

- Unit tests (JUnit) for: commission and Plus-discount calculation, booking status transition rules, FallbackAdvisor keyword matching, Gemini response parsing (including malformed JSON), slot availability calculation.
- Make sure ./gradlew test passes.
- Create a signed-by-debug-or-release APK instructions: how I produce app-debug.apk and a release APK, where it lands, and how to install it on a phone with adb or by file transfer.
- Verify the minSdk 24 build works (no API calls above 24 without a version check).
- Produce a final README.md for the repo: what the app is, features, setup (Firebase, .env), architecture diagram in ASCII, and how each course requirement is satisfied (a table: requirement -> file/class).
```

---

## Prompt 15 — Deliverables: proposal, report, slides, video

```text
Produce the course deliverables as Markdown in a docs/ folder, based on the real code in this repo (not guesses):

1. docs/PROPOSAL.md: project title, motivation, benefit/usage, and a list of primary wireframes (describe each screen in enough detail that I can sketch it by hand or in Figma — layout blocks, key buttons, navigation arrows).
2. docs/REPORT.md with the required sections: Overview/Introduction (motivation, challenges faced during development — pull real ones from our work, e.g. Android 12+ foreground service limits, broadcast restrictions, no RecyclerView, offline sync, Gemini rate limits), Benefit/Usability, Implementation Details (what exceptional things we did: offline queue + sync service, structured-output AI with fallback, custom Canvas chart, commission/Plus model), Business Model, Contribution of each member (leave a table with placeholders for names), and the AI section (where, why, how). Mention requirement-to-code mapping.
3. docs/SLIDES.md: a 12–14 slide outline with the speaker notes, and a split so every group member presents a part (placeholders for member names).
4. docs/VIDEO_SCRIPT.md: a 45–60 second marketing video storyboard with shot list, on-screen text and voiceover, plus what to screen-record on the phone for each shot.
5. docs/DEMO_SCRIPT.md: a step-by-step live demo order that touches every required feature (Activity/Intent, local+remote storage, threads, service, broadcast, notification, business model, AI) so nothing is missed in front of the instructors.

Keep wording natural and specific, avoid marketing fluff.
```

---

## If something breaks

Use this after any prompt when the build or app misbehaves:

```text
The app/build failed. Here's the error or what I saw on the device: <paste log or describe>. Find the root cause in the code (don't guess), fix it, rebuild with ./gradlew assembleDebug, and tell me what you changed and how to re-test.
```

## Notes

- **Gemini model name** changes over time. If Prompt 7 gives a "model not found" error, ask ai to check the current free-tier model on ai.google.dev and update the constant.
- **Free-tier limits** apply to the chatbot (a handful of requests per minute). The fallback advisor exists so the demo never dead-ends.
- **Test mode** Firestore rules expire after ~30 days, so run Prompt 13 well before the demo and publish the rules.
