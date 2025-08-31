package tk.glucodata.headless;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;
import android.util.Log;
import android.nfc.Tag;
import tk.glucodata.ScanNfcV;
import tk.glucodata.GlucoseCurve;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Example usage of the headless Juggluco system
 * This shows how to integrate Libre sensor functionality into your own module
 */
public class UsageExample {
    private static final String TAG = "JugglucoManager";
    private static volatile UsageExample instance;
    public static UsageExample getInstance() {
        if (instance == null) {
            synchronized (UsageExample.class) {
                if (instance == null) instance = new UsageExample();
            }
        }
        return instance;
    }
    
    private HeadlessJugglucoManager jugglucoManager;
    private Context context;
    private boolean initialized = false;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private UsageExample() {}
    
    /**
     * Initialize the headless Juggluco system in your module
     * @param activity Your main activity
     */
    public void initializeJuggluco(Activity activity) {
        if (initialized) return;
        this.context = activity;
        

        jugglucoManager = new HeadlessJugglucoManager();
        
        if (!jugglucoManager.init(activity)) {
            Toast.makeText(activity, "Failed to initialize Juggluco", Toast.LENGTH_LONG).show();
            return;
        }
        
            if (!jugglucoManager.ensurePermissionsAndBluetooth()) {
                Toast.makeText(activity, "Bluetooth not available", Toast.LENGTH_LONG).show();
                return;
            }

        // Set device connection listener for comprehensive Bluetooth monitoring
        jugglucoManager.setDeviceConnectionListener(new DeviceConnectionListener() {
            @Override
            public void onDeviceConnected(String serialNumber, String deviceAddress) {
                String message = String.format("Device connected: %s at %s", serialNumber, deviceAddress);
                Log.i(TAG, message);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onDeviceDisconnected(String serialNumber, String deviceAddress) {
                String message = String.format("Device disconnected: %s at %s", serialNumber, deviceAddress);
                Log.i(TAG, message);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onDeviceConnectionFailed(String serialNumber, String deviceAddress, int errorCode) {
                String message = String.format("Device connection failed: %s at %s (Error: %d)", serialNumber, deviceAddress, errorCode);
                Log.e(TAG, message);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onDevicePaired(String serialNumber, String deviceAddress) {
                String message = String.format("Device paired: %s at %s", serialNumber, deviceAddress);
                Log.i(TAG, message);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onDeviceUnpaired(String serialNumber, String deviceAddress) {
                String message = String.format("Device unpaired: %s at %s", serialNumber, deviceAddress);
                Log.i(TAG, message);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onDeviceFound(String serialNumber, String deviceAddress) {
                String message = String.format("Device found: %s at %s", serialNumber, deviceAddress);
                Log.i(TAG, message);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onBluetoothEnabled() {
                String message = "Bluetooth enabled";
                Log.i(TAG, message);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onBluetoothDisabled() {
                String message = "Bluetooth disabled";
                Log.i(TAG, message);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                });
            }
        });

        jugglucoManager.setGlucoseListener((serial, mgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen) -> {
            String message = String.format("Glucose: %.1f mg/dL, Rate: %.1f", value, rate);
            Log.d(TAG, String.format(
                    "Glucose update - Serial: %s, mgdl: %b, Value: %.1f, Rate: %.1f, Alarm: %s, Time: %d, SensorStart: %d, SensorGen: %d",
                    serial, mgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen
            ));
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            });
            jugglucoManager.getGlucoseStats(0L,System.currentTimeMillis());
            jugglucoManager.getSensorInfo(serial);

            // Example 3: Get complete glucose history (improved method)
            List<HeadlessHistory.GlucoseData> completeHistory = HeadlessHistory.getCompleteGlucoseHistory();
            Log.d(TAG, String.format("Complete history contains size: "+completeHistory.size()+" and last item: %s ", completeHistory.get(completeHistory.size() - 1).toString()));



        });


        jugglucoManager.setStatsListener(( stats) -> {
            Log.d(TAG, "Stats for "  +
                    ": n=" + stats.numberOfMeasurements +
                    ", avg=" + String.format("%.1f", stats.averageGlucose) +
                    ", sd=" + String.format("%.2f", stats.standardDeviation) +
                    ", gv%=" + String.format("%.1f", stats.glucoseVariabilityPercent) +
                    ", durDays=" + String.format("%.1f", stats.durationDays) +
                    ", active%=" + String.format("%.1f", stats.timeActivePercent) +
                    ", A1C%=" + (stats.estimatedA1CPercent==null ? "-" : String.format("%.2f", stats.estimatedA1CPercent)) +
                    ", GMI%=" + (stats.gmiPercent==null ? "-" : String.format("%.2f", stats.gmiPercent)) +
                    ", below%=" + String.format("%.1f", stats.percentBelow) +
                    ", inRange%=" + String.format("%.1f", stats.percentInRange) +
                    ", high%=" + String.format("%.1f", stats.percentHigh) +
                    ", veryHigh%=" + String.format("%.1f", stats.percentVeryHigh)
            );
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "Stats ready: n=" + stats.numberOfMeasurements,
                        Toast.LENGTH_SHORT).show();
            });
        });
        
        initialized = true;
        Toast.makeText(activity, "Juggluco initialized successfully", Toast.LENGTH_SHORT).show();
    }

