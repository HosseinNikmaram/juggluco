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
            // The C++ code returns (len << 48) when no more data
            if ((timeValue >> 48) == 0 || (timeValue >> 48) >= 0xFFFF) {
                break;
            }
            
            // Extract data from the packed value
            // Bits 0-31: time (seconds)
            // Bits 32-47: mg/dL value (16 bits) 
            // Bits 48-63: next position (16 bits)
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
            if (nextPos <= pos || nextPos >= 0xFFFF) {
                break; // Prevent infinite loop or invalid position
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
     * Get the latest glucose reading for a sensor
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
     * Get glucose history using iterator pattern (similar to GlucoseIterator)
     * This is more memory efficient for large datasets
     * @param serial Sensor serial number
     * @return GlucoseIterator object
     */
    public static GlucoseIterator getGlucoseIterator(String serial) {
        return new GlucoseIterator(serial);
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

    /**
     * Iterator class for glucose data (similar to the original GlucoseIterator)
     * This provides a memory-efficient way to iterate through glucose readings
     */
    public static class GlucoseIterator {
        private final String serial;
        private final long sensorPtr;
        private int currentPos;
        private boolean hasNext;
        private GlucoseData currentData;

        public GlucoseIterator(String serial) {
            this.serial = serial;
            this.sensorPtr = Natives.getdataptr(serial);
            this.currentPos = 0;
            this.hasNext = sensorPtr != 0;
            this.currentData = null;
        }

        /**
         * Check if there are more glucose readings available
         * @return true if more data is available
         */
        public boolean hasNext() {
            if (!hasNext) return false;
            
            // Try to get the next reading
            if (currentData == null) {
                currentData = getNextReading();
                if (currentData == null) {
                    hasNext = false;
                }
            }
            return hasNext;
        }

        /**
         * Get the next glucose reading
         * @return Next GlucoseData object, or null if no more data
         */
        public GlucoseData next() {
            if (!hasNext()) {
                return null;
            }
            
            GlucoseData result = currentData;
            currentData = null; // Reset for next call
            return result;
        }

        /**
         * Get the next reading from the sensor
         * @return GlucoseData object or null if no more data
         */
        private GlucoseData getNextReading() {
            if (sensorPtr == 0) return null;
            
            long timeValue = Natives.streamfromSensorptr(sensorPtr, currentPos);
            
            // Check if we've reached the end
            if ((timeValue >> 48) == 0 || (timeValue >> 48) >= 0xFFFF) {
                return null;
            }
            
            // Extract data from the packed value
            long timeSeconds = timeValue & 0xFFFFFFFFL;
            int mgdl = (int) ((timeValue >> 32) & 0xFFFFL);
            int nextPos = (int) ((timeValue >> 48) & 0xFFFFL);
            
            if (mgdl > 0 && timeSeconds > 0) {
                long timeMillis = timeSeconds * 1000L;
                float mmolL = mgdl / 18.0f;
                
                // Update position for next iteration
                if (nextPos > currentPos && nextPos < 0xFFFF) {
                    currentPos = nextPos;
                } else {
                    hasNext = false;
                }
                
                return new GlucoseData(mgdl, mmolL, timeMillis);
            }
            
            // Move to next position
            if (nextPos <= currentPos || nextPos >= 0xFFFF) {
                hasNext = false;
                return null;
            }
            currentPos = nextPos;
            return getNextReading(); // Recursively try next position
        }

        /**
         * Reset the iterator to start from the beginning
         */
        public void reset() {
            currentPos = 0;
            hasNext = sensorPtr != 0;
            currentData = null;
        }
    }
}


