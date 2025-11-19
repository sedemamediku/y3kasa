# Kasa (Yɛkasa) Voice Assistant

Kasa is an Android voice assistant tailored for Ghanaian users. It keeps a wake-word service running in the background, captures audio, sends it to the Ghana NLP stack for speech-to-text and text-to-speech, translates Twi to English with OpenAI, classifies the intent, and performs contextual actions (phone calls, SMS, app launches, or live news briefings). A mascot experience (`MascotActivity`) provides a playful, animated surface for the assistant while `MainActivity` exposes a more traditional control panel.

---

## Feature Highlights

- **Always-on wake word** using Picovoice Porcupine (`WakeWordService`) with local `.ppn` assets.
- **Bi-directional audio pipeline** managed by `AudioRecorder`, `AudioPlayer`, `AudioTranscriber`, `TextTranslator`, and `TextToSpeech`.
- **Intent classification** powered by OpenAI GPT-4o to route requests to:
  - `PhoneCallHandler` – fuzzy contact matching + confirmation dialog before dialing.
  - `TextMessageHandler` – confirm-and-send SMS with validation and audible confirmations.
  - `AppOpener` – launch vetted third-party apps.
  - `NewsHandler` – live news summaries (OpenAI Search + optional cleanup + TTS playback).
- **Animated mascot UI** (`MascotActivity`) with Glide GIFs, floating animations, and audio prompts.
- **Safety-first UX** through confirmations, phone number/message validation, and quick cancel affordances.

---

## Project Layout

```
Kasa/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/Kasa/…     # Core source (activities, services, handlers)
│   │   │   ├── res/                        # Layouts, drawables, audio prompts, backup rules
│   │   │   └── assets/porcupine/           # Wake-word models (.ppn)
│   │   ├── test/                           # JVM unit tests
│   │   └── androidTest/                    # Instrumented tests
│   ├── build.gradle.kts                    # Module configuration
│   └── proguard-rules.pro                  # Release obfuscation rules
├── gradle/                                 # Version catalog + wrapper
├── build.gradle.kts                        # Root build
├── settings.gradle.kts
├── gradle.properties
├── notes.txt                               # Personal notes (ignored by git)
└── README.md
```

---

## Requirements

- Android Studio Iguana (AGP 8.7.x) or newer
- JDK 11 (matching the `compileOptions` in `app/build.gradle.kts`)
- Android SDK 26+ (minSdk 26, target/compile 34)
- Access to the external APIs listed below

---

## Environment Variables

The app will refuse to build/run without the following **environment variables**. Set them globally or per run configuration before building:

| Variable | Purpose | Where it is used |
|----------|---------|------------------|
| `OPENAI_API_KEY` | GPT-4o chat + search (translation, news, text cleanup) | `TextTranslator`, `NewsHandler`, `TextCleaner` |
| `PORCUPINE_ACCESS_KEY` | Wake-word detection license | `WakeWordService` |
| `GHANA_NLP_API_KEY` | Ghana NLP ASR + TTS services | `AudioTranscriber`, `TextToSpeech` |

### macOS / Linux
```bash
export OPENAI_API_KEY="your-key"
export PORCUPINE_ACCESS_KEY="your-key"
export GHANA_NLP_API_KEY="your-key"
```

### Windows PowerShell
```powershell
$env:OPENAI_API_KEY="your-key"
$env:PORCUPINE_ACCESS_KEY="your-key"
$env:GHANA_NLP_API_KEY="your-key"
```

### Android Studio
1. Run ▸ **Edit Configurations**
2. Add each variable under **Environment variables**
3. Apply and relaunch the run configuration

> **Never commit API keys**. `.gitignore` already excludes `.env`, `notes.txt`, and common secrets—keep it that way.

---

## Building & Running

```bash
# From the repo root
./gradlew clean            # optional but recommended
./gradlew assembleDebug    # build debug APK
./gradlew installDebug     # deploy to a connected device/emulator
```

Launch `MascotActivity` (default launcher) to experience the mascot UI, or use `MainActivity` for direct controls.

---

## Testing

```bash
./gradlew testDebugUnitTest            # JVM unit tests in app/src/test
./gradlew connectedDebugAndroidTest    # Instrumented tests (requires device/emulator)
```

The `test-output.txt` file is ignored to keep local run logs out of version control.

---

## Security Hardening

### Certificate Pinning

`OkHttpSingleton` pins every HTTPS call to OpenAI and Ghana NLP. **Replace the placeholder pins** with the real SHA-256 public key hashes before shipping:

```java
private static final CertificatePinner CERTIFICATE_PINNER =
    new CertificatePinner.Builder()
        .add("api.openai.com", "sha256/YOUR_REAL_PIN")
        .add("translation-api.ghananlp.org", "sha256/YOUR_REAL_PIN")
        .build();
```

Generate pins with OpenSSL:

```bash
openssl s_client -servername api.openai.com -connect api.openai.com:443 < /dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

Keep backup pins during certificate rotations and test networking after every update. Removing the pinner should only be a temporary debug step.

### ProGuard / R8

Release builds enable minification (`isMinifyEnabled = true`) with custom rules in `app/proguard-rules.pro`:
- Keeps `BuildConfig` so API keys injected at build time remain accessible.
- Preserves OkHttp, Porcupine, Glide, JSON, Parcelable, Serializable, and custom view classes.

Test your release build regularly:

```bash
./gradlew assembleRelease
```

### Backup Rules

`app/src/main/res/xml/backup_rules.xml` excludes cache, external storage, and `no_backup/` directories from Android Auto Backup so raw audio, cache files, and temporary blobs never leave the device.

### Input Validation

Phone numbers and SMS bodies are validated before sending:
- Regex + `PhoneNumberUtils` enforce length, format, and safe URI construction.
- Messages longer than 1000 chars are rejected.
- Confirmation dialogs provide a manual stop before any outbound call/SMS.

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `Cannot find symbol: BuildConfig` | Ensure `buildFeatures { buildConfig = true }` is enabled (already set) and run `./gradlew clean build`. |
| Network calls fail after enabling pinning | Confirm the pins match the server’s current certs. Update both pins when certificates rotate. |
| ProGuard/R8 crash at runtime | Add keep rules for any class mentioned in stack traces, then rebuild release. |
| Environment variables not found | Confirm they’re set in your shell *before* launching Android Studio, or add them per run configuration. |

---

## Contributing

1. Fork the repo and create a feature branch.
2. Follow the existing Java/Kotlin style (Java 11 in this project).
3. Add or update unit tests whenever possible.
4. Submit a pull request describing the change and any new configuration requirements.

---

## License

© 2025 Sedem Amediku. All rights reserved. No permission is granted to use, copy, modify, or distribute this software without explicit written consent from the author.

