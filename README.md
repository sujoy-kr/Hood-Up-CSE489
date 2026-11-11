# Hood Up

Hood Up is an Android car workshop booking app built for the **CSE489 course project**. Customers can browse workshops, book services, track appointments, and get service suggestions from the “Ask the Mechanic” chatbot. Admins manage workshops, services, and bookings.

## Built with

- **Java and XML layouts** in Android Studio
- **Firebase Authentication** for email/password login
- **Cloud Firestore** for online data
- **SQLite** for local storage and offline caching
- **Google Gemini API** with a free-tier API key for the chatbot (subject to availability and usage limits)
- Android services, background threads, broadcasts, and notifications

## Setup

1. Open the project in Android Studio. Install **Android SDK 37** and use a complete **JetBrains JDK 25** for Gradle. Let Android Studio create `local.properties` with your SDK path.
2. Create a Firebase project and register an Android app with package **`com.example.carworkshopbookingapp`**. Download its configuration to **`app/google-services.json`**.
3. Enable **Email/Password** authentication and create the default **Cloud Firestore** database. Publish `firestore.rules` and create the indexes in `firestore.indexes.json` in that project.
4. Get a Gemini API key from Google AI Studio and create **`.env` in the project root**, beside `gradlew`:

   ```dotenv
   GEMINI_API_KEY=
   ```

   Fill in your own key. Without a working key, the chatbot uses local fallback suggestions. Rebuild after changing it.

These local configuration files are ignored by Git. Keep your API key private.

## Run

Select an Android **7.0 / API 24 or newer** phone or emulator in Android Studio and click **Run**, or build the debug APK:

```sh
./gradlew assembleDebug
```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

Register a customer account in the app while online. For admin access, the Firebase project owner must verify that account and change its existing `users/{uid}.role` to `admin` in Firestore. Log out and use **Admin login**.

For a new database, use the admin dashboard to add services first, then workshops offering those services. Customers can then add a car and book an appointment. Allow notification permission when prompted to receive booking updates and reminders.
