package tk.glucodata.headless;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.bluetooth.BluetoothAdapter;

import tk.glucodata.Natives;
import tk.glucodata.SensorBluetooth;

/**
 * Main headless manager for Juggluco Libre sensor integration
 * Provides NFC scanning, BLE management, and glucose data access
 */
public class HeadlessJugglucoManager {
    public static GlucoseListener glucoseListener;
    private static HeadlessHistory staticHistoryManager;
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
     * @param listener History listener implementation
     */
    public void setHistoryListener(HistoryListener listener) {
        if (listener != null) {
            historyManager = new HeadlessHistory(listener);
            staticHistoryManager = historyManager;
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
     * @param serial Sensor serial number
     */
    public void getGlucoseHistory(String serial) {
        if (historyManager != null) {
            historyManager.emitFromNativeLast(serial);
        }
    }
    /**
     * Get glucose history for a sensor within an optional time range
     * If startMillis/endMillis are null, they are ignored
     */
    public void getGlucoseHistory(String serial, Long startMillis, Long endMillis) {
        if (historyManager != null) {
            historyManager.emitFromNativeRange(serial, startMillis, endMillis);
        }
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
        staticHistoryManager = null;
    }

    /**
     * Emit latest history to the registered listener, if any.
     * Safe to call from BLE callbacks when new data arrives.
     */
    public static void emitLatestHistoryIfAny(String serial) {
        HeadlessHistory hm = staticHistoryManager;
        if (hm != null) {
            hm.emitFromNativeRange(serial, null, null);
        }
    }
}


