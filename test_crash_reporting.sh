#!/bin/bash

echo "=== Firebase Crashlytics Test Script ==="
echo "This script helps test the crash reporting functionality"
echo ""

echo "1. Building the project..."
./gradlew clean build

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "2. Install the app on a connected device/emulator:"
    echo "   ./gradlew installDebug"
    echo ""
    echo "3. To test crash reporting, you can:"
    echo "   a) Launch the CrashTestActivity from your app"
    echo "   b) Or add this code anywhere in your app:"
    echo "      Applic.logError(\"Test error message\");"
    echo ""
    echo "4. Check Firebase Console for crash reports:"
    echo "   https://console.firebase.google.com/"
    echo ""
    echo "5. View local logs:"
    echo "   adb logcat | grep -E \"(Firebase|Crashlytics|Applic)\""
    echo ""
    echo "Note: Make sure you have:"
    echo "   - Replaced Common/google-services.json with your actual Firebase config"
    echo "   - Internet connection for crash reports to be sent"
    echo "   - Firebase project properly configured"
else
    echo "❌ Build failed! Check the error messages above."
    exit 1
fi