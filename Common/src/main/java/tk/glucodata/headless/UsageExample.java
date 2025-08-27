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
        
            if (!jugglucoManager.ensurePermissionsAndBluetooth(context)) {
                Toast.makeText(activity, "Bluetooth not available", Toast.LENGTH_LONG).show();
                return;
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
    

    
    public void startNfcScanning() {
        if (!initialized) return;
        if (jugglucoManager.isNfcScanning()) return;
        jugglucoManager.startNfcScanning();
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
    }
}
