/* Minimal listener interface for headless history snapshots */
package tk.glucodata.headless;

public interface HistoryListener {
    // history: array of [timeMillis, mgdl]
    void onHistory(String serial, long[][] history);
}


