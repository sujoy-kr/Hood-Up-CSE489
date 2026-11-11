> Prompt 13 security update: client-side admin registration is disabled. Create a customer account and have the trusted project owner approve its UID in the console; see SECURITY_NOTES.md. Earlier admin-registration steps below describe the original demo flow.

# Prompt 3 device checks

## Build and install

Run outside the restricted runner sandbox:

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest
```

Install `app/build/outputs/apk/debug/app-debug.apk` on a device with API 24 or newer. Use a Firebase project configured for this Android app, Email/Password Authentication, and Cloud Firestore. If sign-in reports that the provider is unavailable, enable Email/Password in Firebase Authentication. If profile access fails, inspect the project's Firestore access rules; do not open database access publicly to bypass an error.

The root `.env` must supply `ADMIN_INVITE_CODE` for admin registration. Rebuild after changing it. Do not paste its value into test reports.

## Customer flow

1. Start signed out (or use Sign out on either placeholder screen). Launch Hood Up. Confirm a dark logo/name splash appears for about one second before Login. A network-dependent session check may take longer.
2. Submit an empty login form and an invalid email. Confirm danger-colored errors appear below the fields without a request.
3. Choose Create account. Submit missing name/phone, an invalid email, a password shorter than six characters, and mismatched confirmation. Confirm each affected field has an inline error.
4. Enter a new email and valid details. Submit once. Confirm controls disable and a progress indicator appears. Success opens the Home placeholder. Back must not return to a login or registration form.
5. In Firebase, verify the Auth account and `users/{uid}` document exist with the same UID and `role: customer`, plus the entered name, phone, and email. Passwords must not appear in Firestore or the SQLite session.
6. Close and relaunch the app while still signed in. Confirm splash routes to Home. Sign out, relaunch, and confirm Login.
7. Log in with the registered customer. Confirm Home. Sign out and try a wrong password, an unknown email, and registering an already-used email. Confirm friendly messages and re-enabled controls. Firebase email enumeration protection may intentionally use the same generic credentials message for unknown emails and wrong passwords.

## Admin flow

1. On Login, select Admin login. Confirm registration fields are initially collapsed. Expand First time? Register as admin.
2. Enter a new name/email/password and an incorrect invite code. Confirm an inline invite error, with no Auth account created. An empty build-time invite must also reject registration.
3. Enter the configured invite code and register. Confirm the Admin dashboard placeholder opens with the old stack cleared. Verify the matching Firebase user document has `role: admin`.
4. Close and relaunch while signed in. Confirm the splash routes to Admin dashboard. Sign out, open Admin login, and sign in with the admin credentials; confirm Admin dashboard again.
5. Sign out and try the customer credentials through Admin login. Confirm the exact message “This account isn't an admin”, controls re-enable, and Firebase signs the customer out. Relaunch; it must show Login rather than Admin dashboard.
6. Sign in using admin credentials through the regular login form. Confirm role-based routing still opens Admin dashboard.

## Keyboard, lifecycle, and offline checks

- In light and dark system modes, confirm the app stays dark and text/system-bar icons remain readable.
- On a small device and in landscape, focus every field, including customer confirmation and the expanded admin invite. Scroll to the submit button with the keyboard open. Confirm fields and controls are reachable and avoid system bars/cutouts.
- Rotate during a sign-in or registration request. Confirm progress and disabled controls persist, no second request is submitted, and completion routes once. Rotate an expanded admin form and confirm it stays expanded. Password and invite fields deliberately do not restore their text after recreation.
- Double-tap submit during a request. Confirm only one account/request is created. Background the app during a request and return; confirm completion is handled when the screen is started again.
- While signed out, disable connectivity and attempt login/registration. Confirm a friendly connection error and restored controls. Reconnect and retry.
- After one successful online sign-in has populated the SQLite session, relaunch offline. Confirm a valid cached session is used only for the same Firebase UID. Sign out offline and relaunch; it must return to Login.
- With a disposable test account, have a project owner temporarily deny profile creation in a controlled Firebase test environment. Confirm registration does not route to Home/Admin, reports failure, and attempts to remove the newly created Auth account. If rollback itself fails, the app asks the user to contact support rather than claiming registration succeeded. Restore the test rules afterward.

These are recommended human runtime checks. The runner has not performed Firebase console or device actions.
