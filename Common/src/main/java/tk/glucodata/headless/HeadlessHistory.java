package tk.glucodata.headless;

import tk.glucodata.Natives;

public final class HeadlessHistory {
    private final HistoryListener listener;

    public HeadlessHistory(HistoryListener listener) {
        this.listener = listener;
    }

    /**
     * Extract glucose data from native data using the same logic as watchdrip
     * @param serial Sensor serial number
     * @return true if glucose data was successfully extracted and emitted
     */
    public boolean extractAndEmitGlucose(String serial) {
        if (listener == null) return false;
        
        GlucoseData glucoseData = extractGlucoseData();
        if (glucoseData != null) {
            // Create a single entry history array for the current glucose reading
            long[][] currentGlucose = new long[1][2];
            currentGlucose[0][0] = glucoseData.timeMillis;
            currentGlucose[0][1] = glucoseData.mgdl;
            
            // Emit the current glucose reading as history
            listener.onHistory(serial, currentGlucose);
            return true;
        }
        return false;
    }

    /**
     * Extract and return detailed glucose data using the same logic as watchdrip
     * @return GlucoseData object containing all extracted information, or null if no data
     */
    public GlucoseData extractGlucoseData() {
        var gl = Natives.getlastGlucose();
        if (gl == null) return null;
        
        long res = gl[1];
        int glumgdl = (int) (res & 0xFFFFFFFFL);
        if (glumgdl != 0) {
            int alarm = (int) ((res >> 48) & 0xFFL);
            short ratein = (short) ((res >> 32) & 0xFFFFL);
            float rate = ratein / 1000.0f;
            long timeMillis = gl[0] * 1000L; // Convert seconds to milliseconds
            
            // Calculate mmol/L value (similar to how Applic.unit conversion works)
            float mmolL = glumgdl / 18.0f; // Convert mg/dL to mmol/L
            
            return new GlucoseData(glumgdl, mmolL, rate, alarm, timeMillis);
        }
        return null;
    }

    /**
     * Get current glucose history for a sensor using the new extraction method
     * @param serial Sensor serial number
     */
    public void emitFromNativeLast(String serial) {
        if (listener == null) return;
        
        GlucoseData glucoseData = extractGlucoseData();
        if (glucoseData != null) {
            // Create a single entry history array for the current glucose reading
            long[][] currentGlucose = new long[1][2];
            currentGlucose[0][0] = glucoseData.timeMillis;
            currentGlucose[0][1] = glucoseData.mgdl;
            
            listener.onHistory(serial, currentGlucose);
        }
    }

    /**
     * Get glucose history for a sensor within an optional time range
     * @param serial Sensor serial number
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis End time in milliseconds (null for no limit)
     */
    public void emitFromNativeRange(String serial, Long startMillis, Long endMillis) {
        if (listener == null) return;
        
        // For range queries, we need to use the historical data method
        // since extractGlucoseData only provides current data
        long[] flat = Natives.getlastGlucose();
        if (flat == null || flat.length < 2) return;
        
        listener.onHistory(serial, toPairs(flat, startMillis, endMillis));
    }

    /**
     * Convert native glucose data to history pairs with optional time filtering
     * @param flat Native glucose data array
     * @param startMillis Start time filter (null for no limit)
     * @param endMillis End time filter (null for no limit)
     * @return Array of [timeMillis, mgdl] pairs
     */
    private static long[][] toPairs(long[] flat, Long startMillis, Long endMillis) {
        int totalPairs = flat.length / 2;
        int count = 0;
        
        // First pass: count matches to allocate exact array
        for (int i = 0; i < totalPairs; i++) {
            long tMillis = flat[2 * i] * 1000L; // native gives seconds
            if (startMillis != null && tMillis < startMillis) continue;
            if (endMillis != null && tMillis > endMillis) continue;
            count++;
        }
        
        long[][] hist = new long[count][2];
        int idx = 0;
        
        for (int i = 0; i < totalPairs; i++) {
            long tMillis = flat[2 * i] * 1000L;
            if (startMillis != null && tMillis < startMillis) continue;
            if (endMillis != null && tMillis > endMillis) continue;
            
            long packed = flat[2 * i + 1];
            // Decode Q32.32 mmol/L -> mg/dL
            double mmolL = (double) packed / 4294967296.0; // 2^32
            long mgdl = Math.round(mmolL * 18.0);
            
            hist[idx][0] = tMillis;
            hist[idx][1] = mgdl;
            idx++;
        }
        
        return hist;
    }

    /**
     * Data class to hold extracted glucose information
     */
    public static class GlucoseData {
        public final int mgdl;           // Glucose value in mg/dL
        public final float mmolL;        // Glucose value in mmol/L
        public final float rate;         // Rate of change in mg/dL/min
        public final int alarm;          // Alarm status
        public final long timeMillis;    // Timestamp in milliseconds
        
        public GlucoseData(int mgdl, float mmolL, float rate, int alarm, long timeMillis) {
            this.mgdl = mgdl;
            this.mmolL = mmolL;
            this.rate = rate;
            this.alarm = alarm;
            this.timeMillis = timeMillis;
        }
        
        @Override
        public String toString() {
            return String.format("GlucoseData{mgdl=%d, mmolL=%.1f, rate=%.3f, alarm=%d, time=%d}", 
                               mgdl, mmolL, rate, alarm, timeMillis);
        }
    }
}


