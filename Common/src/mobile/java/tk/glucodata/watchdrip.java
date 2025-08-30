/*      This file is part of Juggluco, an Android app to receive and display         */
/*      glucose values from Freestyle Libre 2 and 3 sensors.                         */
/*                                                                                   */
/*      Copyright (C) 2021 Jaap Korthals Altes <jaapkorthalsaltes@gmail.com>         */
/*                                                                                   */
/*      Juggluco is free software: you can redistribute it and/or modify             */
/*      it under the terms of the GNU General Public License as published            */
/*      by the Free Software Foundation, either version 3 of the License, or         */
/*      (at your option) any later version.                                          */
/*                                                                                   */
/*      Juggluco is distributed in the hope that it will be useful, but              */
/*      WITHOUT ANY WARRANTY; without even the implied warranty of                   */
/*      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         */
/*      See the GNU General Public License for more details.                         */
/*                                                                                   */
/*      You should have received a copy of the GNU General Public License            */
/*      along with Juggluco. If not, see <https://www.gnu.org/licenses/>.            */
/*                                                                                   */
/*      Thu Mar 23 21:03:49 CET 2023                                                 */


package tk.glucodata;
import static android.content.Context.RECEIVER_EXPORTED;
import static tk.glucodata.Log.doLog;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.services.broadcastservice.models.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tk.glucodata.headless.GlucoseListener;

public class watchdrip extends BroadcastReceiver {
    private static String LOG_ID = "watchdrip";
    
    // History management
    private static final int MAX_HISTORY_SIZE = 1000; // Maximum number of glucose readings to keep in memory
    private static final ConcurrentHashMap<String, List<GlucoseReading>> glucoseHistory = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService historyCleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // Glucose reading data structure matching onGlucose parameters
    public static class GlucoseReading {
        public final String serial;
        public final int mgdl;
        public final float value;
        public final float rate;
        public final int alarm;
        public final long timeMillis;
        public final long sensorStartMillis;
        public final int sensorGen;
        
        public GlucoseReading(String serial, int mgdl, float value, float rate, int alarm, 
                           long timeMillis, long sensorStartMillis, int sensorGen) {
            this.serial = serial;
            this.mgdl = mgdl;
            this.value = value;
            this.rate = rate;
            this.alarm = alarm;
            this.timeMillis = timeMillis;
            this.sensorStartMillis = sensorStartMillis;
            this.sensorGen = sensorGen;
        }
        
        @Override
        public String toString() {
            return String.format("GlucoseReading{serial='%s', mgdl=%d, value=%.1f, rate=%.2f, alarm=%d, time=%d, sensorStart=%d, gen=%d}",
                    serial, mgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen);
        }
    }
    
    // Initialize history cleanup scheduler
    static {
        // Clean up old history entries every hour
        historyCleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupOldHistory();
            } catch (Exception e) {
                if(doLog) Log.e(LOG_ID, "History cleanup error: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    static String tostring(Bundle bundle) {
        if(bundle == null)
            return "";
        var builder = new StringBuilder();
        builder.append("bundle content\n");
        var keys = bundle.keySet();
        for(var key: keys) {
            builder.append("[" + key + "]<->[" + bundle.get(key) + "]\n");
        }
        return builder.toString();
    }
    
    /**
     * Add glucose reading to history
     */
    private static void addToHistory(String serial, int mgdl, float value, float rate, 
                                   int alarm, long timeMillis, long sensorStartMillis, int sensorGen) {
        if (serial == null || serial.isEmpty()) {
            if(doLog) Log.w(LOG_ID, "Cannot add to history: serial is null or empty");
            return;
        }
        
        GlucoseReading reading = new GlucoseReading(serial, mgdl, value, rate, alarm, 
                                                  timeMillis, sensorStartMillis, sensorGen);
        
        glucoseHistory.computeIfAbsent(serial, k -> new ArrayList<>()).add(reading);
        
        // Maintain history size limit
        List<GlucoseReading> history = glucoseHistory.get(serial);
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0); // Remove oldest entry
        }
        
