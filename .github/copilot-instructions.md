# Eduk Project: AI Agent Mission Briefing

## Context
You are assisting in the development of **Eduk**, a next-gen Android parental control app. 
The core concept is "Learn First, Play Later". The app blocks entertainment apps and requires students to answer AI-generated questions to earn screen time.

## Current Technical Block: Manifest Merger & Kotlin 2.0
We are experiencing persistent failures in the `processDebugMainManifest` task and Kotlin compilation in GitHub Actions.

### What has been done:
1. **Kotlin 2.0 Migration**: Using `org.jetbrains.kotlin.plugin.compose` version `2.0.0`.
2. **Manifest Fixes**: 
   - Added `android:exported="true"` to all components.
   - Added `tools:replace="android:allowBackup,android:label,android:icon,android:roundIcon"` to resolve conflicts with `androidx.profileinstaller`.
   - Corrected class paths: `.ui.MainActivity`, `.service.AppMonitoringService`, `.service.EdukDeviceAdminReceiver`.
3. **Environment**: Using Java 17 and Android SDK 34.

### The Mission for Copilot:
1. **Debug the Manifest Merger**: Analyze the `app/build/outputs/logs/manifest-merger-debug-report.txt` (if available in the workspace) to find why the merger still fails or why attributes are clashing.
2. **Fix Kotlin References**: Ensure `MainActivity.kt` correctly references `OnboardingScreen`, `ParentDashboard`, and `QuestionScreen`. Check for duplicate declarations in `ParentDashboard.kt`.
3. **Verify Resources**: Ensure `@xml/accessibility_service_config` and `@xml/device_admin_rules` are perfectly formatted and matched in the Manifest.

## Project Structure
- `app/src/main/java/com/eduk/app/ui/`: UI Screens (Compose).
- `app/src/main/java/com/eduk/app/service/`: Accessibility and Device Admin services.
- `app/src/main/java/com/eduk/app/ai/`: Vision AI question generation logic.

## Goal
Generate a successful build (`./gradlew assembleDebug`) and produce the APK.
