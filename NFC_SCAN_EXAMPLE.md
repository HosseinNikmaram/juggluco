# NFC Scanning Integration with ScanResult in MainActivity

## Overview

This document shows how to integrate NFC scanning functionality with the new `ScanResult` class in your MainActivity. The integration provides a clean, structured way to handle NFC scan results and extract glucose values, sensor information, and status codes.

## Integration Steps

### 1. Update MainActivity to Implement NfcAdapter.ReaderCallback

```java
public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    private static final String TAG = "MainActivity";
    private NfcAdapter mNfcAdapter = null;
    private HeadlessJugglucoManager jugglucoManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize Juggluco manager
        jugglucoManager = HeadlessJugglucoManager.getInstance();
        jugglucoManager.init(this);
        
        // Set up NFC
        setupNfc();
    }
    
    private void setupNfc() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Log.e(TAG, "NFC not available on this device");
            return;
        }
        
        if (!mNfcAdapter.isEnabled()) {
            Log.w(TAG, "NFC is disabled");
            // Show dialog to enable NFC
            showEnableNfcDialog();
            return;
        }
        
        // Enable reader mode
        enableReaderMode();
    }
    
    private void enableReaderMode() {
        try {
            int flags = NfcAdapter.FLAG_READER_NFC_V | 
                       NfcAdapter.FLAG_READER_NFC_A | 
                       NfcAdapter.FLAG_READER_NFC_B | 
                       NfcAdapter.FLAG_READER_NFC_F | 
                       NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | 
                       NfcAdapter.FLAG_READER_NFC_BARCODE;
            
            mNfcAdapter.enableReaderMode(this, this, flags, null);
            Log.i(TAG, "NFC reader mode enabled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable NFC reader mode", e);
        }
    }
    
    private void showEnableNfcDialog() {
        new AlertDialog.Builder(this)
            .setTitle("NFC Required")
            .setMessage("NFC is required for scanning Libre sensors. Please enable NFC in settings.")
            .setPositiveButton("Settings", (dialog, which) -> {
                startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    @Override
    public void onTagDiscovered(Tag tag) {
        Log.i(TAG, "NFC tag discovered: " + bytesToHex(tag.getId()));
        
        // Process the tag using HeadlessJugglucoManager
        ScanResult result = jugglucoManager.scanNfcTag(tag);
        
        // Handle the scan result
        handleScanResult(result);
    }
    
    private void handleScanResult(ScanResult result) {
        if (result == null) {
            Log.e(TAG, "Scan result is null");
            return;
        }
        
        // Log the complete result
        Log.i(TAG, "=== NFC SCAN RESULT ===");
        Log.i(TAG, result.toString());
        
        // Update UI on main thread
        runOnUiThread(() -> {
            updateScanResultUI(result);
        });
        
        // Handle different result types
        if (result.isSuccess()) {
            if (result.hasGlucoseReading()) {
                handleGlucoseReading(result);
            } else {
                handleNonGlucoseResult(result);
            }
        } else {
            handleScanError(result);
        }
    }
    
    private void handleGlucoseReading(ScanResult result) {
        Log.i(TAG, "Glucose reading: " + result.getGlucoseValue() + " mg/dL");
        
        // Update glucose display
        updateGlucoseDisplay(result.getGlucoseValue());
        
        // Trigger glucose listener if available
        if (jugglucoManager.glucoseListener != null) {
            // Convert mg/dL to mmol/L if needed
            float mmolL = result.getGlucoseValue() / 18.0f;
            
            jugglucoManager.glucoseListener.onGlucoseReceived(
                result.getSerialNumber(),
                true, // mgdl
                mmolL,
                0.0f, // rate (not available from NFC)
                0,    // alarm (not available from NFC)
                System.currentTimeMillis(),
                0L,   // sensor start time
                0     // sensor generation
            );
        }
        
        // Get glucose stats
        jugglucoManager.getGlucoseStats(result.getSerialNumber());
        
        // Show success message
        showScanMessage("Glucose: " + result.getGlucoseValue() + " mg/dL", true);
    }
    
    private void handleNonGlucoseResult(ScanResult result) {
        String message = result.getMessage();
        Log.i(TAG, "Non-glucose result: " + message);
        
        switch (result.getReturnCode() & 0xFF) {
            case 3: // Sensor needs activation
                showScanMessage("Sensor needs activation", false);
                break;
            case 4: // Sensor ended
                showScanMessage("Sensor has ended", false);
                break;
            case 5: // New sensor
            case 7: // New sensor (V2)
                showScanMessage("New sensor detected", true);
                break;
            case 8: // Streaming enabled
            case 9: // Streaming already enabled
            case 0x85: // Streaming enabled (V2)
            case 0x87: // Streaming enabled (V2)
                showScanMessage("Streaming enabled", true);
                break;
            default:
                showScanMessage(message, result.isSuccess());
                break;
        }
    }
    
    private void handleScanError(ScanResult result) {
        Log.e(TAG, "Scan error: " + result.getMessage());
        
        String errorMessage = "Scan failed: " + result.getMessage();
        showScanMessage(errorMessage, false);
        
        // Handle specific error types
        switch (result.getReturnCode() & 0xFF) {
            case 17: // Read Tag Info Error
                Log.e(TAG, "Tag info read error - sensor might be damaged");
                break;
            case 18: // Read Tag Data Error
                Log.e(TAG, "Tag data read error - try again");
                break;
            case 19: // Unknown error
                Log.e(TAG, "Unknown error occurred");
                break;
        }
    }
    
    private void updateScanResultUI(ScanResult result) {
        // Update your UI elements here
        TextView resultText = findViewById(R.id.scan_result_text);
        if (resultText != null) {
            resultText.setText(result.getMessage());
        }
        
        // Update glucose value display
        if (result.hasGlucoseReading()) {
            TextView glucoseText = findViewById(R.id.glucose_value_text);
            if (glucoseText != null) {
                glucoseText.setText(result.getGlucoseValue() + " mg/dL");
            }
        }
        
        // Update sensor serial number
        TextView serialText = findViewById(R.id.sensor_serial_text);
        if (serialText != null) {
            serialText.setText(result.getSerialNumber());
        }
        
        // Update status indicator
        View statusIndicator = findViewById(R.id.status_indicator);
        if (statusIndicator != null) {
            int color = result.isSuccess() ? Color.GREEN : Color.RED;
            statusIndicator.setBackgroundColor(color);
        }
    }
    
    private void updateGlucoseDisplay(int glucoseValue) {
        // Update your glucose display UI
        TextView glucoseDisplay = findViewById(R.id.glucose_display);
        if (glucoseDisplay != null) {
            glucoseDisplay.setText(String.format("%d mg/dL", glucoseValue));
        }
        
        // You might also want to update a chart or graph
        updateGlucoseChart(glucoseValue);
    }
    
    private void updateGlucoseChart(int glucoseValue) {
        // Update your glucose chart/graph here
        // This depends on your charting library
    }
    
    private void showScanMessage(String message, boolean isSuccess) {
        int duration = isSuccess ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        Toast.makeText(this, message, duration).show();
        
        // You could also show a custom dialog or notification
        Log.i(TAG, "Scan message: " + message);
    }
    
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
            enableReaderMode();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(this);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (jugglucoManager != null) {
            jugglucoManager.cleanup();
        }
    }
}
```

