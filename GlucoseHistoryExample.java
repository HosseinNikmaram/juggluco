import tk.glucodata.headless.HeadlessHistory;
import tk.glucodata.headless.UsageExample;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Comprehensive example showing how to get all glucose history
 * This demonstrates the best practices for retrieving complete glucose data
 */
public class GlucoseHistoryExample {
    
    private static final String TAG = "GlucoseHistoryExample";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) {
        // Example usage of the improved glucose history functionality
        demonstrateGlucoseHistory();
    }
    
    /**
     * Main demonstration method showing all approaches
     */
    public static void demonstrateGlucoseHistory() {
        System.out.println("=== Glucose History Retrieval Examples ===\n");
        
        // Example 1: Get complete history as objects (most user-friendly)
        demonstrateCompleteHistory();
        
        // Example 2: Get history as flat array (most efficient)
        demonstrateFlatHistory();
        
        // Example 3: Get history within time range
        demonstrateTimeRangeHistory();
        
        // Example 4: Process all data efficiently
        demonstrateBulkProcessing();
        
        System.out.println("=== End Examples ===");
    }
    
    /**
     * Example 1: Get complete history as GlucoseData objects
     */
    private static void demonstrateCompleteHistory() {
        System.out.println("1. Getting Complete History as Objects:");
        System.out.println("----------------------------------------");
        
        try {
            // Get all glucose history
            List<HeadlessHistory.GlucoseData> history = HeadlessHistory.getCompleteGlucoseHistory("SENSOR123");
            
            if (history.isEmpty()) {
                System.out.println("   No glucose data available");
                return;
            }
            
            System.out.println("   Retrieved " + history.size() + " glucose readings");
            
            // Show first and last readings
            if (history.size() > 0) {
                HeadlessHistory.GlucoseData first = history.get(0);
                HeadlessHistory.GlucoseData last = history.get(history.size() - 1);
                
                String firstTime = sdf.format(new Date(first.timeMillis));
                String lastTime = sdf.format(new Date(last.timeMillis));
                
                System.out.println("   First reading: " + firstTime + " - " + 
                                 first.mgdl + " mg/dL (" + String.format("%.1f", first.mmolL) + " mmol/L)" +
                                 ", Rate: " + String.format("%.1f", first.rate) + ", Alarm: " + first.alarm);
                System.out.println("   Last reading:  " + lastTime + " - " + 
                                 last.mgdl + " mg/dL (" + String.format("%.1f", last.mmolL) + " mmol/L)" +
                                 ", Rate: " + String.format("%.1f", last.rate) + ", Alarm: " + last.alarm);
            }
            
        } catch (Exception e) {
            System.err.println("   Error getting complete history: " + e.getMessage());
        }
        System.out.println();
    }
    
    /**
     * Example 2: Get history as flat array (most efficient)
     */
    private static void demonstrateFlatHistory() {
        System.out.println("2. Getting History as Flat Array (Most Efficient):");
        System.out.println("-------------------------------------------------");
        
        try {
            long[] flatData = HeadlessHistory.getGlucoseHistoryFlat();
            
            if (flatData == null || flatData.length < 2) {
                System.out.println("   No flat data available");
                return;
            }
            
            int numReadings = flatData.length / 2;
            System.out.println("   Flat history contains " + numReadings + " readings");
            
            // Show first few readings
            int showCount = Math.min(3, numReadings);
            for (int i = 0; i < showCount; i++) {
                long timeSeconds = flatData[i * 2];
                long packedGlucose = flatData[i * 2 + 1];
                
                // Extract all information from packed data
                int mgdl = (int) (packedGlucose & 0xFFFFFFFFL);
                short rateRaw = (short) ((packedGlucose >> 32) & 0xFFFFL);
                int alarm = (int) ((packedGlucose >> 48) & 0xFFL);
                
                float rate = rateRaw / 1000.0f;
                float mmolL = mgdl / 18.0f;
                
                String timeStr = sdf.format(new Date(timeSeconds * 1000L));
                
                System.out.println("   Reading " + (i + 1) + ": " + timeStr + 
                                 " - " + mgdl + " mg/dL (" + String.format("%.1f", mmolL) + " mmol/L)" +
                                 ", Rate: " + String.format("%.1f", rate) + 
                                 ", Alarm: " + alarm);
            }
            
            if (numReadings > showCount) {
                System.out.println("   ... and " + (numReadings - showCount) + " more readings");
            }
            
        } catch (Exception e) {
            System.err.println("   Error getting flat history: " + e.getMessage());
        }
        System.out.println();
    }
    
    /**
     * Example 3: Get history within a time range
     */
    private static void demonstrateTimeRangeHistory() {
        System.out.println("3. Getting History Within Time Range:");
        System.out.println("-------------------------------------");
        
        try {
            // Get history for last 24 hours
            long now = System.currentTimeMillis();
            long yesterday = now - (24 * 60 * 60 * 1000L);
            
            List<HeadlessHistory.GlucoseData> recentHistory = 
                HeadlessHistory.getGlucoseHistoryInRange("SENSOR123", yesterday, now);
            
            System.out.println("   Last 24 hours: " + recentHistory.size() + " readings");
            
            if (!recentHistory.isEmpty()) {
                // Calculate average glucose for the period
                double sum = 0;
                int validCount = 0;
                
                for (HeadlessHistory.GlucoseData data : recentHistory) {
                    if (data.mgdl > 0) {
                        sum += data.mgdl;
                        validCount++;
                    }
                }
                
                if (validCount > 0) {
                    double average = sum / validCount;
                    System.out.println("   Average glucose (24h): " + String.format("%.1f", average) + " mg/dL");
                }
            }
            
        } catch (Exception e) {
            System.err.println("   Error getting time range history: " + e.getMessage());
        }
        System.out.println();
    }
    
    /**
     * Example 4: Efficient bulk processing
     */
    private static void demonstrateBulkProcessing() {
        System.out.println("4. Efficient Bulk Processing:");
        System.out.println("-----------------------------");
        
        try {
            long[] flatData = HeadlessHistory.getGlucoseHistoryFlat();
            
            if (flatData == null || flatData.length < 2) {
                System.out.println("   No data available for bulk processing");
                return;
            }
            
            int numReadings = flatData.length / 2;
            System.out.println("   Processing " + numReadings + " readings efficiently...");
            
            // Process all data in a single pass
            int totalGlucose = 0;
            int validReadings = 0;
            int highReadings = 0;
            int lowReadings = 0;
            int inRangeReadings = 0;
            
            for (int i = 0; i < numReadings; i++) {
                long packedGlucose = flatData[i * 2 + 1];
                
                if (packedGlucose != 0) {
                    int mgdl = (int) (packedGlucose & 0xFFFFFFFFL);
                    
                    if (mgdl > 0) {
                        totalGlucose += mgdl;
                        validReadings++;
                        
                        // Categorize readings
                        if (mgdl > 180) {
                            highReadings++;
                        } else if (mgdl < 70) {
                            lowReadings++;
                        } else {
                            inRangeReadings++;
                        }
                    }
                }
            }
            
            if (validReadings > 0) {
                double average = (double) totalGlucose / validReadings;
                System.out.println("   Statistics:");
                System.out.println("     Total readings: " + validReadings);
                System.out.println("     Average glucose: " + String.format("%.1f", average) + " mg/dL");
                System.out.println("     High readings (>180): " + highReadings + " (" + 
                                 String.format("%.1f", (highReadings * 100.0 / validReadings)) + "%)");
                System.out.println("     Low readings (<70): " + lowReadings + " (" + 
                                 String.format("%.1f", (lowReadings * 100.0 / validReadings)) + "%)");
                System.out.println("     In range (70-180): " + inRangeReadings + " (" + 
                                 String.format("%.1f", (inRangeReadings * 100.0 / validReadings)) + "%)");
            }
            
        } catch (Exception e) {
            System.err.println("   Error in bulk processing: " + e.getMessage());
        }
        System.out.println();
    }
    
    /**
     * Example showing the difference between last reading vs all history
     */
    public static void showLastVsAllDifference() {
        System.out.println("=== Last Reading vs All History ===");
        
        // ❌ WRONG - Only gets last reading
        System.out.println("❌ WRONG - Only gets last reading:");
        System.out.println("   var gl = Natives.getlastGlucose();");
        System.out.println("   // This only returns 2 elements: [timestamp, glucose_value]");
        System.out.println("   // You get ONLY the most recent reading, not the complete history");
        System.out.println();
        
        // ✅ CORRECT - Gets all history
        System.out.println("✅ CORRECT - Gets all history:");
        System.out.println("   List<GlucoseData> allHistory = HeadlessHistory.getCompleteGlucoseHistory(serial);");
        System.out.println("   // This returns ALL glucose readings as objects");
        System.out.println();
        System.out.println("   long[] allData = HeadlessHistory.getGlucoseHistoryFlat();");
        System.out.println("   // This returns ALL glucose readings as efficient flat array");
        System.out.println("   // Format: [timestamp1, glucose1, timestamp2, glucose2, ...]");
        System.out.println();
        
        System.out.println("Key Differences:");
        System.out.println("  • getlastGlucose(): Returns 2 elements (last reading only)");
        System.out.println("  • getAllGlucoseHistory(): Returns ALL readings as objects");
        System.out.println("  • getGlucoseHistoryFlat(): Returns ALL readings as efficient array");
        System.out.println();
    }
}