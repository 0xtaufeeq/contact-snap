# ContactSnap

Point your camera at a business card (or any printed contact details), and
ContactSnap uses **Google Gemini** to read it and saves it straight to your
Android contacts — no typing.

## How it works

1. **Capture** — a custom CameraX viewfinder with a card-framing guide.
2. **Extract** — the image is sent to Gemini (`gemini-2.5-flash`), which returns
   structured contact fields as strict JSON (name, job title, company, phones,
   emails, websites, address). The model reads the whole card, so it handles
   messy / rotated / multi-language layouts far better than plain OCR.
3. **Review** — you confirm/edit every field on a clean Material 3 form.
4. **Save** — written directly into your contacts via the Contacts Provider.

You can also import an existing photo from the gallery instead of using the camera.

## API key

The app needs a **free Gemini API key** (Google AI Studio →
https://aistudio.google.com/app/apikey). Open the app, tap the **gear icon**,
paste the key, and **Save**. It's stored only on-device (DataStore). Scanning
requires internet.

## Tech stack

- Kotlin + Jetpack Compose (Material 3), Navigation-Compose
- CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`)
- Gemini `generateContent` REST API via OkHttp (response-schema JSON)
- DataStore (API key) · Coil (image preview)
- Min SDK 26 · Target/Compile SDK 35

## Project layout

```
app/src/main/java/com/contactsnap/app/
├── MainActivity.kt          # navigation, runtime permissions, gallery picker
├── ui/
│   ├── ScanViewModel.kt     # OCR → parse → save state machine
│   ├── theme/               # editorial Material 3 theme (Color/Type/Theme)
│   ├── components/           # reusable editable fields
│   └── screens/             # Home, Camera, Review, Success
├── ocr/OcrScanner.kt        # ML Kit wrapper (suspend)
├── parsing/ContactParser.kt # text → ParsedContact heuristics
├── contacts/ContactWriter.kt# batch insert into Contacts Provider
└── model/ParsedContact.kt
```

## Build & run

You need **Android Studio** (Ladybug or newer) with the Android SDK.

1. Open this folder in Android Studio (**File → Open**).
2. Let it sync — Android Studio downloads Gradle 8.11.1 (per
   `gradle/wrapper/gradle-wrapper.properties`) and generates the wrapper jar.
   > The `gradle-wrapper.jar` binary isn't checked in. If you build from the
   > command line first, run `gradle wrapper` once (with a local Gradle) to
   > generate it, or just let Android Studio's first sync handle it.
3. Connect a device (or start an emulator with a camera) and press **Run**.

### Permissions requested at runtime
- `CAMERA` — to capture the card.
- `WRITE_CONTACTS` — to save the parsed contact.
- `INTERNET` — to call the Gemini API.

## Releasing

The app supports a **signed, R8-shrunk release build** (~2.4 MB vs the ~60 MB debug build).

### Local
1. A keystore lives at `keystore/release.jks` and signing config in `keystore.properties`
   (both gitignored). **Change the placeholder passwords and regenerate the keystore
   before any real release** — and back the keystore up; losing it means you can't ship updates.
2. Build:
   ```
   ./gradlew assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`.

### CI (GitHub Actions)
`.github/workflows/release.yml` builds a signed APK and attaches it to a GitHub Release
on every `v*` tag. Add these repo secrets first (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | base64 of `keystore/release.jks` — generate with `base64 -w0 keystore/release.jks` (Linux/Git Bash) or `[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore/release.jks"))` (PowerShell) |
| `KEYSTORE_PASSWORD` | the store password |
| `KEY_ALIAS` | `contactsnap` (or your alias) |
| `KEY_PASSWORD` | the key password |

Then cut a release:
```
git tag v1.0.0
git push origin v1.0.0
```
The workflow builds `ContactSnap-v1.0.0.apk` and publishes it under Releases.

## Notes & next steps

- Extraction quality comes from the vision model. To change models, edit the
  `model` default in `GeminiContactExtractor` (e.g. `gemini-2.5-pro` for higher
  accuracy, `gemini-2.0-flash` for the widest free availability).
- The API key lives in app-private DataStore. For a shared build you could bake a
  default key via `BuildConfig` from `local.properties` instead.
- Contacts are written to the local/device account. To target a specific Google
  account, set `ACCOUNT_TYPE`/`ACCOUNT_NAME` in `ContactWriter`.
