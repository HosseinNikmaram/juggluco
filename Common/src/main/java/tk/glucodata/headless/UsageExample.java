package tk.glucodata.headless;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import android.util.Log;

import java.util.List;

import tk.glucodata.ScanNfcV;

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
            boolean initialized = jugglucoManager.init(ctx);
            
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
                    public void onDeviceFound(String serialNumber, String deviceAddress) {
                        Log.i(TAG, "Device found: " + serialNumber + " at " + deviceAddress);
                        Toast.makeText(context, "Found device: " , Toast.LENGTH_SHORT).show();
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

                });
                
                // Set up NFC scan result callback
                ScanNfcV.setScanResultCallback(this::handleNfcScanResult);
                
                // Set up stats listener
                jugglucoManager.setStatsListener(new StatsListener() {
                    @Override
                    public void onStats(HeadlessStatsSummary stats) {
                        stats.toString();
                    }
                });

                jugglucoManager.setGlucoseListener((serial, mgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen) -> {
                    Log.d(TAG, String.format(
                            "Glucose update - Serial: %s, mgdl: %b, Value: %.1f, Rate: %.1f, Alarm: %s, Time: %d, SensorStart: %d, SensorGen: %d",
                            serial, mgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen
                    ));
                    jugglucoManager.getGlucoseStats();
                    jugglucoManager.getSensorInfo(serial);

                    // Example 3: Get complete glucose history (improved method)
                    List<HeadlessHistory.GlucoseData> completeHistory = HeadlessHistory.getCompleteGlucoseHistory();
                    Log.d(TAG, String.format("Complete history contains size: "+completeHistory.size()+" and last item: %s ", completeHistory.get(completeHistory.size() - 1).toString()));
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
            jugglucoManager.getGlucoseStats();
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

}