### 2. Layout XML for Scan Results

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- Status Indicator -->
    <View
        android:id="@+id/status_indicator"
        android:layout_width="match_parent"
        android:layout_height="4dp"
        android:background="@color/status_neutral" />

    <!-- Scan Result -->
    <TextView
        android:id="@+id/scan_result_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="Ready to scan"
        android:textSize="18sp"
        android:textStyle="bold" />

    <!-- Glucose Value -->
    <TextView
        android:id="@+id/glucose_display"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="-- mg/dL"
        android:textSize="24sp"
        android:textStyle="bold"
        android:gravity="center" />

    <!-- Sensor Serial -->
    <TextView
        android:id="@+id/sensor_serial_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="Sensor: --"
        android:textSize="14sp" />

    <!-- Scan Button -->
    <Button
        android:id="@+id/scan_button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:text="Start NFC Scan"
        android:onClick="onScanButtonClick" />

</LinearLayout>
```

### 3. Button Click Handler

```java
public void onScanButtonClick(View view) {
    if (mNfcAdapter == null) {
        Toast.makeText(this, "NFC not available", Toast.LENGTH_LONG).show();
        return;
    }
    
    if (!mNfcAdapter.isEnabled()) {
        showEnableNfcDialog();
        return;
    }
    
    // The NFC scanning is handled automatically by the reader mode
    // Just show a message to the user
    Toast.makeText(this, "Hold your phone near a Libre sensor", Toast.LENGTH_LONG).show();
}
```

## Advanced Usage

### 1. Custom Scan Result Handler

```java
public interface ScanResultHandler {
    void onGlucoseReading(int glucoseValue, String serialNumber);
    void onSensorActivation(String serialNumber);
    void onSensorEnded(String serialNumber);
    void onNewSensor(String serialNumber);
    void onStreamingEnabled(String serialNumber);
    void onScanError(String error, int errorCode);
}

