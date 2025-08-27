package tk.glucodata.headless;

import tk.glucodata.Natives;

public final class HeadlessStats {
    private final StatsListener listener;

    public HeadlessStats(StatsListener listener) {
        this.listener = listener;
    }

    public void emitIfReady(String serial) {
        if (listener == null) return;
        if (!Natives.makepercentages()) return;
        var hist = HeadlessHistoryAccessor.getAll();
        if (hist == null || hist.length < 2) return;
        listener.onStats(serial, computeSummary(hist));
    }

    public void emitIfReady(String serial, Long startMillis, Long endMillis) {
        if (listener == null) return;
        if (!Natives.makepercentages()) return;
        var hist = HeadlessHistoryAccessor.getAll();
        if (hist == null || hist.length < 2) return;
        var ranged = HeadlessHistoryAccessor.filter(hist, startMillis, endMillis);
        if (ranged == null || ranged.length < 2) return;
        listener.onStats(serial, computeSummary(ranged));
    }

    private static HeadlessStatsSummary computeSummary(long[] flat) {
        int n = flat.length / 2;
        if (n == 0) return new HeadlessStatsSummary(0, 0, 0, 0, 0, 0, null, null);
        long first = flat[0];
        long last = flat[(n - 1) * 2];
        double sum = 0.0;
        double sumSq = 0.0;
        for (int i = 0; i < n; i++) {
            double g = flat[2 * i + 1];
            sum += g;
            sumSq += g * g;
        }
        double mean = sum / n;
        double variance = Math.max(0.0, (sumSq / n) - (mean * mean));
        double sd = Math.sqrt(variance);
        double gv = mean > 0 ? (sd * 100.0 / mean) : 0.0;
        double durationDays = (last > first) ? (last - first) / 86_400_000.0 : 0.0;
        // Rough time active based on expected 5-min intervals (Libre3 history)
        double expected = durationDays > 0 ? (durationDays * 24 * 12) : n;
        double timeActivePercent = expected > 0 ? Math.min(100.0, n * 100.0 / expected) : 0.0;
        // Simple A1C/GMI estimates from mean mg/dL
        Double estA1C = (mean > 0) ? ((mean + 46.7) / 28.7) : null; // NGSP %
        Double gmi = (mean > 0) ? (3.31 + 0.02392 * mean) : null;
        return new HeadlessStatsSummary(n, mean, sd, gv, durationDays, timeActivePercent, estA1C, gmi);
    }

    // Internal helper to reuse existing native history without duplicating code
    private static final class HeadlessHistoryAccessor {
        static long[] getAll() { return Natives.getlastGlucose(); }
        static long[] filter(long[] flat, Long startMillis, Long endMillis) {
            if (flat == null) return null;
            if (startMillis == null && endMillis == null) return flat;
            int totalPairs = flat.length / 2;
            int count = 0;
            for (int i = 0; i < totalPairs; i++) {
                long t = flat[2 * i];
                if (startMillis != null && t < startMillis) continue;
                if (endMillis != null && t > endMillis) continue;
                count += 1;
            }
            long[] out = new long[count * 2];
            int idx = 0;
            for (int i = 0; i < totalPairs; i++) {
                long t = flat[2 * i];
                if (startMillis != null && t < startMillis) continue;
                if (endMillis != null && t > endMillis) continue;
                out[idx++] = t;
                out[idx++] = flat[2 * i + 1];
            }
            return out;
        }
    }
}


