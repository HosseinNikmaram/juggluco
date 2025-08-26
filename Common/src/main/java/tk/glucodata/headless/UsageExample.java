package tk.glucodata.headless;

import android.app.Activity;
import android.content.Context;
import android.nfc.Tag;
import android.widget.Toast;

/**
 * Example usage of the headless Juggluco system
 * This shows how to integrate Libre sensor functionality into your own module
 */
public class UsageExample {
    
    private HeadlessJugglucoManager jugglucoManager;
    private Context context;
    
    /**
     * Initialize the headless Juggluco system in your module
     * @param activity Your main activity
     */
    public void initializeJuggluco(Activity activity) {
        this.context = activity;
        
        // Enable headless NFC to avoid MainActivity NFC handling
        HeadlessConfig.enableHeadlessNfc();
        
        // Create the headless manager
        jugglucoManager = new HeadlessJugglucoManager();
        
        // Initialize the system
        if (!jugglucoManager.init(activity)) {
            Toast.makeText(activity, "Failed to initialize Juggluco", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Check and enable Bluetooth
        if (!jugglucoManager.ensurePermissionsAndBluetooth(context)) {
            Toast.makeText(activity, "Bluetooth not available", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Set up glucose listener for real-time updates
        jugglucoManager.setGlucoseListener((serial, mgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen) -> {
            // Handle real-time glucose data
            String message = String.format("Glucose: %.1f mg/dL, Rate: %.1f", value, rate);
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            
            // You can also store this data in your own database
            // saveGlucoseData(serial, mgdl, value, rate, alarm, timeMillis);
        });
        
        // Set up history listener
        jugglucoManager.setHistoryListener((serial, history) -> {
            // Handle glucose history data
            Toast.makeText(activity, "Received " + history.length + " history points", Toast.LENGTH_SHORT).show();
            
            // Process history data
            for (long[] point : history) {
                long time = point[0];
                long mgdl = point[1];
                // Process each history point
            }
        });
        
        // Set up stats listener
        jugglucoManager.setStatsListener((serial, stats) -> {
            // Handle glucose statistics
            Toast.makeText(activity, "Statistics available for " + serial, Toast.LENGTH_SHORT).show();
        });
        
        Toast.makeText(activity, "Juggluco initialized successfully", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Start NFC scanning for Libre sensor pairing
     */
    public void startNfcScanning() {
        if (jugglucoManager == null) {
            Toast.makeText(context, "Juggluco not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        jugglucoManager.startNfcScanning();
    }

    
    /**
     * Handle NFC tag discovery (call this from your NFC callback)
     * @param tag NFC tag from onTagDiscovered
     */
    public void handleNfcTag(Tag tag) {
        if (jugglucoManager == null) return;
        
        HeadlessNfcScanner.ScanResult result = jugglucoManager.scanNfcTag(context, tag);
        
        if (result.success) {
            // Handle successful scan
            if (result.glucoseValue > 0) {
                String message = String.format("Glucose: %.1f mg/dL", (float) result.glucoseValue / 10.0f);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
            
            // If this is a new sensor, you might want to start Bluetooth scanning
            if (result.returnCode == 5 || result.returnCode == 7) {
                startBluetoothScanning();
            }
        } else {
            // Handle failed scan
            Toast.makeText(context, "Scan failed: " + result.message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Start Bluetooth scanning for paired sensors
     */
    public void startBluetoothScanning() {
        if (jugglucoManager != null) {
            jugglucoManager.startBluetoothScanning();
            Toast.makeText(context, "Bluetooth scanning started", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Stop Bluetooth scanning
     */
    public void stopBluetoothScanning() {
        if (jugglucoManager != null) {
            jugglucoManager.stopBluetoothScanning();
        }
    }
    
    /**
     * Get glucose history for a sensor
     * @param serial Sensor serial number
     */
    public void getGlucoseHistory(String serial) {
        if (jugglucoManager != null) {
            jugglucoManager.getGlucoseHistory(serial);
        }
    }
    
    /**
     * Get glucose statistics for a sensor
     * @param serial Sensor serial number
     */
    public void getGlucoseStats(String serial) {
        if (jugglucoManager != null) {
            jugglucoManager.getGlucoseStats(serial);
        }
    }
    
    /**
     * Check if Bluetooth streaming is active
     * @return true if streaming is active
     */
    public boolean isStreamingActive() {
        return jugglucoManager != null && jugglucoManager.isBluetoothStreamingActive();
    }
    
    /**
     * Clean up resources when your module is done
     */
    public void cleanup() {
        if (jugglucoManager != null) {
            jugglucoManager.cleanup();
            jugglucoManager = null;
        }
        HeadlessConfig.disableHeadlessNfc();
    }
    
    // Example of how to implement NFC callback in your activity
    /*
    public class MyActivity extends Activity implements NfcAdapter.ReaderCallback {
        private UsageExample jugglucoExample;
        
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            // Initialize Juggluco
            jugglucoExample = new UsageExample();
            jugglucoExample.initializeJuggluco(this);
        }
        
        @Override
        public void onTagDiscovered(Tag tag) {
            // Handle NFC tag discovery
            jugglucoExample.handleNfcTag(tag);
        }
        
        @Override
        protected void onResume() {
            super.onResume();
            // Start NFC scanning
            jugglucoExample.startNfcScanning();
        }
        
        @Override
        protected void onPause() {
            super.onPause();
            // Stop NFC scanning
            jugglucoExample.stopNfcScanning();
        }
        
        @Override
        protected void onDestroy() {
            super.onDestroy();
            // Clean up
            jugglucoExample.cleanup();
        }
    }
    */
}
