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
 * Example implementation of the headless Juggluco system
 * Demonstrates NFC scanning, BLE management, and glucose data access
 */
public class UsageExample {
    private static final String TAG = "JugglucoManager";
    private static volatile UsageExample instance;
    public static UsageExample getInstance() {
        if (instance == null) {
            synchronized (UsageExample.class) {
                if (instance == null) {
                    instance = new UsageExample();
                }
            }
        }
        return instance;
    }
    
    private UsageExample() {}
    
    private Context context;
    private HeadlessJugglucoManager jugglucoManager;
    private boolean initialized = false;
    
    /**
     * Initialize the headless Juggluco system
     * @param ctx Android context
     * @return true if initialization was successful
     */
    public boolean init(Context ctx) {
        try {
            this.context = ctx;
            
            // Initialize Juggluco manager
            jugglucoManager = HeadlessJugglucoManager.getInstance();
            boolean initialized = jugglucoManager.init((Activity) ctx);
            
            if (initialized) {
                // Set up device connection listener
                jugglucoManager.setDeviceConnectionListener(new DeviceConnectionListener() {
                    @Override
                    public void onDeviceConnected(String serialNumber, String deviceAddress) {
                        Log.i(TAG, "Device connected: " + serialNumber + " at " + deviceAddress);
                        Toast.makeText(context, "Device connected: " + serialNumber, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onDeviceDisconnected(String serialNumber, String deviceAddress) {
                        Log.i(TAG, "Device disconnected: " + serialNumber + " from " + deviceAddress);
                        Toast.makeText(context, "Device disconnected: " + serialNumber, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onDeviceConnecting(String serialNumber, String deviceAddress) {
                        Log.i(TAG, "Device connecting: " + serialNumber + " to " + deviceAddress);
                        Toast.makeText(context, "Connecting to " + serialNumber, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onDeviceConnectionFailed(String serialNumber, String deviceAddress, int errorCode) {
                        Log.e(TAG, "Device connection failed: " + serialNumber + " error: " + errorCode);
                        Toast.makeText(context, "Connection failed: " + serialNumber, Toast.LENGTH_LONG).show();
                    }
                    
                    @Override
                    public void onDevicePaired(String serialNumber, String deviceAddress) {
                        Log.i(TAG, "Device paired: " + serialNumber + " at " + deviceAddress);
                        Toast.makeText(context, "Device paired: " + serialNumber, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onDeviceUnpaired(String serialNumber, String deviceAddress) {
                        Log.i(TAG, "Device unpaired: " + serialNumber + " from " + deviceAddress);
                        Toast.makeText(context, "Device unpaired: " + serialNumber, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onDevicePairing(String serialNumber, String deviceAddress) {
                        Log.i(TAG, "Device pairing: " + serialNumber + " at " + deviceAddress);
                        Toast.makeText(context, "Pairing with " + serialNumber, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onScanStarted() {
                        Log.i(TAG, "Bluetooth scan started");
                        Toast.makeText(context, "Bluetooth scan started", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onScanStopped() {
                        Log.i(TAG, "Bluetooth scan stopped");
                        Toast.makeText(context, "Bluetooth scan stopped", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onDeviceFound(String serialNumber, String deviceAddress, String deviceName) {
                        Log.i(TAG, "Device found: " + serialNumber + " (" + deviceName + ") at " + deviceAddress);
                        Toast.makeText(context, "Found device: " + deviceName, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onBluetoothEnabled() {
                        Log.i(TAG, "Bluetooth enabled");
                        Toast.makeText(context, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onBluetoothDisabled() {
                        Log.i(TAG, "Bluetooth disabled");
                        Toast.makeText(context, "Bluetooth disabled", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onConnectionPriorityChanged(String serialNumber, String deviceAddress, int priority) {
                        Log.i(TAG, "Connection priority changed: " + serialNumber + " priority: " + priority);
                    }
                    
                    @Override
                    public void onConnectionUpdated(String serialNumber, String deviceAddress, int interval, int latency, int timeout) {
                        Log.i(TAG, "Connection updated: " + serialNumber + " interval: " + interval + " latency: " + latency + " timeout: " + timeout);
                    }
                });
                
                // Set up NFC scan result callback
                ScanNfcV.setScanResultCallback(this::handleNfcScanResult);
                
                // Set up stats listener
                jugglucoManager.setStatsListener(new StatsListener() {
                    @Override
                    public void onStatsReady(HeadlessStats.Stats stats) {
                        Log.i(TAG, "=== GLUCOSE STATISTICS ===");
                        Log.i(TAG, "Sensor: " + stats.serial);
                        Log.i(TAG, "Time Range: " + new Date(stats.startMillis) + " to " + new Date(stats.endMillis));
                        Log.i(TAG, "Total Readings: " + stats.totalReadings);
                        Log.i(TAG, "Mean: " + String.format("%.1f", stats.mean) + " mg/dL");
                        Log.i(TAG, "Standard Deviation: " + String.format("%.1f", stats.stdDev) + " mg/dL");
                        Log.i(TAG, "Time in Range: " + String.format("%.1f", stats.timeInRange) + "%");
                        Log.i(TAG, "Below Range: " + String.format("%.1f", stats.belowRange) + "%");
                        Log.i(TAG, "Above Range: " + String.format("%.1f", stats.aboveRange) + "%");
                        Log.i(TAG, "Very High: " + String.format("%.1f", stats.veryHigh) + "%");
                        Log.i(TAG, "=== END STATISTICS ===");
                        
                        Toast.makeText(context, "Stats: " + String.format("%.1f", stats.timeInRange) + "% in range", Toast.LENGTH_LONG).show();
                    }
                });
                
                this.initialized = true;
                Log.i(TAG, "UsageExample initialized successfully");
                return true;
            } else {
                Log.e(TAG, "Failed to initialize Juggluco manager");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UsageExample", e);
            return false;
        }
    }
    
    /**
     * Callback method for NFC scan results
     * This method is called automatically when NFC scan completes
     * @param result The scan result from ScanNfcV
     */
    public void handleNfcScanResult(ScanResult result) {
        if (result == null) {
            Log.e(TAG, "Received null scan result");
            return;
        }
        
        // Log the scan result comprehensively
        logScanResult(result);
        
        // Show result to user
        showScanResultToUser(result);
        
        // Handle the result based on its type
        handleScanResult(result);
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
