package tk.glucodata.headless;

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.util.Log;

/**
 * Headless NFC reader for scanning Libre sensors
 * Provides NFC scanning functionality without requiring a full MainActivity
 */
public class HeadlessNfcReader {
    private static final String TAG = "HeadlessNfcReader";
    
    private NfcAdapter nfcAdapter;
    private boolean isScanning = false;
    private Context context;
    
    /**
     * Initialize the NFC reader
     * @param context The application context
     */
    public HeadlessNfcReader(Context context) {
        this.context = context;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }
    
    /**
     * Default constructor
     */
    public HeadlessNfcReader() {
        // Will be initialized when context is set
    }
    
    /**
     * Set the context for NFC operations
     * @param context The application context
     */
    public void setContext(Context context) {
        this.context = context;
        if (this.nfcAdapter == null) {
            this.nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        }
    }
    
    /**
     * Check if NFC is available on this device
     * @return true if NFC is available
     */
    public boolean isNfcAvailable() {
        return nfcAdapter != null;
    }
    
    /**
     * Check if NFC is enabled
     * @return true if NFC is enabled
     */
    public boolean isNfcEnabled() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }
    
    /**
     * Start NFC scanning
     * @return true if scanning was started successfully
     */
    public boolean startScanning() {
        if (nfcAdapter == null) {
            Log.e(TAG, "NFC adapter not available");
            return false;
        }
        
        if (!nfcAdapter.isEnabled()) {
            Log.e(TAG, "NFC is not enabled");
            return false;
        }
        
        if (isScanning) {
            Log.d(TAG, "NFC scanning already active");
            return true;
        }
        
        try {
            // Note: In headless mode, we can't directly enable reader mode
            // This would typically be handled by the MainActivity
            // For now, we'll just mark scanning as active
            isScanning = true;
            Log.i(TAG, "NFC scanning started");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start NFC scanning", e);
            return false;
        }
    }
    
    /**
     * Stop NFC scanning
     */
    public void stopScanning() {
        if (!isScanning) {
            return;
        }
        
        try {
            isScanning = false;
            Log.i(TAG, "NFC scanning stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping NFC scanning", e);
        }
    }
    
    /**
     * Check if NFC scanning is currently active
     * @return true if scanning is active
     */
    public boolean isScanning() {
        return isScanning;
    }
    
    /**
     * Process an NFC tag that was discovered
     * This method should be called from the MainActivity's onTagDiscovered callback
     * @param tag The discovered NFC tag
     * @return ScanResult containing the scan information
     */
    public ScanResult processDiscoveredTag(Tag tag) {
        if (tag == null) {
            return new ScanResult(false, 0, 19, "", "Tag is null");
        }
        
        if (!isScanning) {
            return new ScanResult(false, 0, 19, "", "NFC scanning not active");
        }
        
        try {
            // Delegate to HeadlessJugglucoManager for actual scanning
            // This maintains separation of concerns
            return HeadlessJugglucoManager.getInstance().scanNfcTag(tag);
        } catch (Exception e) {
            Log.e(TAG, "Error processing discovered tag", e);
            return new ScanResult(false, 0, 19, "", "Error processing tag: " + e.getMessage());
        }
    }
    
    /**
     * Get the NFC adapter
     * @return NfcAdapter instance or null if not available
     */
    public NfcAdapter getNfcAdapter() {
        return nfcAdapter;
    }
    
    /**
     * Get the current scanning status as a string
     * @return String representation of the scanning status
     */
    public String getScanningStatus() {
        if (nfcAdapter == null) {
            return "NFC not available";
        }
        
        if (!nfcAdapter.isEnabled()) {
            return "NFC disabled";
        }
        
        if (isScanning) {
            return "Scanning active";
        } else {
            return "Scanning inactive";
        }
    }
}