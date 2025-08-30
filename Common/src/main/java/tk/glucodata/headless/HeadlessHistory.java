package tk.glucodata.headless;

import tk.glucodata.Natives;
import java.util.ArrayList;
import java.util.List;

public final class HeadlessHistory {
    private final HistoryListener listener;

    public HeadlessHistory(HistoryListener listener) {
        this.listener = listener;
    }

    /**
     * Extract current glucose data using the same logic as watchdrip
     * @return GlucoseData object containing current glucose information, or null if no data
     */
    public GlucoseData extractCurrentGlucoseData() {
        var gl = Natives.getlastGlucose();
        if (gl == null) return null;
        
        long res = gl[1];
        int glumgdl = (int) (res & 0xFFFFFFFFL);
        if (glumgdl != 0) {
            int alarm = (int) ((res >> 48) & 0xFFL);
            short ratein = (short) ((res >> 32) & 0xFFFFL);
            float rate = ratein / 1000.0f;
            long timeMillis = gl[0] * 1000L; // Convert seconds to milliseconds
            
            // Calculate mmol/L value
            float mmolL = glumgdl / 18.0f;
            
            return new GlucoseData(glumgdl, mmolL, rate, alarm, timeMillis);
        }
        return null;
    }

    /**
     * Extract and emit current glucose data to history listener
     * @param serial Sensor serial number
     * @return true if glucose data was successfully extracted and emitted
     */
    public boolean extractAndEmitCurrentGlucose(String serial) {
        if (listener == null) return false;
        
        GlucoseData glucoseData = extractCurrentGlucoseData();
        if (glucoseData != null) {
            // Create a single entry history array for the current glucose reading
            long[][] currentGlucose = new long[1][2];
            currentGlucose[0][0] = glucoseData.timeMillis;
            currentGlucose[0][1] = glucoseData.mgdl;
            
            listener.onHistory(serial, currentGlucose);
            return true;
        }
        return false;
    }

    /**
     * Get complete glucose history for a sensor as a list of GlucoseData objects
     * @param serial Sensor serial number
     * @return List of GlucoseData objects, or empty list if no data
     */
    public List<GlucoseData> getCompleteGlucoseHistory(String serial) {
        List<GlucoseData> history = new ArrayList<>();
        
        // Get sensor pointer
        long sensorPtr = Natives.getdataptr(serial);
        if (sensorPtr == 0) {
            return history;
        }
        
        // Get sensor start time
        long sensorStartMillis = Natives.getSensorStartmsec(sensorPtr);
        
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
                GlucoseData glucoseData = new GlucoseData(mgdl, mmolL, 0.0f, 0, timeMillis, sensorStartMillis, 0);
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
     * @param serial Sensor serial number
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis End time in milliseconds (null for no limit)
     * @return List of GlucoseData objects within the time range
     */
    public List<GlucoseData> getGlucoseHistoryInRange(String serial, Long startMillis, Long endMillis) {
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
     * Get current glucose history for a sensor (legacy method for backward compatibility)
     * @param serial Sensor serial number
     */
    public void emitFromNativeLast(String serial) {
        if (listener == null) return;
        
        GlucoseData glucoseData = extractCurrentGlucoseData();
        if (glucoseData != null) {
            // Create a single entry history array for the current glucose reading
            long[][] currentGlucose = new long[1][2];
            currentGlucose[0][0] = glucoseData.timeMillis;
            currentGlucose[0][1] = glucoseData.mgdl;
            
            listener.onHistory(serial, currentGlucose);
        }
    }

    /**
     * Get glucose history for a sensor within an optional time range (legacy method for backward compatibility)
     * @param serial Sensor serial number
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis End time in milliseconds (null for no limit)
     */
    public void emitFromNativeRange(String serial, Long startMillis, Long endMillis) {
        if (listener == null) return;
        
        List<GlucoseData> history = getGlucoseHistoryInRange(serial, startMillis, endMillis);
        
        // Convert to legacy format for backward compatibility
        long[][] historyArray = new long[history.size()][2];
        for (int i = 0; i < history.size(); i++) {
            GlucoseData data = history.get(i);
            historyArray[i][0] = data.timeMillis;
            historyArray[i][1] = data.mgdl;
        }
        
        listener.onHistory(serial, historyArray);
    }

    /**
     * Data class to hold extracted glucose information
     */
    public static class GlucoseData {
        public final int mgdl;                    // Glucose value in mg/dL
        public final float mmolL;                 // Glucose value in mmol/L
        public final float rate;                  // Rate of change in mg/dL/min
        public final int alarm;                   // Alarm status
        public final long timeMillis;             // Timestamp in milliseconds
        public final long sensorStartMillis;      // Sensor start time in milliseconds
        public final int sensorGen;               // Sensor generation
        
        public GlucoseData(int mgdl, float mmolL, float rate, int alarm, long timeMillis) {
            this(mgdl, mmolL, rate, alarm, timeMillis, 0, 0);
        }
        
        public GlucoseData(int mgdl, float mmolL, float rate, int alarm, long timeMillis, long sensorStartMillis, int sensorGen) {
            this.mgdl = mgdl;
            this.mmolL = mmolL;
            this.rate = rate;
            this.alarm = alarm;
            this.timeMillis = timeMillis;
            this.sensorStartMillis = sensorStartMillis;
            this.sensorGen = sensorGen;
        }
        
        @Override
        public String toString() {
            return String.format("GlucoseData{mgdl=%d, mmolL=%.1f, rate=%.3f, alarm=%d, time=%d, sensorStart=%d, gen=%d}", 
                               mgdl, mmolL, rate, alarm, timeMillis, sensorStartMillis, sensorGen);
        }
    }
}


