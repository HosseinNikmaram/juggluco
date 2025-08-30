package tk.glucodata.headless;

import tk.glucodata.Natives;

public final class HeadlessHistory {
    private final HistoryListener listener;

    public HeadlessHistory(HistoryListener listener) {
        this.listener = listener;
    }

    // Static methods to get raw data for use by other classes like HeadlessStats
    public static long[] getAllRaw() {
        return Natives.getlastGlucose();
    }

    public static long[] getFilteredRaw(Long startMillis, Long endMillis) {
        long[] flat = Natives.getlastGlucose();
        if (flat == null || flat.length < 2) return null;
        return filterRaw(flat, startMillis, endMillis);
    }

    private static long[] filterRaw(long[] flat, Long startMillis, Long endMillis) {
        if (startMillis == null && endMillis == null) return flat;
        int totalPairs = flat.length / 2;
        int count = 0;
        for (int i = 0; i < totalPairs; i++) {
            long tMillis = flat[2 * i] * 1000L; // native gives seconds
            if (startMillis != null && tMillis < startMillis) continue;
            if (endMillis != null && tMillis > endMillis) continue;
            count++;
        }
        long[] out = new long[count * 2];
        int idx = 0;
        for (int i = 0; i < totalPairs; i++) {
            long tMillis = flat[2 * i] * 1000L;
            if (startMillis != null && tMillis < startMillis) continue;
            if (endMillis != null && tMillis > endMillis) continue;
            out[idx++] = flat[2 * i];
            out[idx++] = flat[2 * i + 1];
        }
        return out;
    }

    // Uses Natives.getlastGlucose() which returns a flat long[] as
    // [timeSeconds0, packed0, timeSeconds1, packed1, ...].
    // The packed value is Q32.32 fixed-point mmol/L in a 64-bit long.
    // We transform it into pairs [timeMillis, mgdl] where mgdl is rounded.
    public void emitFromNativeLast(String serial) {
        if (listener == null) return;
        long[] flat = Natives.getlastGlucose();
        if (flat == null || flat.length < 2) return;
        listener.onHistory(serial, toPairs(flat, null, null));
    }

    public void emitFromNativeRange(String serial, Long startMillis, Long endMillis) {
        if (listener == null) return;
        long[] flat = Natives.getlastGlucose();
        if (flat == null || flat.length < 2) return;
        listener.onHistory(serial, toPairs(flat, startMillis, endMillis));
    }

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


