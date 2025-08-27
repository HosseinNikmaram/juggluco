package tk.glucodata.headless;

import tk.glucodata.Natives;

public final class HeadlessHistory {
    private final HistoryListener listener;

    public HeadlessHistory(HistoryListener listener) {
        this.listener = listener;
    }

    // Uses Natives.getlastGlucose() which returns a flat long[];
    // we transform it into pairs [timeMillis, mgdl].
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
        // First pass: count matches to allocate exact array
        int count = 0;
        for (int i = 0; i < totalPairs; i++) {
            long t = flat[2 * i];
            if (startMillis != null && t < startMillis) continue;
            if (endMillis != null && t > endMillis) continue;
            count++;
        }
        long[][] hist = new long[count][2];
        int idx = 0;
        for (int i = 0; i < totalPairs; i++) {
            long t = flat[2 * i];
            if (startMillis != null && t < startMillis) continue;
            if (endMillis != null && t > endMillis) continue;
            hist[idx][0] = t;
            hist[idx][1] = flat[2 * i + 1];
            idx++;
        }
        return hist;
    }
}


