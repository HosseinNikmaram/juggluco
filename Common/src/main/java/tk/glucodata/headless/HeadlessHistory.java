package tk.glucodata.headless;

import tk.glucodata.Natives;
import java.util.ArrayList;
import java.util.List;

public final class HeadlessHistory {

    public HeadlessHistory() {
    }

        /**
     * Get complete glucose history for a sensor as a list of GlucoseData objects
     * This method uses the new Natives.getAllGlucoseHistory() method for complete data
     *
     * @param serial Sensor serial number (not used in current implementation but kept for compatibility)
     * @return List of GlucoseData objects, or empty list if no data
     */
    public static List<GlucoseData> getCompleteGlucoseHistory(String serial) {
        List<GlucoseData> history = new ArrayList<>();
        
        try {
            // Use the new native method that returns ALL glucose history
            long[] flatData = Natives.getAllGlucoseHistory();
            if (flatData == null || flatData.length < 2) {
                return history;
            }
            
            // Process the flat array: [timestamp1, glucose1, timestamp2, glucose2, ...]
            int n = flatData.length / 2;
            for (int i = 0; i < n; i++) {
                long timeSeconds = flatData[i * 2];
                long packedGlucose = flatData[i * 2 + 1];
                
                if (timeSeconds > 0 && packedGlucose != 0) {
                    // Convert timestamp from seconds to milliseconds
                    long timeMillis = timeSeconds * 1000L;
                    
                    // Extract glucose value and rate from packed data
                    // The packed data contains: rate (16 bits) | alarm (16 bits) | mg/dL (32 bits)
                    int mgdl = (int) (packedGlucose & 0xFFFFFFFFL);
                    short rateRaw = (short) ((packedGlucose >> 32) & 0xFFFFL);
                    int alarm = (int) ((packedGlucose >> 48) & 0xFFL);
                    
                    if (mgdl > 0) {
                        float rate = rateRaw / 1000.0f; // Convert rate to proper units
                        float mmolL = mgdl / 18.0f;     // Convert mg/dL to mmol/L
                        
                        // Create GlucoseData with rate information
                        GlucoseData glucoseData = new GlucoseData(mgdl, mmolL, timeMillis, rate, alarm);
                        history.add(glucoseData);
                    }
                }
            }
        } catch (Exception e) {
            // Log error and return empty list
            System.err.println("Error getting glucose history: " + e.getMessage());
        }
        
        return history;
    }

    /**
     * Get glucose history for a sensor within a time range as a list of GlucoseData objects
     *
     * @param serial Sensor serial number
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis   End time in milliseconds (null for no limit)
     * @return List of GlucoseData objects within the time range
     */
    public static List<GlucoseData> getGlucoseHistoryInRange(String serial, Long startMillis, Long endMillis) {
        List<GlucoseData> allHistory = getCompleteGlucoseHistory(serial);
        List<GlucoseData> filteredHistory = new ArrayList<>();

        for (GlucoseData data : allHistory) {
            // Apply time filtering
            if (startMillis != null && data.timeMillis < startMillis) {
                continue;
            }
            if (endMillis != null && data.timeMillis > endMillis) {
                continue;
            }
            filteredHistory.add(data);
        }

        return filteredHistory;
    }

    /**
     * Get the latest glucose reading for a sensor
     *
     * @param serial Sensor serial number
     * @return Latest GlucoseData object, or null if no data available
     */
    public static GlucoseData getLatestGlucoseReading(String serial) {
        List<GlucoseData> history = getCompleteGlucoseHistory(serial);
        if (history.isEmpty()) {
            return null;
        }

        // Find the most recent reading (highest timestamp)
        GlucoseData latest = history.get(0);
        for (GlucoseData data : history) {
            if (data.timeMillis > latest.timeMillis) {
                latest = data;
            }
        }
        return latest;
    }

    /**
     * Get glucose history as a flat array (more efficient for bulk operations)
     * This is the most reliable method for getting all glucose data
     *
     * @return long array with [timestamp1, glucose1, timestamp2, glucose2, ...] format
     */
    public static long[] getGlucoseHistoryFlat() {
        try {
            return Natives.getAllGlucoseHistory();
        } catch (Exception e) {
            System.err.println("Error getting flat glucose history: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get glucose history as a flat array within a time range
     *
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis   End time in milliseconds (null for no limit)
     * @return long array with filtered data, or null if error
     */
    public static long[] getGlucoseHistoryFlatInRange(Long startMillis, Long endMillis) {
        long[] flatData = getGlucoseHistoryFlat();
        if (flatData == null || flatData.length < 2) {
            return null;
        }
        
        if (startMillis == null && endMillis == null) {
            return flatData; // No filtering needed
        }
        
        // Count how many entries are in range
        int totalPairs = flatData.length / 2;
        int count = 0;
        for (int i = 0; i < totalPairs; i++) {
            long t = flatData[i * 2] * 1000L; // Convert to milliseconds
            if (startMillis != null && t < startMillis) continue;
            if (endMillis != null && t > endMillis) continue;
            count++;
        }
        
        // Create filtered array
        long[] out = new long[count * 2];
        int idx = 0;
        for (int i = 0; i < totalPairs; i++) {
            long t = flatData[i * 2] * 1000L; // Convert to milliseconds
            if (startMillis != null && t < startMillis) continue;
            if (endMillis != null && t > endMillis) continue;
            out[idx++] = flatData[i * 2];     // timestamp
            out[idx++] = flatData[i * 2 + 1]; // glucose value
        }
        
        return out;
    }

    /**
     * Data class to hold extracted glucose information
     */
    public static class GlucoseData {
        public int mgdl;                    // Glucose value in mg/dL
        public float mmolL;                 // Glucose value in mmol/L
        public long timeMillis;             // Timestamp in milliseconds
        public float rate;                  // Rate of change (mg/dL/min)
        public int alarm;                   // Alarm code

        public GlucoseData(int mgdl, float mmolL, long timeMillis) {
            this.mgdl = mgdl;
            this.mmolL = mmolL;
            this.timeMillis = timeMillis;
            this.rate = 0.0f;
            this.alarm = 0;
        }

        public GlucoseData(int mgdl, float mmolL, long timeMillis, float rate, int alarm) {
            this.mgdl = mgdl;
            this.mmolL = mmolL;
            this.timeMillis = timeMillis;
            this.rate = rate;
            this.alarm = alarm;
        }

        @Override
        public String toString() {
            return String.format("GlucoseData{mgdl=%d, mmolL=%.1f, time=%d, rate=%.1f, alarm=%d}",
                    mgdl, mmolL, timeMillis, rate, alarm);
        }
    }
}


