# Kasa (Yɛkasa) Voice Assistant

Kasa is an Android voice assistant tailored for disabled twi-speaking users. It provides wake-word detection, Twi speech recognition, English translation, intent classification, and contextual actions including phone calls, SMS, app launches, and live news briefings. The application features both a playful mascot interface and traditional controls.

---

## Feature Highlights

- **Voice Activation**: Always-listening wake word detection
- **Bi-directional Audio**: Speech-to-text and text-to-speech capabilities
- **Multi-language Support**: Twi ↔ English translation and processing
- **Smart Actions**: Phone calls, messaging, app launching, and news retrieval
- **Interactive UI**: Animated mascot experience with visual and audio feedback
- **Safety Features**: Confirmation dialogs and input validation for sensitive operations

---

## Project Structure

```
Kasa/
├── app/
│   ├── src/main/          # Core application code
│   ├── src/test/          # Unit tests
│   └── src/androidTest/   # Integration tests
├── build.gradle.kts       # Project configuration
├── gradle.properties
└── settings.gradle.kts
```

---

## Requirements

- Android Studio (AGP 8.0+)
- Android SDK API 26+ (minimum SDK 26, target SDK 34)
- JDK 11
- Internet connectivity for external services

---

## Setup

The application requires configuration for external services. Follow the build instructions below after setting up your development environment.

### Building and Running

```bash
# Clean and build the project
./gradlew clean
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

The default launcher opens the mascot interface. Use the control panel for direct access to features.

---

## Testing

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run integration tests (requires device)
./gradlew connectedDebugAndroidTest
```

---

## Security Features

- Network security with certificate validation
- Code obfuscation for production builds
- Data protection through backup exclusion rules
- Input sanitization and validation
- Secure configuration management

Production builds include comprehensive security measures. Always test release configurations thoroughly.

---

## Contributing

1. Fork the repository
2. Create a feature branch for your work
3. Add tests for new functionality
4. Submit a pull request with clear descriptions

---

## License

© 2025 Sedem Amediku. All rights reserved.

Proprietary software. No permission granted for use, modification, or distribution without explicit written consent from the author.

