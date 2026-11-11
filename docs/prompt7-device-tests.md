# Prompt 7 device checks

## Build and install

Outside the runner sandbox, run:

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest
```

Install the debug APK over the current app without clearing storage. To compile/run the Android JSON protocol tests on a connected test device or emulator, run:

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew connectedDebugAndroidTest
```

The runner has not performed a live Gemini call or device tests. `GeminiProtocolTest` uses the platform's real `org.json`; no extra production JSON/AI library was added. The root `.env` must contain the Gemini key for online answers; changing it requires a rebuild. Never paste the key into reports or logs.

Read `docs/AI_USAGE.md` for the feature's behavior, prompt, wire format, fallback and privacy details.

## Chat, history and lifecycle

1. As a customer, refresh Home so services are cached (seed the demo catalogue in a debug build if necessary). Open the mechanic-bot card. Confirm the title is **Ask the Mechanic**, the dark chat UI has the five quick-start chips, and the composer is reachable with the keyboard open.
2. Try all five quick starts. Each should create one right-aligned orange user bubble. While waiting, Send/quick starts must be disabled and “Checking under the hood…” must appear. The reply must be left-aligned, readable and brief.
3. Send whitespace only: no new bubble or request. Enter a normal symptom and double-tap Send: only one user turn/request should be created.
4. Try a vague description such as “It feels odd”. Expect one short follow-up question or a cautious reply without invented services. Answer the follow-up; the next request should use recent conversation context.
5. Leave/reopen chat, rotate during a request, background/return and relaunch the app. Persisted messages, fallback labels, urgency and suggestion cards must survive. A controller retains in-flight work across navigation/rotation without retaining the old Activity.
6. To test process death, end the process after the user turn is saved but before the reply is saved, then reopen. Retry should resume that user turn without duplicating it.
7. Confirm Clear chat, then reopen/relaunch. This account's conversation must be empty. Dismiss the confirmation once and verify history stays. Clear is disabled during a request, preventing a late reply from restoring a cleared conversation.
8. Switch customer accounts. Each must see only its own SQLite history. Do not expect clearing local history to delete provider-held request data or existing booking notes.

## Service suggestions to booking

1. Use “Strange noise when braking”, “AC not cooling”, “Car won't start”, “Steering pulls to one side” and “Warning light on” with the demo catalogue. Suggested service IDs must exist in that catalogue. Some descriptions legitimately ask for clarification instead.
2. Under a suggestion, check the name, cached BDT from-price and urgency pill. Prices must come from SQLite, never the model text.
3. Tap a card. Pick a workshop must contain only workshops whose `serviceIds` includes that service, with featured workshops first. Its row price is the selected service's price, not another service's cheaper price.
4. Select a workshop. Booking must receive the correct `workshopId`, `serviceId`, and the recent symptom text as an editable note (capped at 1000 characters). The chatbot must never create a reservation automatically.
5. Remove a suggested service or its workshop association in the test catalogue, refresh the picker, and confirm unavailable choices disappear instead of booking an invented ID.

## Fallback, errors and retry

1. Load services online, enable airplane mode, and send a symptom. Expect a useful local keyword-based answer labelled **offline suggestion**, a friendly in-chat explanation and Retry. SQLite work and the rest of the app must remain responsive while HTTP times out.
2. Reconnect and Retry. The failed bot reply should be replaced under the same user turn, with no duplicate user bubble. Repeat once and inspect `chat_history` to confirm stable reply IDs on retry.
3. In a temporary local test build, leave `GEMINI_API_KEY` empty, rebuild and send. It must use local advice rather than crash. Restore the key and rebuild afterward; do not commit secrets.
4. If the provider returns 429, invalid credentials, unsupported/unavailable model, malformed/blocked/truncated output or an unknown service ID, the same fallback/error/Retry path should apply. To exercise the model-unavailable case without relying on provider downtime, use a disposable local build with an intentionally nonexistent `GeminiClient.MODEL`, then restore the required default. The message must clearly name the unavailable model and the constant to change. No automatic model switching should occur.
5. With an empty service cache, send a symptom. It must not invent a service or price, and should ask the user to refresh the catalogue and retry.
6. Simulate a persistence failure only in a disposable test environment. A failed save should offer Retry/Clear rather than silently claiming durable history or duplicating an API call merely to retry a reply save.

## Urgency and privacy

1. Try “My brakes failed”, “There is smoke from the bonnet”, “I smell fuel” and “The engine is overheating”. Both online and fallback paths must show HIGH urgency and advice to stop driving as soon as safe and arrange help/inspection. A later vague follow-up should keep recent dangerous context in view.
2. Ordinary braking noise or weak AC must not automatically be treated as confirmed brake failure. Advice should remain cautious, not a diagnosis or a promise that a specific repair will fix it.
3. Use the instrumentation tests to verify malformed JSON, invalid field types, unknown IDs, price hallucinations, history length, schema fields and privacy redaction. These tests do not contact Gemini.
4. In an authorized test-only request inspector, verify the request body contains only recent redacted user/assistant text plus catalogue ID/name/category. It must not contain account UID/name/email/phone, Firebase tokens, car records, booking records, service prices, catalogue descriptions or keywords. Do not log or capture the API-key header.
5. Type a test email and phone into a disposable symptom. The outbound body should replace obvious email/phone patterns, while local history keeps the text the user typed. Arbitrary typed names cannot be reliably detected; the UI asks users to avoid personal details.
6. Inspect `chat_history`: persisted metadata should reconstruct cards and urgency after restart. Existing payload rows without the new fields should still load with defaults; no database-table migration is needed for the added JSON fields.

Check landscape, large text, system light/dark modes, scrolling back through older messages, and keyboard insets. The UI must remain dark and controls reachable. Live provider, error/rate-limit and network behavior still require these human runtime checks.

## Build troubleshooting: configuration-cache / missing jlink

The configuration-cache report identified an underlying `androidJdkImage` failure: Gradle selected VS Code's embedded Java 25 runtime, which has no `bin/jlink`. The project daemon criteria now require JetBrains Java 25, matching the installed complete Android Studio JDK. This keeps the existing JVM version requirement and excludes that language-server runtime. Old download URLs for a different runtime were removed.

Gradle's [daemon JVM criteria take precedence over JAVA_HOME](https://docs.gradle.org/current/userguide/gradle_daemon.html). On this machine, `/opt/android-studio/jbr` contains both `javac` and `jlink`. Retry with:

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest --no-daemon
```

A configuration-cache warning alone does not establish build success. Check the final BUILD SUCCESSFUL or BUILD FAILED output, including the underlying failure if one remains. Earlier restricted runs could not start Gradle. After unrestricted execution became available, the full debug build and all 21 JVM tests passed; see build/prompt7-build.log. No device was connected for runtime tests.

If the same provider/cache summary persists, configuration caching is now disabled in `gradle.properties` to expose the actual task failure directly. Ordinary up-to-date checks remain enabled. Stop old daemons and run a fresh build:

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew --stop
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest --no-daemon --no-configuration-cache --stacktrace
```

Share the final failure's `Caused by` lines if this fails; the provider serialization summary alone is not enough to diagnose a new failure. The existing report may still describe the old VS Code runtime rather than a new build.
