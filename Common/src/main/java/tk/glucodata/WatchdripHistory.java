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

package tk.glucodata;

import static tk.glucodata.Log.doLog;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Utility class for accessing watchdrip glucose history functionality.
 * This class provides a clean interface to the watchdrip history system
 * and integrates with the existing glucose data flow.
 */
public class WatchdripHistory {
    private static final String LOG_ID = "WatchdripHistory";
    
    /**
     * Get all glucose history for a specific sensor
     * @param serial Sensor serial number
     * @return List of glucose readings, empty list if none available
     */
    public static List<watchdrip.GlucoseReading> getHistory(String serial) {
        try {
            return watchdrip.getGlucoseHistory(serial);
        } catch (Exception e) {
            if(doLog) Log.e(LOG_ID, "Error getting history for " + serial + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get glucose history for a specific sensor within a time range
     * @param serial Sensor serial number
     * @param startMillis Start time in milliseconds
     * @param endMillis End time in milliseconds
     * @return List of glucose readings within the time range
     */
    public static List<watchdrip.GlucoseReading> getHistory(String serial, long startMillis, long endMillis) {
        try {
            return watchdrip.getGlucoseHistory(serial, startMillis, endMillis);
        } catch (Exception e) {
            if(doLog) Log.e(LOG_ID, "Error getting history for " + serial + " in range: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get the latest glucose reading for a sensor
     * @param serial Sensor serial number
     * @return Latest glucose reading or null if none available
     */
    public static watchdrip.GlucoseReading getLatest(String serial) {
        try {
            return watchdrip.getLatestGlucose(serial);
        } catch (Exception e) {
            if(doLog) Log.e(LOG_ID, "Error getting latest glucose for " + serial + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get glucose history for the last N hours
     * @param serial Sensor serial number
     * @param hours Number of hours to look back
     * @return List of glucose readings from the last N hours
     */
    public static List<watchdrip.GlucoseReading> getRecentHistory(String serial, int hours) {
        try {
            long endMillis = System.currentTimeMillis();
            long startMillis = endMillis - (hours * 60 * 60 * 1000L);
            return watchdrip.getGlucoseHistory(serial, startMillis, endMillis);
        } catch (Exception e) {
            if(doLog) Log.e(LOG_ID, "Error getting recent history for " + serial + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get glucose history for the last 24 hours
     * @param serial Sensor serial number
     * @return List of glucose readings from the last 24 hours
     */
    public static List<watchdrip.GlucoseReading> getDailyHistory(String serial) {
        return getRecentHistory(serial, 24);
    }
    
    /**
     * Get glucose history for the last 7 days
     * @param serial Sensor serial number
     * @return List of glucose readings from the last 7 days
     */
    public static List<watchdrip.GlucoseReading> getWeeklyHistory(String serial) {
        return getRecentHistory(serial, 24 * 7);
    }
    
    /**
     * Get sorted glucose history (oldest first)
     * @param serial Sensor serial number
     * @return Sorted list of glucose readings
     */
    public static List<watchdrip.GlucoseReading> getSortedHistory(String serial) {
        List<watchdrip.GlucoseReading> history = getHistory(serial);
        Collections.sort(history, Comparator.comparingLong(r -> r.timeMillis));
        return history;
    }
    
    /**
     * Get sorted glucose history within a time range (oldest first)
     * @param serial Sensor serial number
     * @param startMillis Start time in milliseconds
     * @param endMillis End time in milliseconds
     * @return Sorted list of glucose readings within the time range
     */
    public static List<watchdrip.GlucoseReading> getSortedHistory(String serial, long startMillis, long endMillis) {
        List<watchdrip.GlucoseReading> history = getHistory(serial, startMillis, endMillis);
        Collections.sort(history, Comparator.comparingLong(r -> r.timeMillis));
        return history;
    }
    
    /**
     * Get glucose statistics for a sensor
     * @param serial Sensor serial number
     * @return Glucose statistics object
     */
    public static GlucoseStats getStats(String serial) {
        List<watchdrip.GlucoseReading> history = getHistory(serial);
        if (history.isEmpty()) {
            return new GlucoseStats();
        }
        
        return new GlucoseStats(history);
    }
    
    /**
     * Get glucose statistics for a sensor within a time range
     * @param serial Sensor serial number
     * @param startMillis Start time in milliseconds
     * @param endMillis End time in milliseconds
     * @return Glucose statistics object
     */
    public static GlucoseStats getStats(String serial, long startMillis, long endMillis) {
        List<watchdrip.GlucoseReading> history = getHistory(serial, startMillis, endMillis);
        if (history.isEmpty()) {
            return new GlucoseStats();
        }
        
        return new GlucoseStats(history);
    }
    
    /**
     * Check if a sensor has any glucose history
     * @param serial Sensor serial number
     * @return true if history exists, false otherwise
     */
    public static boolean hasHistory(String serial) {
        try {
            List<watchdrip.GlucoseReading> history = watchdrip.getGlucoseHistory(serial);
            return history != null && !history.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the number of glucose readings for a sensor
     * @param serial Sensor serial number
     * @return Number of glucose readings
     */
    public static int getHistoryCount(String serial) {
        try {
            List<watchdrip.GlucoseReading> history = watchdrip.getGlucoseHistory(serial);
            return history != null ? history.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Glucose statistics class
     */
    public static class GlucoseStats {
        public final int count;
        public final float minValue;
        public final float maxValue;
        public final float avgValue;
        public final float minRate;
        public final float maxRate;
        public final float avgRate;
        public final int lowAlarmCount;
        public final int highAlarmCount;
        public final int normalCount;
        
        public GlucoseStats() {
            this.count = 0;
            this.minValue = 0;
            this.maxValue = 0;
            this.avgValue = 0;
            this.minRate = 0;
            this.maxRate = 0;
            this.avgRate = 0;
            this.lowAlarmCount = 0;
            this.highAlarmCount = 0;
            this.normalCount = 0;
        }
        
        public GlucoseStats(List<watchdrip.GlucoseReading> history) {
            this.count = history.size();
            
            if (count == 0) {
                this.minValue = 0;
                this.maxValue = 0;
                this.avgValue = 0;
                this.minRate = 0;
                this.maxRate = 0;
                this.avgRate = 0;
                this.lowAlarmCount = 0;
                this.highAlarmCount = 0;
                this.normalCount = 0;
                return;
            }
            
            float sumValue = 0;
            float sumRate = 0;
            float minVal = Float.MAX_VALUE;
            float maxVal = Float.MIN_VALUE;
            float minRateVal = Float.MAX_VALUE;
            float maxRateVal = Float.MIN_VALUE;
            int lowCount = 0;
            int highCount = 0;
            int normalCount = 0;
            
            for (watchdrip.GlucoseReading reading : history) {
                // Value statistics
                sumValue += reading.value;
                if (reading.value < minVal) minVal = reading.value;
                if (reading.value > maxVal) maxVal = reading.value;
                
                // Rate statistics
                sumRate += reading.rate;
                if (reading.rate < minRateVal) minRateVal = reading.rate;
                if (reading.rate > maxRateVal) maxRateVal = reading.rate;
                
                // Alarm statistics
                if ((reading.alarm & 4) == 4) {
                    if ((reading.alarm & 1) == 1) {
                        lowCount++;
                    } else {
                        highCount++;
                    }
                } else {
                    normalCount++;
                }
            }
            
            this.minValue = minVal;
            this.maxValue = maxVal;
            this.avgValue = sumValue / count;
            this.minRate = minRateVal;
            this.maxRate = maxRateVal;
            this.avgRate = sumRate / count;
            this.lowAlarmCount = lowCount;
            this.highAlarmCount = highCount;
            this.normalCount = normalCount;
        }
        
        @Override
        public String toString() {
            return String.format("GlucoseStats{count=%d, value[%.1f-%.1f avg=%.1f], rate[%.2f-%.2f avg=%.2f], alarms[low=%d, high=%d, normal=%d]}",
                    count, minValue, maxValue, avgValue, minRate, maxRate, avgRate, lowAlarmCount, highAlarmCount, normalCount);
        }
    }
}