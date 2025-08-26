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
        int pairs = flat.length / 2;
        long[][] hist = new long[pairs][2];
        for (int i = 0; i < pairs; i++) {
            hist[i][0] = flat[2 * i];     // timeMillis
            hist[i][1] = flat[2 * i + 1]; // mgdl
        }
        listener.onHistory(serial, hist);
    }
}


