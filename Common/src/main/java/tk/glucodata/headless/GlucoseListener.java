/* Minimal listener interface for headless glucose updates */
package tk.glucodata.headless;

public interface GlucoseListener {
    void onGlucose(String serial,
                   int mgdl,
                   float value,
                   float rate,
                   int alarm,
                   long timeMillis,
                   long sensorStartMillis,
                   int sensorGen);
}


