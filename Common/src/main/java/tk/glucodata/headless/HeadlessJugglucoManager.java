package tk.glucodata.headless;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import tk.glucodata.Natives;
import tk.glucodata.SensorBluetooth;

import java.util.ArrayList;
import java.util.List;

/**
 * Main headless manager for Juggluco Libre sensor integration
 * Provides NFC scanning, BLE management, and glucose data access
 */
public class HeadlessJugglucoManager {
    private static final String TAG = "HeadlessHead";
    public static GlucoseListener glucoseListener;
    private Activity activity;
    private HeadlessNfcReader nfcReader;
    private HeadlessHistory historyManager;
    private HeadlessStats statsManager;
    private static volatile boolean nativesInitialized = false;
    
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
            this.activity = ctx;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check and enable Bluetooth if needed
     * @param ctx Android context
     * @return true if Bluetooth is available and enabled
     */
    public boolean ensurePermissionsAndBluetooth(Context ctx) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return false;
        
        if (!adapter.isEnabled()) {
            adapter.enable();
        }
        return true;
    }
    
    public boolean isNfcScanning() {
        return HeadlessNfcReader.isScanning();
    }
    
    /**
     * Set glucose listener for real-time glucose updates
     * @param listener Glucose listener implementation
     */
    public void setGlucoseListener(GlucoseListener listener) {
        glucoseListener = listener;
    }
    
    /**
     * Set history listener for glucose history data
     * @param serial Sensor serial number
     */
    public void setHistoryListener(String serial) {
        if (serial != null) {
            historyManager = new HeadlessHistory(serial);
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
     * Start NFC scanning for Libre sensor pairing
     * @return true if NFC scanning was started successfully
     */
    public void startNfcScanning() {
        if(activity==null) return;
        nfcReader = new HeadlessNfcReader();
        Intent intent = new Intent(activity, HeadlessNfcReader.class);
        activity.startActivity(intent);
    }
    
    /**
     * Check if NFC is available
     * @return true if NFC is available and enabled
     */
    public boolean isNfcAvailable() {
        return nfcReader != null && nfcReader.isNfcAvailable();
    }
    
    /**
     * Manually scan an NFC tag (useful for testing or custom NFC handling)
     * @param ctx Android context
     * @param tag NFC tag from onTagDiscovered
     * @return Scan result with detailed information
     */
    public HeadlessNfcScanner.ScanResult scanNfcTag(Context ctx, android.nfc.Tag tag) {
        return HeadlessNfcScanner.scanTag(ctx, tag);
    }
    
    /**
     * Mark a new device for pairing (call before scanning)
     * @param uid Device UID bytes
     */
    public void markNewDevice(byte[] uid) {
        HeadlessNfcScanner.markNewDevice(uid);
    }

    /**
     * Get current glucose history for a sensor
     * @return Array of [timeMillis, mgdl] pairs
     */
    public long[][] getGlucoseHistory() {
        if (historyManager != null) {
            return historyManager.getCurrentGlucoseHistory();
        }
        return new long[0][2];
    }
    
    /**
     * Get glucose history for a sensor within an optional time range
     * If startMillis/endMillis are null, they are ignored
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis End time in milliseconds (null for no limit)
     * @return Array of [timeMillis, mgdl] pairs
     */
    public long[][] getGlucoseHistory(Long startMillis, Long endMillis) {
        if (historyManager != null) {
            return historyManager.getGlucoseHistoryInRangeLegacy(startMillis, endMillis);
        }
        return new long[0][2];
    }

    /**
     * Get complete glucose history for a sensor as a list of GlucoseData objects
     * @return List of GlucoseData objects, or empty list if no data
     */
    public List<HeadlessHistory.GlucoseData> getCompleteGlucoseHistory() {
        if (historyManager != null) {
            return historyManager.getCompleteGlucoseHistory();
        }
        return new ArrayList<>();
    }

    /**
     * Get glucose history for a sensor within a time range as a list of GlucoseData objects
     * @param startMillis Start time in milliseconds (null for no limit)
     * @param endMillis End time in milliseconds (null for no limit)
     * @return List of GlucoseData objects within the time range
     */
    public List<HeadlessHistory.GlucoseData> getGlucoseHistoryInRange(Long startMillis, Long endMillis) {
        if (historyManager != null) {
            return historyManager.getGlucoseHistoryInRange(startMillis, endMillis);
        }
        return new ArrayList<>();
    }
    
    /**
     * Get glucose statistics for a sensor
     * @param serial Sensor serial number
     */
    public void getGlucoseStats(String serial) {
        if (statsManager != null) {
            statsManager.emitIfReady(serial);
        }
    }
    /**
     * Get glucose statistics for a sensor within an optional time range
     */
    public void getGlucoseStats(String serial, Long startMillis, Long endMillis) {
        if (statsManager != null) {
            statsManager.emitIfReady(serial, startMillis, endMillis);
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
        Log.d(TAG, "Sensor info for " + serial +
                ": lastScanned=" + lastScannedMillis +
                ", lastStream=" + lastStreamMillis +
                ", endsOfficial=" + sensorEndsMillis +
                ", expectedEnd=" + expectedEndMillis);
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

}