    public void startBluetoothScanning() {
        if (jugglucoManager != null) {
            jugglucoManager.startBluetoothScanning();
            Toast.makeText(context, "Bluetooth scanning started", Toast.LENGTH_SHORT).show();
        }
    }
    
    public void stopBluetoothScanning() {
        if (jugglucoManager != null) {
            jugglucoManager.stopBluetoothScanning();
        }
    }

    
    public void getGlucoseStats() {
        if (jugglucoManager != null) {
            jugglucoManager.getGlucoseStats();
        }
    }
    
    public boolean isStreamingActive() {
        return jugglucoManager != null && jugglucoManager.isBluetoothStreamingActive();
    }
    
    public void cleanup() {
        if (jugglucoManager != null) {
            jugglucoManager.cleanup();
            jugglucoManager = null;
        }
        initialized = false;
    }

    public void startNfcScanning() {
        if (!initialized) return;
        
        // NFC scanning is handled by MainActivity and ScanNfcV
        // This method is kept for compatibility
        Toast.makeText(context, "NFC scanning is handled by MainActivity", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "NFC scanning request - scanning is handled by MainActivity and ScanNfcV");
    }
    
    /**
     * Process an NFC tag that was discovered using ScanNfcV
     * This method should be called from MainActivity's onTagDiscovered callback
     * @param tag The discovered NFC tag
     * @param curve The glucose curve context (can be null for headless mode)
     * @return ScanResult containing the scan information
     */
    public ScanResult processNfcTag(Tag tag, GlucoseCurve curve) {
        if (!initialized) {
            return new ScanResult(false, 0, 19, "", "Juggluco not initialized");
        }
        
        try {
            // Use ScanNfcV.scanWithResult for structured results
            ScanResult result = ScanNfcV.scanWithResult(curve, tag);
            
            // Log the scan result comprehensively
            logScanResult(result);
            
            // Show result to user
            showScanResultToUser(result);
            
            // Handle the result based on its type
            handleScanResult(result);
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing NFC tag", e);
            return new ScanResult(false, 0, 19, "", "Error processing tag: " + e.getMessage());
        }
    }
    
    /**
     * Process an NFC tag without GlucoseCurve context (for headless mode)
     * @param tag The discovered NFC tag
     * @return ScanResult containing the scan information
     */
    public ScanResult processNfcTag(Tag tag) {
        if (!initialized) {
            return new ScanResult(false, 0, 19, "", "Juggluco not initialized");
        }
        
        try {
            // For headless mode, we'll use the HeadlessJugglucoManager's scan method
            ScanResult result = jugglucoManager.scanNfcTag(tag);
            
            // Log the scan result comprehensively
            logScanResult(result);
            
            // Show result to user
            showScanResultToUser(result);
            
            // Handle the result based on its type
            handleScanResult(result);
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing NFC tag", e);
            return new ScanResult(false, 0, 19, "", "Error processing tag: " + e.getMessage());
        }
    }
    
    /**
     * Comprehensive logging of NFC scan results
     * @param result The scan result to log
     */
    private void logScanResult(ScanResult result) {
        if (result == null) {
            Log.e(TAG, "Scan result is null");
            return;
        }
        
        Log.i(TAG, "=== NFC SCAN RESULT LOG ===");
        Log.i(TAG, "Timestamp: " + System.currentTimeMillis());
        Log.i(TAG, "Success: " + result.isSuccess());
        Log.i(TAG, "Glucose Value: " + result.getGlucoseValue() + " mg/dL");
        Log.i(TAG, "Return Code: " + result.getReturnCode() + " (0x" + String.format("%02X", result.getReturnCode()) + ")");
        Log.i(TAG, "Return Code Description: " + result.getReturnCodeDescription());
        Log.i(TAG, "Serial Number: " + result.getSerialNumber());
        Log.i(TAG, "Message: " + result.getMessage());
        Log.i(TAG, "Has Glucose Reading: " + result.hasGlucoseReading());
        
        // Log additional context information
        if (result.hasGlucoseReading()) {
            float mmolL = result.getGlucoseValueFloat() / 18.0f;
            Log.i(TAG, "Glucose in mmol/L: " + String.format("%.2f", mmolL));
            
            // Categorize glucose level
            String category = categorizeGlucoseLevel(result.getGlucoseValue());
            Log.i(TAG, "Glucose Category: " + category);
        }
        
        // Log return code details
        int baseCode = result.getReturnCode() & 0xFF;
        Log.i(TAG, "Base Return Code: " + baseCode + " (0x" + String.format("%02X", baseCode) + ")");
        
        // Log flags if present
        if ((result.getReturnCode() & 0x80) != 0) {
            Log.i(TAG, "High Bit Flag Set: 0x80");
        }
        if ((result.getReturnCode() & 0x100) != 0) {
            Log.i(TAG, "Extended Flag Set: 0x100");
        }
        
        Log.i(TAG, "=== END NFC SCAN RESULT LOG ===");
    }
    
