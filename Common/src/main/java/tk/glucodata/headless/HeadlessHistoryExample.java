package tk.glucodata.headless;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Example class demonstrating how to use HeadlessHistory functionality
 * This shows various ways to access glucose data from Libre sensors
 */
public class HeadlessHistoryExample {
    private static final String TAG = "HeadlessHistoryExample";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Example 1: Get complete glucose history for a sensor
     */
    public static void exampleGetCompleteHistory(String serial) {
        try {
            Log.d(TAG, "Getting complete glucose history for sensor: " + serial);
            
            List<HeadlessHistory.GlucoseData> history = HeadlessHistory.getCompleteGlucoseHistory(serial);
            
            if (history.isEmpty()) {
                Log.d(TAG, "No glucose history found for sensor: " + serial);
                return;
            }
            
            Log.d(TAG, "Found " + history.size() + " glucose readings");
            
            // Show first few readings
            for (int i = 0; i < Math.min(5, history.size()); i++) {
                HeadlessHistory.GlucoseData data = history.get(i);
                String timeStr = sdf.format(new Date(data.timeMillis));
                Log.d(TAG, String.format("Reading %d: Time=%s, mg/dL=%d, mmol/L=%.1f",
                    i + 1, timeStr, data.mgdl, data.mmolL));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting complete history: " + e.getMessage(), e);
        }
    }
    
    /**
     * Example 2: Get latest glucose reading
     */
    public static void exampleGetLatestReading(String serial) {
        try {
            Log.d(TAG, "Getting latest glucose reading for sensor: " + serial);
            
            HeadlessHistory.GlucoseData latest = HeadlessHistory.getLatestGlucoseReading(serial);
            
            if (latest == null) {
                Log.d(TAG, "No latest reading found for sensor: " + serial);
                return;
            }
            
            String timeStr = sdf.format(new Date(latest.timeMillis));
            Log.d(TAG, String.format("Latest reading: Time=%s, mg/dL=%d, mmol/L=%.1f",
                timeStr, latest.mgdl, latest.mmolL));
                
        } catch (Exception e) {
            Log.e(TAG, "Error getting latest reading: " + e.getMessage(), e);
        }
    }
    
    /**
     * Example 3: Get glucose history in time range
     */
    public static void exampleGetHistoryInRange(String serial) {
        try {
            Log.d(TAG, "Getting glucose history in time range for sensor: " + serial);
            
            // Get readings from last 24 hours
            long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L);
            long now = System.currentTimeMillis();
            
            List<HeadlessHistory.GlucoseData> recentHistory = 
                HeadlessHistory.getGlucoseHistoryInRange(serial, oneDayAgo, now);
            
            Log.d(TAG, "Last 24 hours readings: " + recentHistory.size());
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting history in range: " + e.getMessage(), e);
        }
    }
    
    /**
     * Example 4: Use iterator for memory-efficient processing
     */
    public static void exampleUseIterator(String serial) {
        try {
            Log.d(TAG, "Using iterator for sensor: " + serial);
            
            HeadlessHistory.GlucoseIterator iterator = HeadlessHistory.getGlucoseIterator(serial);
            
            if (iterator == null) {
                Log.d(TAG, "Could not create iterator for sensor: " + serial);
                return;
            }
            
            int count = 0;
            while (iterator.hasNext() && count < 50) {
                HeadlessHistory.GlucoseData data = iterator.next();
                if (data != null) {
                    count++;
                }
            }
            
            Log.d(TAG, "Processed " + count + " readings with iterator");
            
        } catch (Exception e) {
            Log.e(TAG, "Error using iterator: " + e.getMessage(), e);
        }
    }
    
    /**
     * Run all examples for a given sensor
     */
    public static void runAllExamples(String serial) {
        if (serial == null || serial.trim().isEmpty()) {
            Log.e(TAG, "Invalid sensor serial number");
            return;
        }
        
        Log.d(TAG, "Running all HeadlessHistory examples for sensor: " + serial);
        
        exampleGetCompleteHistory(serial);
        exampleGetLatestReading(serial);
        exampleGetHistoryInRange(serial);
        exampleUseIterator(serial);
        
        Log.d(TAG, "All examples completed for sensor: " + serial);
    }
}