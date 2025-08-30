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
            
            // Example 1: Using HeadlessHistory to get complete glucose history
            List<HeadlessHistory.GlucoseData> completeHistory = HeadlessHistory.getCompleteGlucoseHistory(serial);
            Log.d(TAG, "Complete glucose history count: " + completeHistory.size());
            
            // Example 2: Using HeadlessHistory to get latest reading
            HeadlessHistory.GlucoseData latestReading = HeadlessHistory.getLatestGlucoseReading(serial);
            if (latestReading != null) {
                String latestTimeStr = sdf.format(new Date(latestReading.timeMillis));
                Log.d(TAG, String.format(
                        "Latest reading - Time: %s, Glucose: %d mg/dL (%.1f mmol/L)",
                        latestTimeStr, latestReading.mgdl, latestReading.mmolL
                ));
            }
            
            // Example 3: Using HeadlessHistory iterator for memory-efficient processing
            HeadlessHistory.GlucoseIterator iterator = HeadlessHistory.getGlucoseIterator(serial);
            int count = 0;
            while (iterator.hasNext() && count < 10) { // Show first 10 readings
                HeadlessHistory.GlucoseData data = iterator.next();
                if (data != null) {
                    String timeStr = sdf.format(new Date(data.timeMillis));
                    Log.d(TAG, String.format(
                            "Iterator reading %d - Time: %s, Glucose: %d mg/dL (%.1f mmol/L)",
                            ++count, timeStr, data.mgdl, data.mmolL
                    ));
                }
            }
            
            // Example 4: Using HeadlessHistory to get history in time range
            long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L);
            List<HeadlessHistory.GlucoseData> recentHistory = HeadlessHistory.getGlucoseHistoryInRange(serial, oneDayAgo, null);
            Log.d(TAG, "Last 24 hours glucose readings: " + recentHistory.size());
            
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
    
    // ===== HeadlessHistory Usage Examples =====
    
    /**
     * Get complete glucose history for a sensor
     * @param serial Sensor serial number
     * @return List of glucose readings
     */
    public List<HeadlessHistory.GlucoseData> getCompleteGlucoseHistory(String serial) {
        if (!initialized) return new ArrayList<>();
        return HeadlessHistory.getCompleteGlucoseHistory(serial);
    }
    
    /**
     * Get the latest glucose reading for a sensor
     * @param serial Sensor serial number
     * @return Latest glucose reading or null
     */
    public HeadlessHistory.GlucoseData getLatestGlucoseReading(String serial) {
        if (!initialized) return null;
        return HeadlessHistory.getLatestGlucoseReading(serial);
    }
    
    /**
     * Get glucose history within a time range
     * @param serial Sensor serial number
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis End time in milliseconds (null for no limit)
     * @return List of glucose readings in the time range
     */
    public List<HeadlessHistory.GlucoseData> getGlucoseHistoryInRange(String serial, Long startMillis, Long endMillis) {
        if (!initialized) return new ArrayList<>();
        return HeadlessHistory.getGlucoseHistoryInRange(serial, startMillis, endMillis);
    }
    
    /**
     * Get glucose history using iterator pattern
     * @param serial Sensor serial number
     * @return GlucoseIterator object
     */
    public HeadlessHistory.GlucoseIterator getGlucoseIterator(String serial) {
        if (!initialized) return null;
        return HeadlessHistory.getGlucoseIterator(serial);
    }
    
    /**
     * Print glucose history summary to log
     * @param serial Sensor serial number
     */
    public void printGlucoseHistorySummary(String serial) {
        if (!initialized) return;
        
        List<HeadlessHistory.GlucoseData> history = HeadlessHistory.getCompleteGlucoseHistory(serial);
        if (history.isEmpty()) {
            Log.d(TAG, "No glucose history available for sensor: " + serial);
            return;
        }
        
        // Calculate statistics
        int totalReadings = history.size();
        int minMgdl = Integer.MAX_VALUE;
        int maxMgdl = Integer.MIN_VALUE;
        long totalMgdl = 0;
        long earliestTime = Long.MAX_VALUE;
        long latestTime = Long.MIN_VALUE;
        
        for (HeadlessHistory.GlucoseData data : history) {
            minMgdl = Math.min(minMgdl, data.mgdl);
            maxMgdl = Math.max(maxMgdl, data.mgdl);
            totalMgdl += data.mgdl;
            earliestTime = Math.min(earliestTime, data.timeMillis);
            latestTime = Math.max(latestTime, data.timeMillis);
        }
        
        double avgMgdl = (double) totalMgdl / totalReadings;
        long durationHours = (latestTime - earliestTime) / (1000 * 60 * 60);
        
        Log.d(TAG, String.format(
                "Glucose History Summary for %s:\n" +
                "Total readings: %d\n" +
                "Range: %d - %d mg/dL\n" +
                "Average: %.1f mg/dL\n" +
                "Duration: %d hours\n" +
                "Earliest: %s\n" +
                "Latest: %s",
                serial, totalReadings, minMgdl, maxMgdl, avgMgdl, durationHours,
                sdf.format(new Date(earliestTime)), sdf.format(new Date(latestTime))
        ));
    }
}