    /**
     * Categorize glucose level based on standard ranges
     * @param glucoseValue Glucose value in mg/dL
     * @return Category string
     */
    private String categorizeGlucoseLevel(int glucoseValue) {
        if (glucoseValue < 70) {
            return "Low (< 70 mg/dL)";
        } else if (glucoseValue <= 180) {
            return "Normal (70-180 mg/dL)";
        } else if (glucoseValue <= 250) {
            return "High (181-250 mg/dL)";
        } else {
            return "Very High (> 250 mg/dL)";
        }
    }
    
    /**
     * Show scan result to user via Toast
     * @param result The scan result to display
     */
    private void showScanResultToUser(ScanResult result) {
        if (result == null) return;
        
        String message = String.format("NFC Scan: %s", result.getMessage());
        if (result.isSuccess()) {
            if (result.hasGlucoseReading()) {
                message += String.format(" - Glucose: %d mg/dL", result.getGlucoseValue());
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Handle different scan results based on their type
     * @param result The scan result to process
     */
    public void handleScanResult(ScanResult result) {
        if (result == null) {
            Log.e(TAG, "Scan result is null");
            return;
        }
        
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
    
    /**
     * Handle successful glucose reading
     * @param result The scan result containing glucose data
     */
    private void handleGlucoseReading(ScanResult result) {
        Log.i(TAG, "=== PROCESSING GLUCOSE READING ===");
        Log.i(TAG, "Glucose reading: " + result.getGlucoseValue() + " mg/dL");
        Log.i(TAG, "Serial number: " + result.getSerialNumber());
        
        // Get glucose stats for this sensor
        if (jugglucoManager != null) {
            jugglucoManager.getGlucoseStats(result.getSerialNumber());
        }
        
        // Get sensor info
        if (jugglucoManager != null) {
            jugglucoManager.getSensorInfo(result.getSerialNumber());
        }
        
        Log.i(TAG, "=== END PROCESSING GLUCOSE READING ===");
    }
    
    /**
     * Handle successful scan without glucose reading
     * @param result The scan result to process
     */
    private void handleNonGlucoseResult(ScanResult result) {
        Log.i(TAG, "=== PROCESSING NON-GLUCOSE RESULT ===");
        String message = result.getMessage();
        Log.i(TAG, "Non-glucose result: " + message);
        
        switch (result.getReturnCode() & 0xFF) {
            case 3: // Sensor needs activation
                Log.i(TAG, "Sensor needs activation - " + result.getSerialNumber());
                break;
            case 4: // Sensor ended
                Log.i(TAG, "Sensor has ended - " + result.getSerialNumber());
                break;
            case 5: // New sensor
            case 7: // New sensor (V2)
                Log.i(TAG, "New sensor detected - " + result.getSerialNumber());
                break;
            case 8: // Streaming enabled
            case 9: // Streaming already enabled
            case 0x85: // Streaming enabled (V2)
            case 0x87: // Streaming enabled (V2)
                Log.i(TAG, "Streaming enabled - " + result.getSerialNumber());
                break;
            default:
                Log.i(TAG, "Other successful operation - " + result.getMessage());
                break;
        }
        Log.i(TAG, "=== END PROCESSING NON-GLUCOSE RESULT ===");
    }
    
    /**
     * Handle scan errors
     * @param result The scan result containing error information
     */
    private void handleScanError(ScanResult result) {
        Log.e(TAG, "=== PROCESSING SCAN ERROR ===");
        Log.e(TAG, "Scan error: " + result.getMessage());
        
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
            default:
                Log.e(TAG, "Unexpected error code: " + result.getReturnCode());
                break;
        }
        Log.e(TAG, "=== END PROCESSING SCAN ERROR ===");
    }
    
    /**
     * Simulate NFC scan for testing purposes
     * This method creates a test ScanResult for development and testing
     * @return Test ScanResult
     */
    public ScanResult simulateNfcScan() {
        Log.i(TAG, "=== SIMULATING NFC SCAN ===");
        
        // Create a test result
        ScanResult testResult = new ScanResult(true, 120, 0, "TEST123", "Test glucose reading");
        
        // Log the test result
        logScanResult(testResult);
        
        // Handle the test result
        handleScanResult(testResult);
        
        Log.i(TAG, "=== END SIMULATING NFC SCAN ===");
        
        return testResult;
    }

}
