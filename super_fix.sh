#!/bin/bash
echo "🚀 Starting Super Fix for Eduk Build..."

# 1. Ensure absolute SDK path
echo "sdk.dir=/home/codespace/android-sdk" > local.properties
export ANDROID_HOME=/home/codespace/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 2. Verify SDK components
echo "Checking SDK components..."
if [ ! -d "$ANDROID_HOME/build-tools/34.0.0" ]; then
    echo "Installing missing build-tools 34.0.0..."
    $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0" "platforms;android-34"
fi

# 3. Clean everything
echo "Cleaning project..."
./gradlew clean

# 4. Final build attempt
echo "Building APK..."
./gradlew assembleDebug
