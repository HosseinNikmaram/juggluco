package tk.glucodata.headless;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
            
            // Example 2: Using HeadlessHistory to get latest reading
            HeadlessHistory.GlucoseData latestReading = HeadlessHistory.getLatestGlucoseReading(serial);
            if (latestReading != null) {
                String latestTimeStr = sdf.format(new Date(latestReading.timeMillis));
                Log.d(TAG, String.format(
                        "Latest reading - Time: %s, Glucose: %d mg/dL (%.1f mmol/L)",
                        latestTimeStr, latestReading.mgdl, latestReading.mmolL
                ));
            }

            // Example 3: Get complete glucose history (improved method)
            List<HeadlessHistory.GlucoseData> completeHistory = HeadlessHistory.getCompleteGlucoseHistory(serial);
            Log.d(TAG, String.format("Complete history contains %d readings", completeHistory.size()));
            
            // Example 4: Get history as flat array (most efficient for bulk operations)
            long[] flatHistory = HeadlessHistory.getGlucoseHistoryFlat();
            if (flatHistory != null) {
                int numReadings = flatHistory.length / 2;
                Log.d(TAG, String.format("Flat history contains %d readings", numReadings));
                
                // Show first few readings
                for (int i = 0; i < Math.min(3, numReadings); i++) {
                    long timeSeconds = flatHistory[i * 2];
                    long packedGlucose = flatHistory[i * 2 + 1];
                    double mmolL = (double) packedGlucose / 4294967296.0;
                    int mgdl = (int) Math.round(mmolL * 18.0);
                    String timeStr = sdf.format(new Date(timeSeconds * 1000L));
                    Log.d(TAG, String.format("Reading %d: Time: %s, Glucose: %d mg/dL", 
                            i + 1, timeStr, mgdl));
                }
            }

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
    
    /**
     * Get complete glucose history for a sensor
     * This is the recommended way to get all glucose data
     * @param serial Sensor serial number
     * @return List of GlucoseData objects, or empty list if no data
     */
    public List<HeadlessHistory.GlucoseData> getAllGlucoseHistory(String serial) {
        if (jugglucoManager != null) {
            return jugglucoManager.getAllGlucoseHistory(serial);
        }
        return new ArrayList<>();
    }
    
    /**
     * Get glucose history as a flat array (most efficient for bulk operations)
     * This is the most reliable method for getting all glucose data
     * @param serial Sensor serial number
     * @return long array with [timestamp1, glucose1, timestamp2, glucose2, ...] format
     */
    public long[] getGlucoseHistoryFlat(String serial) {
        if (jugglucoManager != null) {
            return jugglucoManager.getGlucoseHistoryFlat(serial);
        }
        return null;
    }
    
    /**
     * Get glucose history within a time range
     * @param serial Sensor serial number
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis End time in milliseconds (null for no limit)
     * @return List of GlucoseData objects within the time range
     */
    public List<HeadlessHistory.GlucoseData> getGlucoseHistoryInRange(String serial, Long startMillis, Long endMillis) {
        if (jugglucoManager != null) {
            return jugglucoManager.getGlucoseHistoryInRange(serial, startMillis, endMillis);
        }
        return new ArrayList<>();
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
     * Demo method showing how to get all glucose history
     * This demonstrates the best practices for retrieving glucose data
     */
    public void demonstrateGlucoseHistory(String serial) {
        if (!initialized) {
            Log.w(TAG, "Juggluco not initialized");
            return;
        }
        
        Log.d(TAG, "=== Demonstrating Glucose History Retrieval ===");
        
        // Method 1: Get complete history as GlucoseData objects (most user-friendly)
        List<HeadlessHistory.GlucoseData> completeHistory = getAllGlucoseHistory(serial);
        Log.d(TAG, String.format("Complete history: %d readings", completeHistory.size()));
        
        if (!completeHistory.isEmpty()) {
            // Show first and last readings
            HeadlessHistory.GlucoseData first = completeHistory.get(0);
            HeadlessHistory.GlucoseData last = completeHistory.get(completeHistory.size() - 1);
            
            String firstTime = sdf.format(new Date(first.timeMillis));
            String lastTime = sdf.format(new Date(last.timeMillis));
            
            Log.d(TAG, String.format("First reading: %s - %d mg/dL (%.1f mmol/L)", 
                    firstTime, first.mgdl, first.mmolL));
            Log.d(TAG, String.format("Last reading: %s - %d mg/dL (%.1f mmol/L)", 
                    lastTime, last.mgdl, last.mmolL));
        }
        
        // Method 2: Get history as flat array (most efficient for bulk processing)
        long[] flatHistory = getGlucoseHistoryFlat(serial);
        if (flatHistory != null) {
            int numReadings = flatHistory.length / 2;
            Log.d(TAG, String.format("Flat history: %d readings", numReadings));
            
            // Calculate some basic statistics
            if (numReadings > 0) {
                double sum = 0;
                int validCount = 0;
                for (int i = 0; i < numReadings; i++) {
                    long packedGlucose = flatHistory[i * 2 + 1];
                    if (packedGlucose != 0) {
                        double mmolL = (double) packedGlucose / 4294967296.0;
                        double mgdl = mmolL * 18.0;
                        sum += mgdl;
                        validCount++;
                    }
                }
                
                if (validCount > 0) {
                    double average = sum / validCount;
                    Log.d(TAG, String.format("Average glucose: %.1f mg/dL", average));
                }
            }
        }
        
        // Method 3: Get history within a time range (last 24 hours)
        long now = System.currentTimeMillis();
        long yesterday = now - (24 * 60 * 60 * 1000L);
        List<HeadlessHistory.GlucoseData> recentHistory = getGlucoseHistoryInRange(serial, yesterday, now);
        Log.d(TAG, String.format("Last 24 hours: %d readings", recentHistory.size()));
        
        Log.d(TAG, "=== End Glucose History Demo ===");
    }

}
