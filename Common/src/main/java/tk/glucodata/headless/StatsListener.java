/* Minimal listener interface for headless stats */
package tk.glucodata.headless;

public interface StatsListener {
    void onStats(String serial, HeadlessStatsSummary stats);
}



