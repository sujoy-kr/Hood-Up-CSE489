Current pricing and workshop changes are recorded in [WORKSHOP_UPDATE.md](WORKSHOP_UPDATE.md). Plus activation is now denied, new bookings use full prices with no benefit flags, and the updated rules have been deployed to the demo project. Earlier subscription descriptions below are historical.

# Hood Up security notes

## Publish Firestore rules

`firestore.rules` is the authorization policy. `firebase.json` points to it and retains the existing index configuration. A rule file in the repository does **not** secure a running Firebase project until it is published.

1. Sign in to the [Firebase console](https://console.firebase.google.com/) as the trusted project owner and select the project used by this app's `google-services.json`.
2. Open **Build → Firestore Database → Rules**, selecting the correct database if there is more than one. Save a copy of the currently published rules for recovery.
3. Replace the editor contents with the entire root `firestore.rules`. Review the diff, then click **Publish**. Reopen the app after propagation. Do not paste it into Realtime Database or Storage rules.
4. In **Firestore Database → Data**, audit every existing `users` document with role `admin`. Remove unexpected privileges using the trusted console before letting customers use the project. Review old data as well: new rules do not retroactively validate previous writes.
5. Run the device checks below using two customer accounts and one approved admin. Keep the prepared booking query indexes deployed; rules are not query filters.

These console actions were not performed by this task. Firebase documents both [console publication](https://firebase.google.com/docs/firestore/security/get-started) and [query authorization](https://firebase.google.com/docs/firestore/security/rules-query).

For repeatable **local-only** authorization checks, use Node/npm and Java 21+:

```sh
cd security-tests
npm ci
npm test
```

The suite starts the Firestore Emulator with the fixed **demo-hood-up** project ID, tests allow/deny cases, then stops it. It does not authenticate to or deploy to a real project. Test dependencies are development-only and pinned by `package-lock.json`; they are not packaged in the Android app.

## Access boundaries and transaction compatibility

| Data | Client access |
| --- | --- |
| `users/{uid}` | Owner reads/edits their profile, preserving UID and role. Admins may read all profiles but cannot edit other profiles through client rules. Client creation always has role `customer`, Plus false and expiry zero. No client deletes. |
| `users/{uid}/cars/{id}` | Owner-only CRUD, including against other admins. |
| `workshops`, `services` | Signed-in reads; admin catalog writes. Service money is nonnegative, capped at ৳10,000,000, with at most two decimals. Duration is 1–300 minutes. |
| Workshop rating | A completed booking owner can set/edit a 1–5 rating only with the matching aggregate update in the same transaction. `ratingBookingId` binds the aggregate to that booking. Sum/count/average must match the old rating delta; changing the same review never adds another review count. |
| `bookings` | Customers create only their own PENDING, synced booking, with current catalog/car/profile values and matching slot locks. Reads require ownership or admin role. Owners cancel only PENDING/CONFIRMED (idempotent CANCELLED retries allowed). Admin updates follow the forward status graph. No client may delete. |
| `workshops/{wid}/slots` | Signed-in users read minimal occupancy (opaque booking ID; document ID is the hour). Only a matching atomic customer booking can create locks. Locks cannot be overwritten; cancellation is required for release. No customer name, note, email, car or phone is exposed. |
| `booking_cancellations/{id}` | Owner-scoped immutable cancellation marker for an offline booking that has not been created remotely. A marker prevents a delayed transaction from recreating it. It does not permit creating a CANCELLED booking document. |
| All other paths | Denied by default. |

Admin checks use `get(users/{request.auth.uid}).data.role`. Booking prices, discounts, commission, hours/closed days, car ownership, priority eligibility and required lock records are verified by rules, not just by the UI. The five-hour service cap keeps the proof within Firestore's [document-access limits for atomic operations](https://firebase.google.com/docs/firestore/security/rules-conditions). Keep the rules' rates/limits aligned with `BusinessConfig` if those constants change. The app validates durations and monetary precision accordingly.

Availability now reads occupancy rather than querying every customer's booking document by workshop ID. Owner booking queries must include `userId == currentUid`. Existing old slot records might still contain a pseudonymous `userId`; new locks write only `bookingId`. Review/migrate old catalog/slot data in a controlled admin maintenance window if needed. Terminal local cancellations remain in owner-scoped SQLite history even if no remote booking document was ever created.

Server/Admin SDKs and console IAM access bypass client Security Rules; protect project-owner credentials separately. These rules do not implement per-account booking rate limits, authenticated app attestation or a paid entitlement system. Plus remains a deliberately simulated 30-day subscription: bounded owner activation is allowed without payment verification. A real subscription must be set by a trusted payment webhook/backend.

## Admin invite and provisioning

`ADMIN_INVITE_CODE` still comes only from root `.env` into BuildConfig. The comparison rejects missing values and uses `MessageDigest.isEqual`. **The code is compiled into the APK and can be extracted; a modified APK can bypass any Java comparison.** That limitation is recorded next to the check in AuthRepository.

The invite is therefore only a local UX check and **never grants a role**. The Admin access section explains owner approval rather than collecting an unused new account name/email/password. A correct code returns an approval-required message; neither correct nor incorrect codes create a Firebase account or admin document. Customer account creation remains available normally. The rules independently reject attempts to create an admin profile or change one's role, including requests sent outside the app.

To provision the first or another admin safely:

1. Have that person create a normal customer account using the app.
2. Verify the person/account through a trusted channel. In Firebase Authentication, identify their exact UID and verified email/account ownership.
3. As project owner, use Firestore **Data**, open `users/{that UID}`, and change only `role` to the string `admin`. Keep the other profile fields, including `uid`, intact.
4. Have them log out and use **Admin login**. Do not add a client rule that permits role promotion, and do not distribute an Admin SDK service-account file with the APK.

This intentionally supersedes the earlier demo's client-side “register as admin” behavior. Existing trusted admins can continue to sign in. A production invitation flow needs server-generated, short-lived, one-use approvals tied to a verified account; it cannot be secured by an APK secret.

## Gemini key restrictions and quota

The Gradle script reads `GEMINI_API_KEY` from root `.env` and generates `BuildConfig.GEMINI_API_KEY`, falling back to an empty string. GeminiClient sends it only in `x-goog-api-key`, never a URL or log. `.gitignore` excludes `.env`, credential variants and `local.properties`. Ignoring a file does not untrack a previously committed secret: remove it from repository history as appropriate and rotate any exposed key. Build output, APKs and debug reports also need careful distribution.

**BuildConfig is not a secret vault. The key can be extracted from the APK.** Google's [Gemini key guidance](https://ai.google.dev/gemini-api/docs/api-key) recommends a backend proxy for production mobile clients.

For this direct-call demo:

1. In **Google AI Studio → API keys**, identify the key/project without copying its value into documentation or screenshots. Apply **Gemini API only** restrictions. Key types and accepted restrictions can change; follow the current console for the actual key.
2. Open the key in **Google Cloud Console → APIs & Services → Credentials**. Where supported, choose **Application restrictions → Android apps**, add package `com.example.carworkshopbookingapp` and the **SHA-1 signing certificate fingerprint** of the installed APK. Include the correct debug/release or Play App Signing certificate as appropriate; upload and app-signing certificates may differ. Use Android Studio's Gradle `signingReport` to inspect the relevant certificate locally.
3. GeminiClient supplies `X-Android-Package` and `X-Android-Cert` (the installed signing certificate's SHA-1 without separators), as required by [Google Cloud's Android key restriction documentation](https://docs.cloud.google.com/docs/authentication/api-keys). Those public identity headers are spoofable, so they reduce accidental misuse but are not strong caller authentication. Test the actual restriction with a valid request and a mismatched package/certificate. If the provider/key type does not enforce the intended restriction, keep the key off released clients and use a proxy instead of claiming protection.
4. In the project's Gemini/Generative Language **Quotas & System Limits**, lower adjustable request/token quotas to a small demo cap. Review [project-wide Gemini rate limits](https://ai.google.dev/gemini-api/docs/rate-limits) and AI Studio usage. Add billing alerts and monitor usage. **A billing budget alert is not a hard spending cap**; where quotas cannot provide the required limit, enforce a hard application cap on the backend. Limits are per project, not a fresh allowance per key.
5. If a key is exposed, rotate/disable it and review usage. A new key compiled into another APK is still extractable.

No live key was printed, tested against Google, restricted or rotated during this task; no cloud quotas or billing settings were changed.

### Production alternative

Use an authenticated Cloud Function/Cloud Run proxy. Keep the Gemini key in Secret Manager, verify the Firebase ID token (and preferably App Check), enforce per-UID quotas/rate/size limits and an application spending cutoff, fetch the trusted service catalog server-side, validate structured results, and send the same minimized mechanical concepts. The APK should contain no Gemini key and no privileged admin invitation credential. Avoid logging symptoms, personal information or full provider errors on that backend.

## Gemini privacy and backups

No account/profile, Firebase UID/token, car, booking, location or workshop object is passed to GeminiProtocol. To avoid relying on unreliable personal-name redaction, the network payload now uses **fixed allowlisted mechanical concepts only** extracted locally from recent turns. Unknown text becomes a fixed follow-up placeholder. Catalog names/categories receive the same conversion; raw service IDs are replaced with temporary `service_0`, `service_1`, etc., mapped back on-device after the structured reply is validated. Prices/descriptions/keywords remain local. Request-contract tests assert that sample names, addresses, plates, emails, phone numbers and raw IDs are absent from the serialized JSON.

This deliberately sacrifices some free-language detail (including unrecognized languages/symptoms), so the bot may ask for clarification or use the local fallback more often. Full typed text remains in local chat history and may become an owner-scoped booking note if the user chooses a suggestion; it is not sent to Gemini. Ordinary HTTPS transport still exposes network-level metadata such as the connecting IP to the provider. Only public application package/certificate identity is added to the API headers.

The manifest explicitly has `android:allowBackup="false"`. Cloud/device-transfer XML rules also exclude databases, preferences and files, retaining the exclusions for platforms/manufacturers that consult transfer rules. Sessions, chat and queued booking data are not intentionally exported through Android backup. This is not encryption against a rooted device or physical access to an unlocked app. Logout clears the session, cancels known alarms and stops sync, while owner-scoped caches/queue remain on that device for later recovery.

## Recommended device/backend checks

| Check | Expected result |
| --- | --- |
| Publish the reviewed rules, then sign in as two different customers | Own profile/cars/bookings work; cross-account reads/writes and unfiltered booking queries fail. |
| Customer modifies role or tries to create an admin profile using an SDK/REST test | Denied even if the APK invite code is known. |
| Correct invite in Admin access, then owner console promotion | Invite alone never grants access; the approved account can log out and use Admin login. |
| Book a normal and a Plus service; try a duplicate/overlapping slot | PENDING creation, correct rounded price/commission and lock proof succeed; a conflicting reservation fails. |
| Queue offline, cancel before upload, then reconnect/retry | No CANCELLED booking creation; marker prevents resurrection and local history retains cancellation. |
| Complete/rate/edit a booking rating and attempt aggregate-only manipulation | Paired bounded changes succeed; invalid stars, another owner's rating and forged count/sum fail. |
| Try a service longer than five hours or price with more than two decimals | Admin form/server reject it; all seeded services remain supported. |
| Send typed identifiers alongside mechanical symptoms | Serialized request contains only mechanical concepts and temporary aliases; no raw personal/catalog identifiers. |
| Restrict package/certificate and quota in the actual cloud project | Valid demo request works, invalid identity is rejected if the provider supports the restriction, and quota errors use offline fallback. |
| Inspect backup configuration and log out with reminders active | Backup disabled; no account session is imported; listeners/alarms/notifications are cleaned up. |

Compilation and local emulator/JVM tests do not establish production publication, cloud restriction enforcement, device backup behavior or live Gemini operation. Those external checks remain recommended before the demo/release.

## Automated verification for this prompt

Executed locally:

- `./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest lint` with Android Studio's JDK: passed; 34 JVM tests, zero failures/errors, lint zero errors (90 non-blocking warnings), both APKs compiled.
- `npm test` in `security-tests`: Firestore rules compiled and all 56 local emulator allow/deny cases passed.
- `apksigner verify app/build/outputs/apk/debug/app-debug.apk`: passed. The built debug APK's public SHA-1 is recorded in `build/prompt13-apk-identity.txt`; use the installed release/Play signing identity for a release key restriction instead.
- Static checks verified the merged debug manifest has `allowBackup=false`, both requested ignore entries exist, and production Gemini BuildConfig access is confined to GeminiClient. Serialized request tests exercised privacy minimization and service-alias mapping without contacting Google.

Logs: `build/prompt13-build.log` and `build/prompt13-rules.log`. No connected Android device was available. Instrumentation was compiled, not run; rules publication, admin provisioning, live Google key restrictions/quotas and all phone checks remain external actions, not claimed test results.
