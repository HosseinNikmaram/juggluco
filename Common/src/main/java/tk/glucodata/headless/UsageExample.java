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
    private boolean bleStarted = false;
    
    /**
     * Initialize the headless Juggluco system in your module
     * @param activity Your main activity
     */
    public void initializeJuggluco(Activity activity) {
        if (initialized) return;
        this.context = activity;
        
        HeadlessConfig.enableHeadlessNfc();
        HeadlessConfig.setBleEnabled(false);
        
        jugglucoManager = new HeadlessJugglucoManager();
        
        if (!jugglucoManager.init(activity)) {
            Toast.makeText(activity, "Failed to initialize Juggluco", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (HeadlessConfig.isBleEnabled()) {
            if (!jugglucoManager.ensurePermissionsAndBluetooth(context)) {
                Toast.makeText(activity, "Bluetooth not available", Toast.LENGTH_LONG).show();
                return;
            }
        }
        
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
    
    public void setBleEnabled(boolean enabled) {
        HeadlessConfig.setBleEnabled(enabled);
    }
    
    public void startNfcScanning() {
        if (!initialized) return;
        // Use real runtime state from the reader
        if (jugglucoManager.isNfcScanning()) return;
        jugglucoManager.startNfcScanning();
    }

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
        bleStarted = false;
        HeadlessConfig.disableHeadlessNfc();
    }
}
