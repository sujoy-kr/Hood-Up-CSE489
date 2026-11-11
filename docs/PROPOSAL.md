# Project proposal — Hood Up

## Project title

**Hood Up: a car workshop booking app with offline booking and symptom-based service suggestions**

Android application: `com.example.carworkshopbookingapp`; Java; Android 7.0/API 24 minimum. This proposal documents the implementation now in the repository, rather than promising additional screens or integrations.

## Motivation

A customer may know that the brakes squeal or the AC feels warm without knowing which service to request. Choosing a workshop, explaining the symptom, arranging a suitable time and checking progress can become separate conversations. Connectivity can also disappear while the customer is arranging a booking.

Hood Up brings workshop browsing, car details, appointment selection and status updates into one flow. Its mechanic assistant suggests catalog services without claiming a diagnosis. An offline queue preserves a booking request until it can be checked and submitted online.

## Benefits and usage

| User | Intended use | Implemented benefit |
| --- | --- | --- |
| Customer | Compare workshops by area, rating, opening status and offered services | Search and filters, visible prices/durations, Featured badges and call/maps Intents |
| Customer unsure of the fault | Describe a symptom, choose a suggested service, then choose a workshop | Ask the Mechanic, local keyword fallback and a symptom note passed into booking |
| Customer with weak connectivity | Browse cached data and save a booking request | SQLite cache/queue and a visible “Waiting to sync” state; server acceptance is checked later |
| Customer after booking | Follow progress and manage an appointment | Timeline, permitted cancellation, reminders and completion rating |
| Workshop administrator | Maintain the catalog and move bookings through the work stages | Admin editors, status controls, customer dial action, dashboard and seven-day chart |

The six seeded Dhaka workshops and ten services are demo records, not a claim of business partnerships. Plus is a simulated subscription; there is no payment processor or pickup-dispatch system.

## Primary wireframes

Draw portrait screens using a dark background, orange primary actions and teal accents. Use 16dp spacing, 14dp rounded shape backgrounds, 16sp body text and controls at least 48dp high. A “card” below means an ordinary layout with a shape background, not CardView. Unless noted, tall forms scroll. Label arrows with the action that opens the next Activity.

