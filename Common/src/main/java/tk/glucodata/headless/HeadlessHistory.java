package tk.glucodata.headless;

import tk.glucodata.Natives;
import java.util.ArrayList;
import java.util.List;

public final class HeadlessHistory {

    public HeadlessHistory() {}
    /**
     * Get complete glucose history for a sensor as a list of GlucoseData objects
     * @return List of GlucoseData objects, or empty list if no data
     */
    public static List<GlucoseData> getCompleteGlucoseHistory(String serial) {
        List<GlucoseData> history = new ArrayList<>();
        
        // Get sensor pointer
        long sensorPtr = Natives.getdataptr(serial);
        if (sensorPtr == 0) {
            return history;
        }
        
        // Iterate through all glucose readings
        int pos = 0;
        while (true) {
            long timeValue = Natives.streamfromSensorptr(sensorPtr, pos);
            
            // Check if we've reached the end
            if ((timeValue >> 48) == 0) {
                break;
            }
            
            // Extract data from the packed value
            long timeSeconds = timeValue & 0xFFFFFFFFL;
            int mgdl = (int) ((timeValue >> 32) & 0xFFFFL);
            int nextPos = (int) ((timeValue >> 48) & 0xFFFFL);
            
            if (mgdl > 0 && timeSeconds > 0) {
                long timeMillis = timeSeconds * 1000L;
                float mmolL = mgdl / 18.0f;
                
                // Create GlucoseData object
                GlucoseData glucoseData = new GlucoseData(mgdl, mmolL, timeMillis);
                history.add(glucoseData);
            }
            
            // Move to next position
            if (nextPos <= pos) {
                break; // Prevent infinite loop
            }
            pos = nextPos;
        }
        
        return history;
    }

    /**
     * Get glucose history for a sensor within a time range as a list of GlucoseData objects
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis End time in milliseconds (null for no limit)
     * @return List of GlucoseData objects within the time range
     */
    public static List<GlucoseData> getGlucoseHistoryInRange(String serial,Long startMillis, Long endMillis) {
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
     * Data class to hold extracted glucose information
     */
    public static class GlucoseData {
        public int mgdl;                    // Glucose value in mg/dL
        public float mmolL;                 // Glucose value in mmol/L
        public long timeMillis;             // Timestamp in milliseconds

        public GlucoseData(int mgdl, float mmolL, long timeMillis) {
            this.mgdl = mgdl;
            this.mmolL = mmolL;
            this.timeMillis = timeMillis;
        }


        
        @Override
        public String toString() {
            return String.format("GlucoseData{mgdl=%d, mmolL=%.1f,  time=%d}",
                               mgdl, mmolL, timeMillis);
        }
    }
}


