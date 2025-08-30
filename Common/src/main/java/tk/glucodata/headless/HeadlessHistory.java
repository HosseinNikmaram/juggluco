package tk.glucodata.headless;

import tk.glucodata.Natives;
import tk.glucodata.watchdrip;
import tk.glucodata.Applic;

import java.util.List;
import java.util.ArrayList;

public final class HeadlessHistory {
    private final HistoryListener listener;
    private final GlucoseListener glucoseListener;

    public HeadlessHistory(HistoryListener listener) {
        this.listener = listener;
        this.glucoseListener = null;
    }

    public HeadlessHistory(GlucoseListener glucoseListener) {
        this.listener = null;
        this.glucoseListener = glucoseListener;
    }

    public HeadlessHistory(HistoryListener historyListener, GlucoseListener glucoseListener) {
        this.listener = historyListener;
        this.glucoseListener = glucoseListener;
    }

    // Uses Natives.getlastGlucose() which returns a flat long[] as
    // [timeSeconds0, packed0, timeSeconds1, packed1, ...].
    // The packed value contains glucose data, rate, and alarm information.
    // We transform it into onGlucose calls matching the watchdrip structure.
    public void emitFromNativeLast(String serial) {
        if (glucoseListener == null) return;
        long[] flat = Natives.getlastGlucose();
        if (flat == null || flat.length < 2) return;
        emitGlucoseReadings(serial, flat, null, null);
    }

    public void emitFromNativeRange(String serial, Long startMillis, Long endMillis) {
        if (glucoseListener == null) return;
        long[] flat = Natives.getlastGlucose();
        if (flat == null || flat.length < 2) return;
        emitGlucoseReadings(serial, flat, startMillis, endMillis);
    }

    // Legacy method for backward compatibility
    public void emitFromNativeLastLegacy(String serial) {
        if (listener == null) return;
        long[] flat = Natives.getlastGlucose();
        if (flat == null || flat.length < 2) return;
        listener.onHistory(serial, toPairs(flat, null, null));
    }

    public void emitFromNativeRangeLegacy(String serial, Long startMillis, Long endMillis) {
        if (listener == null) return;
        long[] flat = Natives.getlastGlucose();
        if (flat == null || flat.length < 2) return;
        listener.onHistory(serial, toPairs(flat, startMillis, endMillis));
    }

    /**
     * Emit glucose readings using onGlucose method based on watchdrip structure
     */
    private void emitGlucoseReadings(String serial, long[] flat, Long startMillis, Long endMillis) {
        int totalPairs = flat.length / 2;
        
        for (int i = 0; i < totalPairs; i++) {
            long tMillis = flat[2 * i] * 1000L; // native gives seconds, convert to milliseconds
            
            // Check time range filter
            if (startMillis != null && tMillis < startMillis) continue;
            if (endMillis != null && tMillis > endMillis) continue;
            
            long packed = flat[2 * i + 1];
            
            // Decode glucose data using watchdrip structure
            int glumgdl = (int) (packed & 0xFFFFFFFFL);
            if (glumgdl == 0) continue; // Skip invalid readings
            
            int alarm = (int) ((packed >> 48) & 0xFFL);
            short ratein = (short) ((packed >> 32) & 0xFFFFL);
            float rate = ratein / 1000.0f;
            
            // Convert to current units (same as watchdrip)
            float value = Applic.unit == 1 ? glumgdl / Applic.mgdLmult : glumgdl;
            
            // Get sensor information (default values if not available)
            long sensorStartMillis = System.currentTimeMillis() - (24 * 60 * 60 * 1000L); // Default to 24 hours ago
            int sensorGen = 1; // Default sensor generation
            
            // Call onGlucose with decoded data
            try {
                glucoseListener.onGlucose(serial, glumgdl, value, rate, alarm, tMillis, sensorStartMillis, sensorGen);
            } catch (Exception e) {
                // Log error but continue processing other readings
                System.err.println("Error calling onGlucose: " + e.getMessage());
            }
        }
    }

    /**
     * Get glucose history from watchdrip if available
     */
    public void emitFromWatchdripHistory(String serial) {
        if (glucoseListener == null) return;
        
        try {
            List<watchdrip.GlucoseReading> history = watchdrip.getGlucoseHistory(serial);
            for (watchdrip.GlucoseReading reading : history) {
                glucoseListener.onGlucose(
                    reading.serial,
                    reading.mgdl,
                    reading.value,
                    reading.rate,
                    reading.alarm,
                    reading.timeMillis,
                    reading.sensorStartMillis,
                    reading.sensorGen
                );
            }
        } catch (Exception e) {
            System.err.println("Error getting watchdrip history: " + e.getMessage());
        }
    }

    /**
     * Get glucose history from watchdrip within a time range
     */
    public void emitFromWatchdripHistory(String serial, Long startMillis, Long endMillis) {
        if (glucoseListener == null) return;
        
        try {
            List<watchdrip.GlucoseReading> history = watchdrip.getGlucoseHistory(serial, startMillis, endMillis);
            for (watchdrip.GlucoseReading reading : history) {
                glucoseListener.onGlucose(
                    reading.serial,
                    reading.mgdl,
                    reading.value,
                    reading.rate,
                    reading.alarm,
                    reading.timeMillis,
                    reading.sensorStartMillis,
                    reading.sensorGen
                );
            }
        } catch (Exception e) {
            System.err.println("Error getting watchdrip history: " + e.getMessage());
        }
    }

    // Legacy method for backward compatibility - keep the old toPairs implementation
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
}


