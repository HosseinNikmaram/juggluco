/*      This file is part of Juggluco, an Android app to receive and display         */
/*      glucose values from Freestyle Libre 2 and 3 sensors.                         */
/*                                                                                   */
/*      Copyright (C) 2021 Jaap Korthals Altes <jaapkorthalsaltes@gmail.com>         */
/*                                                                                   */
/*      Juggluco is free software: you can redistribute it and/or modify             */
/*      it under the terms of the GNU General Public License as published            */
/*      by the GNU General Public License Foundation, either version 3 of the License, or         */
/*      (at your option) any later version.                                          */
/*                                                                                   */
/*      Juggluco is distributed in the hope that it will be useful, but              */
/*      WITHOUT ANY WARRANTY; without even the implied warranty of                   */
/*      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         */
/*      See the GNU General Public License for more details.                         */
/*                                                                                   */
/*      You should have received a copy of the GNU General Public License            */
/*      along with Juggluco. If not, see <https://www.gnu.org/licenses/>.            */

package tk.glucodata.headless;

import android.util.Log;

/**
 * Example class demonstrating how to use HeadlessHistory with onGlucose method
 * This shows the proper way to implement glucose history using the watchdrip structure
 */
public class HeadlessHistoryExample {
    private static final String TAG = "HeadlessHistoryExample";
    
    /**
     * Example implementation of GlucoseListener for history data
     */
    public static class ExampleGlucoseListener implements GlucoseListener {
        @Override
        public void onGlucose(String serial,
                             int mgdl,
                             float value,
                             float rate,
                             int alarm,
                             long timeMillis,
                             long sensorStartMillis,
                             int sensorGen) {
            
            // Process each glucose reading
            Log.d(TAG, String.format("Glucose: Serial=%s, mg/dL=%d, value=%.1f, rate=%.2f, alarm=%d, time=%d, sensorStart=%d, gen=%d",
                    serial, mgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen));
            
            // You can add your custom logic here:
            // - Store in database
            // - Send to cloud service
            // - Update UI
            // - Calculate statistics
            // - Trigger alarms
            
            // Example: Check for high/low glucose
            if (mgdl > 180) {
                Log.w(TAG, "High glucose detected: " + mgdl + " mg/dL");
            } else if (mgdl < 70) {
                Log.w(TAG, "Low glucose detected: " + mgdl + " mg/dL");
            }
            
            // Example: Check rate of change
            if (Math.abs(rate) > 2.0) {
                Log.w(TAG, "Rapid glucose change detected: " + rate + " mg/dL/min");
            }
        }
    }
    
    /**
     * Example of how to set up and use HeadlessHistory
     */
    public static void setupHistoryExample(HeadlessJugglucoManager manager) {
        // Create a glucose listener for history data
        ExampleGlucoseListener glucoseListener = new ExampleGlucoseListener();
        
        // Set the glucose listener for history
        manager.setGlucoseHistoryListener(glucoseListener);
        
        // Now you can get glucose history using onGlucose method
        
        // Get all available history for a sensor
        manager.getGlucoseHistoryOnGlucose("sensor123");
        
        // Get history within a time range (last 24 hours)
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (24 * 60 * 60 * 1000L); // 24 hours ago
        manager.getGlucoseHistoryOnGlucose("sensor123", startTime, endTime);
        
        // Get history from watchdrip storage (if available)
        manager.getGlucoseHistoryFromWatchdrip("sensor123");
        
        // Get watchdrip history within a time range
        manager.getGlucoseHistoryOnGlucoseFromWatchdrip("sensor123", startTime, endTime);
    }
    
    /**
     * Example of how to use HeadlessHistory directly
     */
    public static void useHeadlessHistoryDirectly() {
        // Create a glucose listener
        ExampleGlucoseListener glucoseListener = new ExampleGlucoseListener();
        
        // Create HeadlessHistory with glucose listener
        HeadlessHistory history = new HeadlessHistory(glucoseListener);
        
        // Get history from native data
        history.emitFromNativeLast("sensor123");
        
        // Get history from native data within time range
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (2 * 60 * 60 * 1000L); // 2 hours ago
        history.emitFromNativeRange("sensor123", startTime, endTime);
        
        // Get history from watchdrip storage
        history.emitFromWatchdripHistory("sensor123");
        
        // Get watchdrip history within time range
        history.emitFromWatchdripHistory("sensor123", startTime, endTime);
    }
    
    /**
     * Example of how to handle both history and glucose listeners
     */
    public static void useBothListeners(HeadlessJugglucoManager manager) {
        // Create both listeners
        ExampleGlucoseListener glucoseListener = new ExampleGlucoseListener();
        ExampleHistoryListener historyListener = new ExampleHistoryListener();
        
        // Set both listeners
        manager.setHistoryAndGlucoseListeners(historyListener, glucoseListener);
        
        // Now you can use both methods:
        // Legacy method (HistoryListener)
        manager.getGlucoseHistory("sensor123");
        
        // New method (GlucoseListener with onGlucose)
        manager.getGlucoseHistoryOnGlucose("sensor123");
    }
    
    /**
     * Example implementation of HistoryListener for backward compatibility
     */
    public static class ExampleHistoryListener implements HistoryListener {
        @Override
        public void onHistory(String serial, long[][] pairs) {
            Log.d(TAG, "History received for " + serial + " with " + pairs.length + " readings");
            
            for (int i = 0; i < pairs.length; i++) {
                long timeMillis = pairs[i][0];
                long mgdl = pairs[i][1];
                
                Log.d(TAG, String.format("History entry %d: time=%d, mg/dL=%d", i, timeMillis, mgdl));
            }
        }
    }
}