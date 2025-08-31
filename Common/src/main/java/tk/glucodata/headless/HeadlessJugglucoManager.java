package tk.glucodata.headless;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import tk.glucodata.Natives;
import tk.glucodata.SensorBluetooth;
import tk.glucodata.AlgNfcV;
import android.nfc.Tag;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Main headless manager for Juggluco Libre sensor integration
 * Provides NFC scanning, BLE management, and glucose data access
 */
public class HeadlessJugglucoManager {
    private static final String TAG = "HeadlessHead";
    private static volatile HeadlessJugglucoManager instance;
    
    public static GlucoseListener glucoseListener;
    private DeviceConnectionListener deviceConnectionListener;
    private Activity activity;
    private HeadlessNfcReader nfcReader;
    private HeadlessStats statsManager;
    private static volatile boolean nativesInitialized = false;
    
    /**
     * Get the singleton instance of HeadlessJugglucoManager
     * @return HeadlessJugglucoManager instance
     */
    public static HeadlessJugglucoManager getInstance() {
        if (instance == null) {
            synchronized (HeadlessJugglucoManager.class) {
                if (instance == null) {
                    instance = new HeadlessJugglucoManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor for singleton pattern
     */
    private HeadlessJugglucoManager() {
        // Private constructor
    }
    
    /**
     * Initialize the headless Juggluco system
     * @param ctx Android context
     * @return true if initialization was successful
     */
    public boolean init(Activity ctx) {
        try {
            // Initialize native libraries and core system (idempotent)
            if (!nativesInitialized) {
                Natives.setfilesdir(ctx.getFilesDir().getAbsolutePath(), "IR", ctx.getApplicationInfo().nativeLibraryDir);
                Natives.initjuggluco(ctx.getFilesDir().getAbsolutePath());
                Natives.onCreate();
                nativesInitialized = true;
            }
            Natives.setusebluetooth(true);

            // Register device connection listener if available
            if (deviceConnectionListener != null) {
                registerDeviceConnectionListener();
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check and enable Bluetooth if needed
     * @return true if Bluetooth is available and enabled
     */
    public boolean ensurePermissionsAndBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return false;
        
        if (!adapter.isEnabled()) {
            adapter.enable();
        }
        return true;
    }
    

    /**
     * Set glucose listener for real-time glucose updates
     * @param listener Glucose listener implementation
     */
    public void setGlucoseListener(GlucoseListener listener) {
        glucoseListener = listener;
    }
    
    /**
     * Set device connection listener for Bluetooth connection, pairing, and scanning events
     * @param listener Device connection listener implementation
     */
    public void setDeviceConnectionListener(DeviceConnectionListener listener) {
        this.deviceConnectionListener = listener;
        if (nativesInitialized && listener != null) {
            registerDeviceConnectionListener();
        }
    }
    
    /**
     * Register the device connection listener with SensorBluetooth
     */
    private void registerDeviceConnectionListener() {
        if (deviceConnectionListener != null) {
            try {
                // Register with SensorBluetooth for connection events
                SensorBluetooth.registerDeviceConnectionListener(deviceConnectionListener);
            } catch (Exception e) {
                Log.e(TAG, "Failed to register device connection listener", e);
            }
        }
    }

    /**
     * Set stats listener for glucose statistics
     * @param listener Stats listener implementation
     */
    public void setStatsListener(StatsListener listener) {
        if (listener != null) {
            statsManager = new HeadlessStats(listener);
        }
    }

    /**
     * Configure TIR thresholds (mg/dL). All parameters optional; pass null to keep current.
     * Defaults are 70, 180, 250.
     */
    public void configureTirThresholds(Double low, Double inRangeUpper, Double highUpper) {
        if (statsManager == null) return;
        if (low != null) statsManager.setLowThresholdMgdl(low);
        if (inRangeUpper != null) statsManager.setInRangeUpperThresholdMgdl(inRangeUpper);
        if (highUpper != null) statsManager.setHighUpperThresholdMgdl(highUpper);
    }

    /**
     * Get complete glucose history for a sensor as a list of GlucoseData objects
     * @return List of GlucoseData objects, or empty list if no data
     */
    public List<HeadlessHistory.GlucoseData> getAllGlucoseHistory() {
        return HeadlessHistory.getCompleteGlucoseHistory();
    }


    /**
     * Get glucose history for a sensor within a time range as a list of GlucoseData objects
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis End time in milliseconds (null for no limit)
     * @return List of GlucoseData objects within the time range
     */
    public List<HeadlessHistory.GlucoseData> getGlucoseHistoryInRange(Long startMillis, Long endMillis) {
        return HeadlessHistory.getGlucoseHistoryInRange(startMillis, endMillis);
    }
    
    /**
     * Get glucose statistics for a sensor
     */
    public void getGlucoseStats() {
        if (statsManager != null) {
            statsManager.emitIfReady();
        }
    }
    /**
     * Get glucose statistics for a sensor within an optional time range
     */
    public void getGlucoseStats(Long startMillis, Long endMillis) {
        if (statsManager != null) {
            statsManager.emitIfReady( startMillis, endMillis);
        }
    }

    /**
     * Retrieve sensor timeline info: last scanned, last stream, official and expected ends.
     * lastScannedMillis currently not exposed via Natives; returns null.
     */
    public HeadlessSensorInfo getSensorInfo(String serial) {
        // Official end from natives; value appears to be in seconds -> convert to ms
        Long sensorEndsMillis = null;
        try {
            long endsSec = Natives.sensorends();
            if (endsSec > 0) sensorEndsMillis = endsSec * 1000L;
        } catch (Throwable ignored) { }

        // Expected end: if not exposed separately, reuse official
        Long expectedEndMillis = sensorEndsMillis;

        // Last stream: derive from latest native glucose timestamp if available
        Long lastStreamMillis = null;
        try {
            long[] flat = Natives.getlastGlucose();
            if (flat != null && flat.length >= 2) {
                int n = flat.length / 2;
                long lastSec = flat[(n - 1) * 2];
                if (lastSec > 0) lastStreamMillis = lastSec * 1000L;
            }
        } catch (Throwable ignored) { }

        // Last scanned time: not directly available in Java; leave null for now
        Long lastScannedMillis = null;

        HeadlessSensorInfo info = new HeadlessSensorInfo(serial, lastScannedMillis, lastStreamMillis, sensorEndsMillis, expectedEndMillis);
        try {
            Log.d(TAG, "Sensor info for " + serial +
                    ": lastScanned=" + new Date(lastScannedMillis) +
                    ", lastStream=" + new Date(lastStreamMillis) +
                    ", endsOfficial=" + new Date(sensorEndsMillis) +
                    ", expectedEnd=" + new Date(expectedEndMillis));
        }
        catch (Exception e){}

        return info;
    }
    
    /**
     * Check if Bluetooth streaming is active
     * @return true if Bluetooth streaming is active
     */
    public boolean isBluetoothStreamingActive() {
        return SensorBluetooth.isActive();
    }
    
    /**
     * Start Bluetooth scanning for paired sensors
     */
    public void startBluetoothScanning() {
        if (SensorBluetooth.isActive()) return;
        SensorBluetooth.start(true);
    }
    
    /**
     * Stop Bluetooth scanning
     */
    public void stopBluetoothScanning() {
        if (!SensorBluetooth.isActive()) return;
        SensorBluetooth.stopScanning();
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        stopBluetoothScanning();
        SensorBluetooth.destructor();
        glucoseListener = null;
    }

    /**
     * Start NFC scanning for Libre sensor pairing
     * @return true if NFC scanning was started successfully
     */
    public boolean startNfcScanning() {
        if (nfcReader == null) {
            nfcReader = new HeadlessNfcReader();
        }
        return nfcReader.startScanning();
    }
    
    /**
     * Stop NFC scanning
     */
    public void stopNfcScanning() {
        if (nfcReader != null) {
            nfcReader.stopScanning();
        }
    }
    
    /**
     * Check if NFC scanning is active
     * @return true if NFC scanning is currently active
     */
    public boolean isNfcScanning() {
        return nfcReader != null && nfcReader.isScanning();
    }
    
    /**
     * Scan an NFC tag and return the result
     * This method processes the NFC tag data and returns a ScanResult object
     * @param tag The NFC tag to scan
     * @return ScanResult containing the scan information
     */
    public ScanResult scanNfcTag(Tag tag) {
        try {
            if (tag == null) {
                return new ScanResult(false, 0, 19, "", "Tag is null");
            }
            
            byte[] uid = tag.getId();
            if (uid == null || uid.length == 0) {
                return new ScanResult(false, 0, 19, "", "Invalid tag UID");
            }
            
            // Get sensor info
            byte[] info = AlgNfcV.nfcinfotimes(tag, 1);
            if (info == null || info.length != 6) {
                // Try Libre3 scan if info is not available
                if (uid.length == 8 && uid[6] != 7) {
                    return performLibre3Scan(tag, uid);
                } else {
                    return new ScanResult(false, 0, 17, "", "Failed to read tag info");
                }
            }
            
            // Read tag data
            byte[] data = AlgNfcV.readNfcTag(tag, uid, info);
            if (data == null) {
                return new ScanResult(false, 0, 18, "", "Failed to read tag data");
            }
            
            // Process the data using native method
            int result = Natives.nfcdata(uid, info, data);
            int glucoseValue = result & 0xFFFF;
            int returnCode = result >> 16;
            
            // Get serial number
            String serialNumber = Natives.getserial(uid, info);
            if (serialNumber == null) {
                serialNumber = "";
            }
            
            // Determine success and create message
            boolean success = glucoseValue > 0 || (returnCode & 0xFF) == 0 || (returnCode & 0xFF) == 8 || (returnCode & 0xFF) == 9;
            String message = createScanMessage(returnCode, glucoseValue, serialNumber);
            
            return new ScanResult(success, glucoseValue, returnCode, serialNumber, message);
            
        } catch (Exception e) {
            Log.e(TAG, "Error scanning NFC tag", e);
            return new ScanResult(false, 0, 19, "", "Scan error: " + e.getMessage());
        }
    }
    
    /**
     * Perform Libre3 specific NFC scan
     * @param tag The NFC tag to scan
     * @param uid The tag UID
     * @return ScanResult for Libre3 sensor
     */
    private ScanResult performLibre3Scan(Tag tag, byte[] uid) {
        try {
            // This is a simplified Libre3 scan - in a real implementation,
            // you would call the appropriate Libre3 scanning method
            String serialNumber = "Libre3_" + bytesToHex(uid);
            return new ScanResult(true, 0, 5, serialNumber, "Libre3 sensor detected");
        } catch (Exception e) {
            Log.e(TAG, "Error in Libre3 scan", e);
            return new ScanResult(false, 0, 19, "", "Libre3 scan error: " + e.getMessage());
        }
    }
    
    /**
     * Create a human-readable message based on the scan result
     * @param returnCode The return code from the scan
     * @param glucoseValue The glucose value
     * @param serialNumber The sensor serial number
     * @return Human-readable message
     */
    private String createScanMessage(int returnCode, int glucoseValue, String serialNumber) {
        int baseCode = returnCode & 0xFF;
        
        switch (baseCode) {
            case 0:
                if (glucoseValue > 0) {
                    return String.format("Glucose reading: %d mg/dL", glucoseValue);
                } else {
                    return "Scan successful, no glucose reading available";
                }
            case 3:
                return "Sensor needs activation";
            case 4:
                return "Sensor has ended";
            case 5:
                return "New sensor detected";
            case 7:
                return "New sensor detected (V2)";
            case 8:
                return "Streaming enabled successfully";
            case 9:
                return "Streaming already enabled";
            case 0x85:
            case 0x87:
                return "Streaming enabled (V2)";
            case 17:
                return "Failed to read tag information";
            case 18:
                return "Failed to read tag data";
            case 19:
                return "Unknown error occurred";
            default:
                return String.format("Unknown result code: %d", baseCode);
        }
    }
    
    /**
     * Convert byte array to hexadecimal string
     * @param bytes The byte array to convert
     * @return Hexadecimal string representation
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

}


