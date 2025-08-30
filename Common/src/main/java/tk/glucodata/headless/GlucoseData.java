package tk.glucodata.headless;

import android.util.Log;
import tk.glucodata.Natives;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single glucose reading with all associated data
 */
public class GlucoseData {
    public final long timeMillis;
    public final int mgdl;
    public final float mmolL;
    public final Float rate;
    public final Integer alarm;
    
    /**
     * Constructor for glucose data with rate and alarm
     */
    public GlucoseData(int mgdl, float mmolL, long timeMillis, Float rate, Integer alarm) {
        this.timeMillis = timeMillis;
        this.mgdl = mgdl;
        this.mmolL = mmolL;
        this.rate = rate;
        this.alarm = alarm;
    }
    
    /**
     * Constructor for glucose data without rate and alarm
     */
    public GlucoseData(int mgdl, float mmolL, long timeMillis) {
        this(mgdl, mmolL, timeMillis, null, null);
    }
    
    @Override
    public String toString() {
        return String.format("GlucoseData{time=%d, mgdl=%d, mmolL=%.1f, rate=%s, alarm=%s}", 
            timeMillis, mgdl, mmolL, rate, alarm);
    }
    
    /**
     * Static utility method to get complete glucose history for a sensor
     * @param serial Sensor serial number
     * @return List of GlucoseData objects, empty list if no data or error
     */
    public static List<GlucoseData> getCompleteGlucoseHistory(String serial) {
        List<GlucoseData> history = new ArrayList<>();
        
        // Validate input parameter
        if (serial == null || serial.trim().isEmpty()) {
            Log.e("GlucoseData", "Serial number is null or empty");
            return history;
        }

        try {
            // Get sensor pointer with null check
            long sensorPtr = Natives.getdataptr(serial);
            if (sensorPtr == 0) {
                Log.w("GlucoseData", "No sensor data found for serial: " + serial);
                return history;
            }

            // Add safety counter to prevent infinite loops
            int maxIterations = 10000; // Adjust this value based on expected data size
            int iterationCount = 0;
            
            // Iterate through all glucose readings
            int pos = 0;
            while (iterationCount < maxIterations) {
                iterationCount++;
                
                try {
                    long timeValue = Natives.streamfromSensorptr(sensorPtr, pos);
                    
                    // Check if we've reached the end
                    if ((timeValue >> 48) == 0) {
                        break;
                    }

                    // Extract data from the packed value with bounds checking
                    long timeSeconds = timeValue & 0xFFFFFFFFL;
                    int mgdl = (int) ((timeValue >> 32) & 0xFFFFL);
                    int nextPos = (int) ((timeValue >> 48) & 0xFFFFL);

                    // Validate extracted data
                    if (mgdl > 0 && mgdl <= 1000 && timeSeconds > 0 && timeSeconds < System.currentTimeMillis() / 1000) {
                        long timeMillis = timeSeconds * 1000L;
                        float mmolL = mgdl / 18.0f;

                        // Create GlucoseData object
                        GlucoseData glucoseData = new GlucoseData(mgdl, mmolL, timeMillis);
                        history.add(glucoseData);
                    }

                    // Move to next position with safety checks
                    if (nextPos <= pos || nextPos > 65535) {
                        Log.w("GlucoseData", "Invalid next position: " + nextPos + ", current: " + pos);
                        break; // Prevent infinite loop
                    }
                    pos = nextPos;
                    
                } catch (Exception e) {
                    Log.e("GlucoseData", "Error reading data at position " + pos, e);
                    break; // Exit loop on error
                }
            }
            
            if (iterationCount >= maxIterations) {
                Log.w("GlucoseData", "Reached maximum iterations, possible infinite loop detected");
            }
            
        } catch (OutOfMemoryError e) {
            Log.e("GlucoseData", "Out of memory while reading glucose history", e);
            System.gc(); // Request garbage collection
        } catch (Exception e) {
            Log.e("GlucoseData", "Error reading glucose history for serial: " + serial, e);
        }

        return history;
    }
}