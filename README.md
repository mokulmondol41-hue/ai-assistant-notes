<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# AI Assistant & Notes

An elegant AI assistant that helps you write, organize, and automatically summarize notes using Gemini.

This project builds with plain Gradle (no Android Studio required) and includes a GitHub Actions
workflow that automatically builds a debug APK on every push.

## Build with Gradle (command line)

**Prerequisites:** JDK 17 and the Android SDK command-line tools.

```bash
export GEMINI_API_KEY=your_gemini_api_key   # optional, can also be set later
./gradlew assembleDebug
```

The debug APK will be produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Build with GitHub Actions

Every push to this repository triggers `.github/workflows/build.yml`, which:
1. Sets up JDK 17 and the Android SDK
2. Runs `./gradlew assembleDebug`
3. Uploads the resulting APK as a workflow artifact named `app-debug-apk`

To let the CI build embed your Gemini API key, add a repository secret named `GEMINI_API_KEY`
(Settings → Secrets and variables → Actions → New repository secret).

## Open in Android Studio (optional)

You can still open this project in [Android Studio](https://developer.android.com/studio) if you prefer:
1. Open Android Studio, select **Open**, and choose this project's directory.
2. Set the `GEMINI_API_KEY` environment variable, or pass it as a Gradle property, before running.
3. Run the app on an emulator or physical device.