// In your MainActivity
private ScanResultHandler scanResultHandler = new ScanResultHandler() {
    @Override
    public void onGlucoseReading(int glucoseValue, String serialNumber) {
        // Handle glucose reading
        updateGlucoseDisplay(glucoseValue);
        saveGlucoseReading(glucoseValue, serialNumber);
    }
    
    @Override
    public void onSensorActivation(String serialNumber) {
        // Handle sensor activation
        showActivationDialog(serialNumber);
    }
    
    // ... implement other methods
};
```

### 2. Batch Processing Multiple Tags

```java
private List<ScanResult> scanResults = new ArrayList<>();

public void processMultipleTags(List<Tag> tags) {
    scanResults.clear();
    
    for (Tag tag : tags) {
        ScanResult result = jugglucoManager.scanNfcTag(tag);
        scanResults.add(result);
        
        // Process each result
        handleScanResult(result);
    }
    
    // Generate summary
    generateScanSummary();
}

private void generateScanSummary() {
    int successCount = 0;
    int glucoseReadings = 0;
    int totalGlucose = 0;
    
    for (ScanResult result : scanResults) {
        if (result.isSuccess()) {
            successCount++;
            if (result.hasGlucoseReading()) {
                glucoseReadings++;
                totalGlucose += result.getGlucoseValue();
            }
        }
    }
    
    String summary = String.format("Scanned %d tags, %d successful, %d glucose readings, avg: %.1f mg/dL",
        scanResults.size(), successCount, glucoseReadings,
        glucoseReadings > 0 ? (float) totalGlucose / glucoseReadings : 0.0f);
    
    Log.i(TAG, summary);
    Toast.makeText(this, summary, Toast.LENGTH_LONG).show();
}
```

### 3. Error Recovery and Retry

```java
private void handleScanWithRetry(Tag tag, int maxRetries) {
    int retryCount = 0;
    ScanResult result = null;
    
    while (retryCount < maxRetries && (result == null || !result.isSuccess())) {
        try {
            result = jugglucoManager.scanNfcTag(tag);
            retryCount++;
            
            if (!result.isSuccess()) {
                Log.w(TAG, "Scan attempt " + retryCount + " failed: " + result.getMessage());
                
                if (retryCount < maxRetries) {
                    // Wait before retry
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in scan attempt " + retryCount, e);
            retryCount++;
        }
    }
    
    if (result != null && result.isSuccess()) {
        Log.i(TAG, "Scan successful after " + retryCount + " attempts");
        handleScanResult(result);
    } else {
        Log.e(TAG, "All scan attempts failed");
        showScanMessage("Scan failed after " + maxRetries + " attempts", false);
    }
}
```

## Testing and Debugging

### 1. Enable Detailed Logging

```java
// In your MainActivity
private void enableDetailedLogging() {
    if (BuildConfig.DEBUG) {
        Log.d(TAG, "=== NFC SCAN DEBUG INFO ===");
        Log.d(TAG, "NFC Adapter: " + (mNfcAdapter != null ? "Available" : "Not available"));
        Log.d(TAG, "NFC Enabled: " + (mNfcAdapter != null && mNfcAdapter.isEnabled()));
        Log.d(TAG, "Juggluco Manager: " + (jugglucoManager != null ? "Initialized" : "Not initialized"));
    }
}
```

### 2. Test Different Sensor Types

```java
// Test with different sensor types
public void testSensorTypes() {
    // This would be called with actual NFC tags
    Log.i(TAG, "Testing different sensor types...");
    
    // You can simulate different scan results for testing
    ScanResult testResult = new ScanResult(true, 120, 0, "TEST123", "Test glucose reading");
    handleScanResult(testResult);
}
```

## Best Practices

1. **Always check NFC availability** before attempting to scan
2. **Handle errors gracefully** and provide user feedback
3. **Use runOnUiThread** for UI updates from NFC callbacks
4. **Implement proper cleanup** in onDestroy
5. **Log scan results** for debugging and monitoring
6. **Provide user feedback** for all scan outcomes
7. **Handle edge cases** like null tags or failed reads
8. **Implement retry logic** for transient failures

This integration provides a robust, user-friendly way to handle NFC scanning with comprehensive result handling and error management.