        if(doLog) Log.d(LOG_ID, "Added to history: " + reading);
    }
    
    /**
     * Get glucose history for a specific sensor
     */
    public static List<GlucoseReading> getGlucoseHistory(String serial) {
        if (serial == null || serial.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(glucoseHistory.getOrDefault(serial, new ArrayList<>()));
    }
    
    /**
     * Get glucose history for a specific sensor within a time range
     */
    public static List<GlucoseReading> getGlucoseHistory(String serial, long startMillis, long endMillis) {
        if (serial == null || serial.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<GlucoseReading> allHistory = glucoseHistory.getOrDefault(serial, new ArrayList<>());
        List<GlucoseReading> filteredHistory = new ArrayList<>();
        
        for (GlucoseReading reading : allHistory) {
            if (reading.timeMillis >= startMillis && reading.timeMillis <= endMillis) {
                filteredHistory.add(reading);
            }
        }
        
        return filteredHistory;
    }
    
    /**
     * Get latest glucose reading for a sensor
     */
    public static GlucoseReading getLatestGlucose(String serial) {
        if (serial == null || serial.isEmpty()) {
            return null;
        }
        
        List<GlucoseReading> history = glucoseHistory.get(serial);
        if (history == null || history.isEmpty()) {
            return null;
        }
        
        return history.get(history.size() - 1);
    }
    
    /**
     * Clean up old history entries (older than 24 hours)
     */
    private static void cleanupOldHistory() {
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L); // 24 hours ago
        
        for (String serial : glucoseHistory.keySet()) {
            List<GlucoseReading> history = glucoseHistory.get(serial);
            if (history != null) {
                history.removeIf(reading -> reading.timeMillis < cutoffTime);
                
                // Remove empty history entries
                if (history.isEmpty()) {
                    glucoseHistory.remove(serial);
                }
            }
        }
        
        if(doLog) Log.d(LOG_ID, "History cleanup completed. Current entries: " + glucoseHistory.size());
    }
    
    /**
     * Process glucose data and add to history
     */
    private static void processGlucoseData(String serial, long[] gl) {
        if (gl == null || gl.length < 2) {
            if(doLog) Log.w(LOG_ID, "Invalid glucose data received");
            return;
        }
        
        long res = gl[1];
        int glumgdl = (int) (res & 0xFFFFFFFFL);
        
        if (glumgdl != 0) {
            int alarm = (int) ((res >> 48) & 0xFFL);
            short ratein = (short) ((res >> 32) & 0xFFFFL);
            float rate = ratein / 1000.0f;
            long timeMillis = gl[0] * 1000L; // Convert seconds to milliseconds
            
            // Convert to current units
            float value = Applic.unit == 1 ? glumgdl / Applic.mgdLmult : glumgdl;
            
            // Get sensor information (default values if not available)
            long sensorStartMillis = System.currentTimeMillis() - (24 * 60 * 60 * 1000L); // Default to 24 hours ago
            int sensorGen = 1; // Default sensor generation
            
            // Add to history
            addToHistory(serial, glumgdl, value, rate, alarm, timeMillis, sensorStartMillis, sensorGen);
            
            if(doLog) Log.d(LOG_ID, "Processed glucose data: mgdl=" + glumgdl + ", rate=" + rate + ", alarm=" + alarm);
        }
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        {if(doLog) {Log.i(LOG_ID,"onReceive ");};};
        var extras = intent.getExtras();
        if(doLog) Natives.log(tostring(extras));
        var function = extras.getString("FUNCTION","");
        
        if("update_bg_force".equals(function)) {
            String key = extras.getString("PACKAGE",null);
            if(key == null) {
                Log.e(LOG_ID,"no package");
                return;
            }
            
            Settings settings = extras.getParcelable("SETTINGS");
            if(settings == null) {
                Log.e(LOG_ID,"settings==null");
                return;
            }
            
            WearInt.mapsettings.put(key,settings);
            
            // Get glucose data
            var gl = Natives.getlastGlucose();
            if(gl == null) return;
            
            // Process and add to history
            processGlucoseData(key, gl);
            
            // Extract data for wearable intent
            long res = gl[1];
            int glumgdl = (int) (res & 0xFFFFFFFFL);
            
            if (glumgdl != 0) {
                int alarm = (int) ((res >> 48) & 0xFFL);
                short ratein = (short) ((res >> 32) & 0xFFFFL);
                float rate = ratein / 1000.0f;
                
                var newintent = WearInt.mksendglucoseintent(settings, glumgdl, rate, alarm, gl[0] * 1000L);
                newintent.putExtra("FUNCTION","update_bg_force");
                newintent.setPackage(key);
                Applic.app.sendBroadcast(newintent);
            }
        }
    }
    
    static private watchdrip receiver = null;
    
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    static void register() {
        if(receiver == null)
            receiver = new watchdrip();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Applic.app.registerReceiver(receiver, new IntentFilter("com.eveningoutpost.dexdrip.watch.wearintegration.BROADCAST_SERVICE_RECEIVER"),RECEIVER_EXPORTED);
        }
        else
            Applic.app.registerReceiver(receiver, new IntentFilter("com.eveningoutpost.dexdrip.watch.wearintegration.BROADCAST_SERVICE_RECEIVER"));
    }
    
    static void unregister() {
        if(receiver != null) {
            try {
                Applic.app.unregisterReceiver(receiver);
            } 
            catch(Throwable th) {
                Log.stack(LOG_ID,"unregister",th);
            }
        }
    }

    public static void set(boolean val) {
        SuperGattCallback.doWearInt = val;
        if(val) {
            register();
        }
        else {
            unregister();
        }
    }
    
    /**
     * Shutdown history management
     */
    public static void shutdown() {
        try {
            historyCleanupExecutor.shutdown();
            if (!historyCleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                historyCleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            historyCleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        glucoseHistory.clear();
        if(doLog) Log.d(LOG_ID, "Watchdrip history management shutdown completed");
    }
}
