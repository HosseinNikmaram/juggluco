package tk.glucodata.headless;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import android.util.Log;

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
        
            if (!jugglucoManager.ensurePermissionsAndBluetooth(context)) {
                Toast.makeText(activity, "Bluetooth not available", Toast.LENGTH_LONG).show();
                return;
            }


        jugglucoManager.setGlucoseListener((serial, mgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen) -> {
            String message = String.format("Glucose: %.1f mg/dL, Rate: %.1f", value, rate);
            Log.d(TAG, String.format(
                    "Glucose update - Serial: %s, mgdl: %b, Value: %.1f, Rate: %.1f, Alarm: %s, Time: %d, SensorStart: %d, SensorGen: %d",
                    serial, mgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen
            ));
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            });
            jugglucoManager.getGlucoseStats(serial,0L,System.currentTimeMillis());
            jugglucoManager.getAllGlucoseHistory(serial).forEach(glucoseData -> {
                String timeStr = sdf.format(new Date(glucoseData.timeMillis));
                Log.d(TAG, String.format(
                        "Time: %s, Glucose: %d mg/dL (%.1f mmol/L), Rate: %.3f, Alarm: %d",
                        timeStr,
                        glucoseData.mgdl,
                        glucoseData.mmolL
                ));
            });
            jugglucoManager.getSensorInfo(serial);
        });


        jugglucoManager.setStatsListener((serial, stats) -> {
            Log.d(TAG, "Stats for " + serial +
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
