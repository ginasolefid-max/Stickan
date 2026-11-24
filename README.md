# Stickman Archer: Zombie Siege (Android Kotlin + LibGDX)

Complete Android APK source code (`core/` and `android/`) plus an interactive web playable preview (`src/`).

## Build an Installable APK via GitHub Actions (No PC Required)
1. Push this repository to GitHub.
2. On GitHub.com, open your repository -> **Actions** -> **New workflow** -> **set up a workflow yourself**.
3. Copy the contents of `docs/android-apk-workflow.yml` into `.github/workflows/android-apk.yml` and commit.
4. Once the Action finishes, download the `stickman-archer-zombie-siege-debug` artifact zip, extract the `.apk`, and install it on your Android phone.

## Build a Signed Release APK in Android Studio
1. Open the repository root folder in **Android Studio**.
2. Wait for Gradle Sync (`build.gradle.kts`, `:core`, `:android`).
3. Go to **Build > Generate Signed Bundle / APK...** -> **APK** -> create/select a `.jks` keystore -> choose **release** -> **Create**.