| # / implemented screen | Blocks to sketch, top to bottom | Key controls and navigation arrows |
| --- | --- | --- |
| 1. Splash — `SplashActivity` | Centered hood/wrench logo, Hood Up name, loading indicator; error text and retry/login actions when restoration fails | After about one second minimum: valid customer → Home; approved admin → Admin; signed out → Login. Network/profile restoration can take longer. |
| 2. Login — `LoginActivity` | Logo/app name; title/subtitle; email label/input/error; password label/input/error; progress/error area; orange Log in; Create account; small Admin login at bottom | Log in → role-appropriate home with cleared task; Create account → Register; Admin login → Admin Login. |
| 3. Register — `RegisterActivity` | Name, phone, email, password, confirmation, each with inline error; progress area; Create account; existing-account link | Successful customer registration → Home; existing-account link → Login. |
| 4. Admin Login — `AdminLoginActivity` | Email/password form and Log in; collapsed “First time? Admin access” panel; inside: approval explanation, invite input/error and Check invite; customer-login link | Approved admin → Dashboard. An invite alone cannot create an admin: owner approval in Firebase is required. Customer link → Login. Do not sketch the superseded admin self-registration form. |
| 5. Home — `HomeActivity` | Optional thin offline banner; Hood Up label; “Hey, first name” with profile icon; prominent mechanic-bot card; search; two-column All/Featured/Open now/Top rated chips; Refresh/Retry/loading/empty area; workshop rows | Each row: name, area, rating, open/closed pill, Featured badge if applicable, starting price. Row → Workshop Detail; bot card → Chat; profile icon → Profile. Fixed LinearLayout bottom bar: Home / My Bookings / Profile. Header and rows scroll together. |
| 6. Workshop Detail — `WorkshopDetailActivity` | Back; workshop name, address, rating, hours and description; Call and Directions; Refresh/state; service rows with description, BDT price, duration and Book this service | Call → dialer; Directions → maps handler; service button → Booking carrying workshop/service IDs. |
| 7. Profile — `ProfileActivity` | Back/title; initials circle; editable name, phone and contact email; explanation that sign-in email stays unchanged; Save profile; My Cars, My Bookings, Hood Up Plus; reminder switch; About; Logout | Buttons → their Activities. Logout → Login with task cleared; reminder preference is owner-scoped locally. |
| 8. Cars — `CarsActivity`, `CarFormActivity` | Cars list header: Back, title, Add a car, Refresh, progress/error/empty state. Rows show make/model/year, plate, Edit and Delete. Separate form: make, model, year, plate, errors and Save | Add/Edit → Car Form; Save → Cars. Delete opens a confirmation dialog. |
| 9. Booking — `BookingActivity` | Back/title; workshop/service summary with price and duration; car Spinner and Add a car; date button; availability text and time-chip GridLayout; Refresh; optional pickup checkbox for Plus; note; base/total price and “protected by Hood Up”; progress/error; Confirm booking | Date → DatePickerDialog; Add a car → Car Form; Confirm → Booking Success. Occupied slots are disabled; past/closed selections are rejected. Plus gets the first opening-hour slot if available. Commission is not shown to customers. |
| 10. Booking Success — `BookingSuccessActivity` | Checkmark, submitted/queued title, explanation, workshop/service/car/date/price/reference details, My Bookings action | Queued version explicitly says it will be sent after reconnecting. My Bookings → list; do not label a queued request as already accepted by the server. |
| 11. My Bookings — `MyBookingsActivity` | Offline banner; Back/title; Upcoming and History chips; Refresh/state; rows with service/workshop/car/date/price/status and optional Waiting to sync | Row → Booking Detail. Bottom bar matches Home. Completed/cancelled entries appear in History; an overdue active booking remains Upcoming. |
| 12. Booking Detail — `BookingDetailActivity` | Back/Refresh/state; full booking card, note/reference, waiting label; vertical Booked → Confirmed → In progress → Ready → Completed timeline; Call; conditional Cancel; conditional five-star row and Save rating | Cancel only for PENDING/CONFIRMED, with confirmation. Rating only after COMPLETED. Notification tap → this screen; Back from that notification route → Home. |
| 13. Ask the Mechanic — `ChatbotActivity` | Back and Clear chat; title/privacy text; five quick-start chips; ScrollView of right orange user bubbles and left dark bot bubbles; typing text; service suggestion cards with catalog price and urgency; error/Retry; input and Send | Quick start/Send → append conversation; suggestion → Pick a workshop; Clear chat → confirmation. Empty send disabled; no diagnosis or invented price. |
| 14. Pick a workshop — `PickWorkshopActivity` | Back/title, selected-service description, Refresh/state; familiar workshop rows restricted to workshops offering that service | Row → Booking, carrying workshop ID, service ID and editable symptom note. |
| 15. Hood Up Plus — `PlusActivity` | Back/title; ৳199/month; priority, pickup request, discount and early-reminder benefits; prominent “Demo mode — no real payment”; active-until state; Subscribe | Subscribe writes a bounded 30-day demo entitlement; active accounts do not renew again by repeated taps. Back → Profile. |
| 16. About — `AboutActivity` | Back/title; app purpose, commission/featured/Plus explanation and course-demo/AI limitations | Back → previous screen. |
| 17. Admin Dashboard — `AdminDashboardActivity` | Admin heading and Logout; Bookings/Workshops/Services/Seed demo data/Refresh stats actions; two-column stat cards; bookings-per-appointment-day Canvas chart | Stats: today, pending, completed appointments this month, estimated commission on those completed appointments. Menu → admin lists; Seed → confirmation; Logout → Login. Actual code places menu actions before the loaded statistics. |
| 18. Admin Bookings / Detail | List header: title, Refresh, vertically stacked All/status chips, workshop Spinner and loading/error/empty state; familiar booking rows. Detail: saved customer/car/service/slot/note/price/commission/flags; customer phone dial action; permitted next-status buttons | Row → Admin Booking Detail. PENDING → CONFIRMED/CANCELLED; CONFIRMED → IN_PROGRESS/CANCELLED; IN_PROGRESS → READY; READY → COMPLETED. Each change is confirmed. |
| 19. Admin Workshops / editor | List title, Add, Refresh and rows with name/area/Featured. Editor: name, area, description, address, phone, opening/closing hour, Featured switch, offered-services multi-select dialog, Save, conditional Delete, Back to list | Add/row switches content within the same Activity. Save → list; Delete requires confirmation. No map picker or image upload is implemented. |
| 20. Admin Services / editor | List title, Add, Refresh and name/BDT-price rows. Editor: name, category, description, base price, duration, comma-separated keywords, Save, conditional Delete, Back to list | Same-Activity editor; keywords feed local fallback. Duration 1–300 minutes and prices at most two decimals. |

### Navigation overview to put beside the sketches

```text
Splash → Login → Register
   └── signed-in customer → Home → Workshop Detail → Booking → Success → My Bookings
                            ├── Chat → Pick a workshop ────────────┘       └── Detail
                            └── Profile → Cars → Car Form
                                       ├── Plus
                                       └── About / Logout → Login
Login → Admin Login → approved Admin Dashboard → Bookings → Admin Detail
                                             ├── Workshops [list/editor]
                                             └── Services [list/editor]
Notification → Home → Booking Detail
```

Wireframes describe current layouts and navigation from `ui/`, `res/layout/` and the adapters. They are a sketching specification; no Figma file or actual wireframe images are claimed as produced by this document.
