package tk.glucodata.headless;

import tk.glucodata.Natives;

public final class HeadlessStats {
    private final StatsListener listener;

    public HeadlessStats(StatsListener listener) {
        this.listener = listener;
    }

    public void emitIfReady(String serial) {
        if (listener == null) return;
        if (Natives.makepercentages()) {
            // In app they render UI and call Stats.mkstats(act). Here just notify listener.
            listener.onStats(serial, /* placeholder summary */ Boolean.TRUE);
        }
    }
}


