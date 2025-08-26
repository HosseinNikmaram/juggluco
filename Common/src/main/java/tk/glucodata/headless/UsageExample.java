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
    private boolean initialized = false;
    private boolean nfcStarted = false;
    private boolean bleStarted = false;
    
    /**
     * Initialize the headless Juggluco system in your module
     * @param activity Your main activity
     */
    public void initializeJuggluco(Activity activity) {
        if (initialized) return;
        this.context = activity;
        
        // Enable headless NFC to avoid MainActivity NFC handling
        HeadlessConfig.enableHeadlessNfc();
        // Default: NFC-only unless caller enables BLE
        HeadlessConfig.setBleEnabled(false);
        
        // Create the headless manager
        jugglucoManager = new HeadlessJugglucoManager();
        
        // Initialize the system
        if (!jugglucoManager.init(activity)) {
            Toast.makeText(activity, "Failed to initialize Juggluco", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Check and enable Bluetooth (only if enabled)
        if (HeadlessConfig.isBleEnabled()) {
            if (!jugglucoManager.ensurePermissionsAndBluetooth(context)) {
                Toast.makeText(activity, "Bluetooth not available", Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        // Listeners
        jugglucoManager.setGlucoseListener((serial, mgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen) -> {
            String message = String.format("Glucose: %.1f mg/dL, Rate: %.1f", value, rate);
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        });
        jugglucoManager.setHistoryListener((serial, history) -> {
            Toast.makeText(activity, "Received " + history.length + " history points", Toast.LENGTH_SHORT).show();
        });
        jugglucoManager.setStatsListener((serial, stats) -> {
            Toast.makeText(activity, "Statistics available for " + serial, Toast.LENGTH_SHORT).show();
        });
        
        initialized = true;
        Toast.makeText(activity, "Juggluco initialized successfully", Toast.LENGTH_SHORT).show();
    }
    
    /** Enable or disable BLE functionality in headless mode. */
    public void setBleEnabled(boolean enabled) {
        HeadlessConfig.setBleEnabled(enabled);
    }
    
    /** Start NFC scanning for Libre sensor pairing */
    public void startNfcScanning() {
        if (!initialized) return;
        if (nfcStarted) return;
        jugglucoManager.startNfcScanning();
        nfcStarted = true;
    }

    /** Handle NFC tag discovery (call this from your NFC callback) */
    public void handleNfcTag(Tag tag) {
        if (jugglucoManager == null) return;
        HeadlessNfcScanner.ScanResult result = jugglucoManager.scanNfcTag(context, tag);
        if (result.success) {
            if (result.glucoseValue > 0) {
                String message = String.format("Glucose: %.1f mg/dL", (float) result.glucoseValue / 10.0f);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
            if (HeadlessConfig.isBleEnabled() && (result.returnCode == 5 || result.returnCode == 7)) {
                startBluetoothScanning();
            }
        } else {
            Toast.makeText(context, "Scan failed: " + result.message, Toast.LENGTH_SHORT).show();
        }
    }
    
    public void startBluetoothScanning() {
        if (jugglucoManager != null && HeadlessConfig.isBleEnabled()) {
            if (bleStarted) return;
            jugglucoManager.startBluetoothScanning();
            bleStarted = true;
            Toast.makeText(context, "Bluetooth scanning started", Toast.LENGTH_SHORT).show();
        }
    }
    
    public void stopBluetoothScanning() {
        if (jugglucoManager != null) {
            jugglucoManager.stopBluetoothScanning();
            bleStarted = false;
        }
    }
    
    public void getGlucoseHistory(String serial) {
        if (jugglucoManager != null) {
            jugglucoManager.getGlucoseHistory(serial);
        }
    }
    
    public void getGlucoseStats(String serial) {
        if (jugglucoManager != null) {
            jugglucoManager.getGlucoseStats(serial);
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
        nfcStarted = false;
        bleStarted = false;
        HeadlessConfig.disableHeadlessNfc();
    }
}
