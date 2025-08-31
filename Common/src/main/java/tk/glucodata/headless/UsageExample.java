package tk.glucodata.headless;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
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

}
