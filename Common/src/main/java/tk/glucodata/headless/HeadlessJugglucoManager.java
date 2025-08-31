package tk.glucodata.headless;

import android.content.Context;
import android.util.Log;

import tk.glucodata.Natives;
import tk.glucodata.SensorBluetooth;

/**
 * Main headless manager for Juggluco Libre sensor integration
 * Provides NFC scanning, BLE management, and glucose data access
 */
public class HeadlessJugglucoManager {
    private static final String TAG = "HeadlessHead";
    private static volatile HeadlessJugglucoManager instance;
    
    public static GlucoseListener glucoseListener;
    private DeviceConnectionListener deviceConnectionListener;
    private HeadlessStats statsManager;
    private static volatile boolean nativesInitialized = false;
    

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

    public boolean init(Context ctx) {
        try {
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
                Log.d(TAG, "Device connection listener registered successfully");
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
            statsManager.emitIfReady(startMillis, endMillis);
        }
    }

    /**
     * Retrieve sensor timeline info: last scanned, last stream, official and expected ends.
     * lastScannedMillis currently not exposed via Natives; returns null.
     *
     * @return
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


