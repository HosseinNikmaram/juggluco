# Firebase Crashlytics Setup Guide

This guide will help you set up Firebase Crashlytics for automatic crash reporting in your Juggluco Android app.

## Prerequisites

1. A Google account
2. Android Studio (recommended)
3. Your Android project

## Step 1: Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or "Add project"
3. Enter a project name (e.g., "Juggluco-Crash-Reporting")
4. Choose whether to enable Google Analytics (recommended)
5. Click "Create project"

## Step 2: Add Android App to Firebase

1. In your Firebase project, click the Android icon (+ Add app)
2. Enter your package name: `tk.glucodata`
3. Enter app nickname: "Juggluco"
4. Click "Register app"
5. Download the `google-services.json` file

## Step 3: Configure the Project

1. **Replace the template file**: 
   - Replace `Common/google-services.json` with your downloaded file
   - The template file contains placeholder values that need to be replaced

2. **Verify dependencies**: The following have been added to your project:
   - `Common/build.gradle`: Firebase Crashlytics dependencies
   - `build.gradle`: Firebase Crashlytics plugin
   - `Common/src/main/java/tk/glucodata/Applic.java`: Crash reporting initialization

## Step 4: Build and Test

1. Clean and rebuild your project:
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. Install the app on a device/emulator

3. **Test crash reporting** by adding this code anywhere in your app:
   ```java
   // Test crash reporting
   Applic.logError("Test error message");
   
   // Or force a crash (for testing only)
   throw new RuntimeException("Test crash for Firebase Crashlytics");
   ```

## Step 5: View Crash Reports

1. Go to your Firebase Console
2. Click on "Crashlytics" in the left sidebar
3. You should see crash reports appear within a few minutes

## Features Enabled

✅ **Automatic Crash Collection**: All uncaught exceptions are automatically reported
✅ **Custom Crash Keys**: App version, build type, target SDK, wearable status
✅ **Manual Error Logging**: Use `Applic.logError()` for non-fatal errors
✅ **Manual Crash Logging**: Use `Applic.logCrash()` for specific exceptions
✅ **Thread Information**: Crash reports include thread name and ID
✅ **Local Logging**: Falls back to local logging if Firebase fails

## Usage Examples

### Log a non-fatal error:
```java
Applic.logError("User attempted invalid operation: " + operation);
```

### Log a specific exception:
```java
try {
    // Some risky operation
} catch (Exception e) {
    Applic.logCrash(e, "Failed to perform operation: " + operation);
}
```

### Log a crash with context:
```java
try {
    // Some risky operation
} catch (Exception e) {
    Applic.logCrash(e, "Operation failed in " + context + " with data: " + data);
}
```

## Troubleshooting

### Build Errors
- Ensure you have the latest Google Services plugin
- Verify `google-services.json` is in the correct location
- Check that package name matches exactly

### No Crash Reports
- Verify internet connection
- Check Firebase Console for any setup issues
- Ensure the app has been launched at least once
- Check logcat for Firebase initialization errors

### Performance Issues
- Crashlytics has minimal performance impact
- Reports are sent in the background
- Only crash data is transmitted (no personal data)

## Security Notes

- Crash reports are sent to Google's servers
- No personal user data is included by default
- Crash reports help improve app stability
- Users can opt out in app settings if needed

## Next Steps

1. **Monitor crashes** in Firebase Console
2. **Set up alerts** for critical crashes
3. **Analyze crash patterns** to prioritize fixes
4. **Set up crash grouping** for better organization

## Support

If you encounter issues:
1. Check Firebase Console for error messages
2. Review Android logcat for initialization errors
3. Verify all dependencies are correctly added
4. Ensure `google-services.json` is properly configured

---

**Note**: This setup provides comprehensive crash reporting while maintaining the existing local logging functionality. All crashes will be automatically reported to Firebase Crashlytics for better monitoring and debugging.