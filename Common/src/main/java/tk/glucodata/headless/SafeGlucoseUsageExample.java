package tk.glucodata.headless;

import android.util.Log;
import java.util.List;

/**
 * Example demonstrating safe usage of glucose history methods
 * This shows how to properly handle the getAllGlucoseHistory method
 * with comprehensive error handling and null checks
 */
public class SafeGlucoseUsageExample {
    private static final String TAG = "SafeGlucoseExample";
    private HeadlessJugglucoManager jugglucoManager;
    
    public SafeGlucoseUsageExample() {
        jugglucoManager = new HeadlessJugglucoManager();
    }
    
    /**
     * Safe method to retrieve and process glucose history
     */
    public void safelyGetGlucoseHistory(String serial) {
        // Method 1: Using the instance method from HeadlessJugglucoManager
        new Thread(() -> {
            try {
                // Add null check for jugglucoManager
                if (jugglucoManager == null) {
                    Log.e(TAG, "jugglucoManager is null");
                    return;
                }
                
                // Add null check for serial
                if (serial == null || serial.trim().isEmpty()) {
                    Log.e(TAG, "Serial number is null or empty");
                    return;
                }
                
                // Get glucose history with error handling
                List<GlucoseData> glucoseHistory = jugglucoManager.getAllGlucoseHistory(serial);
                
                // Check if history is null or empty
                if (glucoseHistory == null) {
                    Log.w(TAG, "Glucose history is null for serial: " + serial);
                    return;
                }
                
                if (glucoseHistory.isEmpty()) {
                    Log.i(TAG, "No glucose data found for serial: " + serial);
                    return;
                }
                
                // Process each glucose data entry with null checks
                for (GlucoseData glucoseData : glucoseHistory) {
                    if (glucoseData != null) {
                        try {
                            Log.d(TAG, String.format(
                                "Time: %s, Glucose: %d mg/dL (%.1f mmol/L), Rate: %s, Alarm: %s",
                                glucoseData.timeMillis,
                                glucoseData.mgdl,
                                glucoseData.mmolL,
                                glucoseData.rate != null ? String.format("%.3f", glucoseData.rate) : "N/A",
                                glucoseData.alarm != null ? glucoseData.alarm.toString() : "N/A"
                            ));
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing glucose data: " + glucoseData, e);
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in glucose history thread", e);
            }
        }).start();
    }
    
    /**
     * Alternative method using the static utility method
     */
    public void safelyGetGlucoseHistoryStatic(String serial) {
        new Thread(() -> {
            try {
                // Add null check for serial
                if (serial == null || serial.trim().isEmpty()) {
                    Log.e(TAG, "Serial number is null or empty");
                    return;
                }
                
                // Get glucose history using static method
                List<GlucoseData> glucoseHistory = GlucoseData.getCompleteGlucoseHistory(serial);
                
                // Check if history is null or empty
                if (glucoseHistory == null) {
                    Log.w(TAG, "Glucose history is null for serial: " + serial);
                    return;
                }
                
                if (glucoseHistory.isEmpty()) {
                    Log.i(TAG, "No glucose data found for serial: " + serial);
                    return;
                }
                
                // Process each glucose data entry with null checks
                for (GlucoseData glucoseData : glucoseHistory) {
                    if (glucoseData != null) {
                        try {
                            Log.d(TAG, String.format(
                                "Time: %s, Glucose: %d mg/dL (%.1f mmol/L), Rate: %s, Alarm: %s",
                                glucoseData.timeMillis,
                                glucoseData.mgdl,
                                glucoseData.mmolL,
                                glucoseData.rate != null ? String.format("%.3f", glucoseData.rate) : "N/A",
                                glucoseData.alarm != null ? glucoseData.alarm.toString() : "N/A"
                            ));
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing glucose data: " + glucoseData, e);
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in glucose history thread", e);
            }
        }).start();
    }
    
    /**
     * Method with timeout protection to prevent hanging
     */
    public void safelyGetGlucoseHistoryWithTimeout(String serial) {
        new Thread(() -> {
            try {
                // Set a timeout for the operation
                long startTime = System.currentTimeMillis();
                long timeout = 30000; // 30 seconds timeout
                
                // Add null check for jugglucoManager
                if (jugglucoManager == null) {
                    Log.e(TAG, "jugglucoManager is null");
                    return;
                }
                
                // Add null check for serial
                if (serial == null || serial.trim().isEmpty()) {
                    Log.e(TAG, "Serial number is null or empty");
                    return;
                }
                
                // Get glucose history with error handling
                List<GlucoseData> glucoseHistory = jugglucoManager.getAllGlucoseHistory(serial);
                
                // Check timeout
                if (System.currentTimeMillis() - startTime > timeout) {
                    Log.w(TAG, "Glucose history operation timed out");
                    return;
                }
                
                // Check if history is null or empty
                if (glucoseHistory == null) {
                    Log.w(TAG, "Glucose history is null for serial: " + serial);
                    return;
                }
                
                if (glucoseHistory.isEmpty()) {
                    Log.i(TAG, "No glucose data found for serial: " + serial);
                    return;
                }
                
                // Process each glucose data entry with null checks
                for (GlucoseData glucoseData : glucoseHistory) {
                    if (glucoseData != null) {
                        try {
                            Log.d(TAG, String.format(
                                "Time: %s, Glucose: %d mg/dL (%.1f mmol/L), Rate: %s, Alarm: %s",
                                glucoseData.timeMillis,
                                glucoseData.mgdl,
                                glucoseData.mmolL,
                                glucoseData.rate != null ? String.format("%.3f", glucoseData.rate) : "N/A",
                                glucoseData.alarm != null ? glucoseData.alarm.toString() : "N/A"
                            ));
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing glucose data: " + glucoseData, e);
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in glucose history thread", e);
            }
        }).start();
    }
    
    /**
     * Method to validate sensor before attempting to get history
     */
    public void safelyGetGlucoseHistoryWithValidation(String serial) {
        new Thread(() -> {
            try {
                // Add null check for jugglucoManager
                if (jugglucoManager == null) {
                    Log.e(TAG, "jugglucoManager is null");
                    return;
                }
                
                // Add null check for serial
                if (serial == null || serial.trim().isEmpty()) {
                    Log.e(TAG, "Serial number is null or empty");
                    return;
                }
                
                // Check if Bluetooth streaming is active (optional validation)
                if (!jugglucoManager.isBluetoothStreamingActive()) {
                    Log.w(TAG, "Bluetooth streaming is not active for serial: " + serial);
                    // You might want to start it here or just continue
                }
                
                // Get glucose history with error handling
                List<GlucoseData> glucoseHistory = jugglucoManager.getAllGlucoseHistory(serial);
                
                // Check if history is null or empty
                if (glucoseHistory == null) {
                    Log.w(TAG, "Glucose history is null for serial: " + serial);
                    return;
                }
                
                if (glucoseHistory.isEmpty()) {
                    Log.i(TAG, "No glucose data found for serial: " + serial);
                    return;
                }
                
                // Process each glucose data entry with null checks
                for (GlucoseData glucoseData : glucoseHistory) {
                    if (glucoseData != null) {
                        try {
                            Log.d(TAG, String.format(
                                "Time: %s, Glucose: %d mg/dL (%.1f mmol/L), Rate: %s, Alarm: %s",
                                glucoseData.timeMillis,
                                glucoseData.mgdl,
                                glucoseData.mmolL,
                                glucoseData.rate != null ? String.format("%.3f", glucoseData.rate) : "N/A",
                                glucoseData.alarm != null ? glucoseData.alarm.toString() : "N/A"
                            ));
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing glucose data: " + glucoseData, e);
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in glucose history thread", e);
            }
        }).start();
    }
}