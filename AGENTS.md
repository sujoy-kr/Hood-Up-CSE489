# Hood Up — project instructions

These rules apply to the entire project and all subsequent build tasks. Read `Project Info.txt` and inspect existing work before changing anything. Follow only the current numbered prompt; never advance automatically or redo completed work blindly.

## App and architecture

- Build **Hood Up**, a car workshop booking app for customers and admins.
- Write application code in **Java only**. Existing Kotlin DSL Gradle scripts are build configuration, not permission to add Kotlin application code.
- Keep namespace and application ID `com.example.carworkshopbookingapp`, with **minSdk 24**.
- Use plain Activities and explicit Intents for navigation. Do not use Jetpack Compose, the Navigation component, or a Fragments-heavy architecture.
- Allowed layout containers: `LinearLayout`, `RelativeLayout`, `FrameLayout`, `TableLayout`, `GridLayout`, and `ScrollView`. Use `ListView` and `Spinner` where appropriate, with ordinary Android widgets.
- Do not use Material Components, RecyclerView, CardView, ConstraintLayout, or other advanced layout systems. Build styled widgets with XML shapes and standard Android views.

## Required course features and ownership

Every feature must be functional and demonstrable in the app, not merely declared or included as a dependency.

| Requirement | Implementation and location |
| --- | --- |
| Activities + Intents + layouts | Customer and admin screens in `ui/`, using basic XML layouts in `res/layout/` |
| Local storage | SQLite helper and local persistence in `data/` |
| Remote storage | Firebase Authentication and Cloud Firestore repositories in `data/` |
| Threads | `ExecutorService` for background work and `Handler` for posting UI updates; shared helpers may live in `util/` |
| Service | A working Android Service in `service/` |
| Broadcasts | Both custom and system broadcasts, with receivers in `receiver/` |
| Notifications | Working notifications connected to app behavior, with helpers in `util/` as appropriate |
| Business model | Implement the business model in app behavior and UI; documentation alone is insufficient |
| AI integration | Gemini symptom-to-service chatbot, with integration code in `ai/` |

Never perform network or database work on the main thread, including SQLite operations. Manage executors, callbacks, receivers, and Service lifecycles appropriately. Keep UI updates on the main thread.

`Project Info.txt` also requires a distinct implementation, a functioning app on a mobile device, a marketing video, a zipped project, presentation slides with every member presenting, and a report covering motivation, challenges, benefits, implementation details, business model, and member contributions. Explain where, why, and how AI is used. The proposal requires a title, motivation, benefits/usage, and primary wireframes. These are overall course deliverables, not authorization to produce them outside the current prompt.

## Design system

Use **dark mode only**, including when the device is in light mode. Define the following palette in `app/src/main/res/values/colors.xml` and reference only these color names throughout app styling; do not introduce extra palette names or scatter literal colors through layouts, drawables, and Java.

| Name | Value |
| --- | --- |
| `bg` | `#0E1116` |
| `surface` | `#171B22` |
| `surface_alt` | `#212733` |
| `stroke` | `#2C3342` |
| `primary` | `#FF8A3D` |
| `primary_dark` | `#E5701F` |
| `accent` | `#2DD4BF` |
| `text` | `#ECEFF4` |
| `text_muted` | `#8B93A5` |
| `success` | `#4ADE80` |
| `warning` | `#FBBF24` |
| `danger` | `#FF5C6C` |

Use warm orange for primary actions and teal for accents. Create the card, button, chip, and input appearance with XML shape drawables: rounded **14dp** corners and **1dp** strokes. Use consistent **16dp** spacing and readable **16sp** body text. Do not use Material widgets or themes to achieve this appearance.

## Secrets

- Read `GEMINI_API_KEY` and `ADMIN_INVITE_CODE` from the project-root `.env` file at build time into corresponding `BuildConfig` fields.
- Never hardcode their values in source, resources, Gradle scripts, tests, documentation, or sample files. Never print them in logs, tool output, or responses, and never commit `.env`.
- If required credentials or external console setup are unavailable, stop and ask for the specific human action needed; do not invent credentials or claim the integration works.

## Code organization and quality

Under `app/src/main/java/com/example/carworkshopbookingapp/`, use:

- `ui/`: Activities.
- `data/`: models, SQLite helper, and Firestore repositories.
- `service/`: Android Services.
- `receiver/`: broadcast receivers.
- `ai/`: Gemini integration and chatbot logic.
- `util/`: focused shared helpers.

Keep classes small and responsibilities clear. Comment only non-obvious behavior. Leave no unused code or unnecessary dependencies.

## Existing starter and task boundaries

At Prompt 0 inspection, the starter uses a Java `MainActivity` in the base package, a ConstraintLayout screen, Material3 DayNight themes, Material and ConstraintLayout dependencies, and the original black/white palette. Firebase configuration includes the Google Services plugin and Analytics, but does not yet implement the required Auth and Firestore features. These starter choices do not override the rules above; migrate them during the appropriate later code tasks.

Prompt 0 changes **only this AGENTS.md**. Do not modify app code, resources, configuration, or credentials as part of Prompt 0.

## Verification and reporting

- After every later task that changes code or build configuration, run `./gradlew assembleDebug`. Resolve failures within the authorized scope before claiming success. Never claim a check passed without actually running it.
- Prompt 0 is documentation-only: inspect this file for completeness and accuracy; do not change application files or run a build that could require such changes. Report that the build was not run for this prompt.
- After each task, give exact device test steps for the changed functionality and distinguish automated checks from device tests still requiring a human. For Prompt 0, no device test is required for the documentation; a later app launch smoke test remains recommended.
- If credentials, decisions, permissions, console actions, device testing, or other human actions are required to complete the current task, stop and ask for the specific action. Do not claim success for unperformed required checks.
- When operating under the numbered prompt runner, return a single JSON object, without Markdown fences: `{"status":"success|needs_input|blocked","summary":"what happened and checks run","question":"specific input/action required, or empty string"}`. Use `success` only when the current prompt is complete and its available automated checks pass; use `needs_input` for required human steps and `blocked` for failures that cannot be resolved. Mention recommended device tests in the summary.
