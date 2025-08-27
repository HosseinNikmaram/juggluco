package tk.glucodata.headless;

import android.util.Log;
import tk.glucodata.Natives;

public final class HeadlessStats {
    private static final String TAG = "HeadlessStats";
    private final StatsListener listener;

    public HeadlessStats(StatsListener listener) {
        this.listener = listener;
    }

    public void emitIfReady(String serial) {
        if (listener == null) return;
        if (!Natives.makepercentages()) return;
        var hist = HeadlessHistoryAccessor.getAll();
        if (hist == null || hist.length < 2) return;
        HeadlessStatsSummary s = computeSummary(hist);
        Log.d(TAG, "Stats serial=" + serial
                + " n=" + s.numberOfMeasurements
                + " mean=" + String.format("%.1f", s.averageGlucose)
                + " sd=" + String.format("%.1f", s.standardDeviation)
                + " GV%=" + String.format("%.1f", s.glucoseVariabilityPercent)
                + " days=" + String.format("%.2f", s.durationDays)
                + " active%=" + String.format("%.1f", s.timeActivePercent)
                + " TIR <70=" + String.format("%.1f", s.percentBelow70)
                + "% 70-180=" + String.format("%.1f", s.percent70to180)
                + "% 181-250=" + String.format("%.1f", s.percent181to250)
                + "% >250=" + String.format("%.1f", s.percentAbove250) + "%");
        listener.onStats(serial, s);
    }

    public void emitIfReady(String serial, Long startMillis, Long endMillis) {
        if (listener == null) return;
        if (!Natives.makepercentages()) return;
        var hist = HeadlessHistoryAccessor.getAll();
        if (hist == null || hist.length < 2) return;
        var ranged = HeadlessHistoryAccessor.filter(hist, startMillis, endMillis);
        if (ranged == null || ranged.length < 2) return;
        HeadlessStatsSummary s = computeSummary(ranged);
        Log.d(TAG, "Stats(serial=" + serial + ") range: n=" + s.numberOfMeasurements
                + " mean=" + String.format("%.1f", s.averageGlucose)
                + " sd=" + String.format("%.1f", s.standardDeviation)
                + " GV%=" + String.format("%.1f", s.glucoseVariabilityPercent)
                + " days=" + String.format("%.2f", s.durationDays)
                + " active%=" + String.format("%.1f", s.timeActivePercent)
                + " TIR <70=" + String.format("%.1f", s.percentBelow70)
                + "% 70-180=" + String.format("%.1f", s.percent70to180)
                + "% 181-250=" + String.format("%.1f", s.percent181to250)
                + "% >250=" + String.format("%.1f", s.percentAbove250) + "%");
        listener.onStats(serial, s);
    }

    private static HeadlessStatsSummary computeSummary(long[] flat) {
        int n = flat.length / 2;
        if (n == 0) return new HeadlessStatsSummary(0, 0, 0, 0, 0, 0, null, null, 0, 0, 0, 0);
        long firstMillis = flat[0] * 1000L;
        long lastMillis = flat[(n - 1) * 2] * 1000L;
        double sum = 0.0;
        double sumSq = 0.0;
        int countBelow70 = 0;
        int count70to180 = 0;
        int count181to250 = 0;
        int countAbove250 = 0;
        for (int i = 0; i < n; i++) {
            long packed = flat[2 * i + 1];
            // Decode Q32.32 mmol/L to mg/dL
            double mmolL = (double) packed / 4294967296.0;
            double g = mmolL * 18.0;
            sum += g;
            sumSq += g * g;
            if (g < 70.0) {
                countBelow70++;
            } else if (g <= 180.0) {
                count70to180++;
            } else if (g <= 250.0) {
                count181to250++;
            } else {
                countAbove250++;
            }
        }
        double mean = sum / n;
        double variance = Math.max(0.0, (sumSq / n) - (mean * mean));
        double sd = Math.sqrt(variance);
        double gv = mean > 0 ? (sd * 100.0 / mean) : 0.0;
        double durationDays = (lastMillis > firstMillis) ? (lastMillis - firstMillis) / 86_400_000.0 : 0.0;
        // Rough time active based on expected 5-min intervals (Libre3 history)
        double expected = durationDays > 0 ? (durationDays * 24 * 12) : n;
        double timeActivePercent = expected > 0 ? Math.min(100.0, n * 100.0 / expected) : 0.0;
        // Simple A1C/GMI estimates from mean mg/dL
        Double estA1C = (mean > 0) ? ((mean + 46.7) / 28.7) : null; // NGSP %
        Double gmi = (mean > 0) ? (3.31 + 0.02392 * mean) : null;
        double pBelow70 = n > 0 ? (countBelow70 * 100.0 / n) : 0.0;
        double p70to180 = n > 0 ? (count70to180 * 100.0 / n) : 0.0;
        double p181to250 = n > 0 ? (count181to250 * 100.0 / n) : 0.0;
        double pAbove250 = n > 0 ? (countAbove250 * 100.0 / n) : 0.0;
        return new HeadlessStatsSummary(n, mean, sd, gv, durationDays, timeActivePercent, estA1C, gmi,
                pBelow70, p70to180, p181to250, pAbove250);
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


