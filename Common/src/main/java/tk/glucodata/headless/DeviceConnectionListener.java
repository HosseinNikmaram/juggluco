package tk.glucodata.headless;

/**
 * Listener interface for device connection, pairing, and scanning events
 * Provides comprehensive monitoring of Bluetooth device status
 */
public interface DeviceConnectionListener {
    
    // Connection status events
    void onDeviceConnected(String serialNumber, String deviceAddress);
    void onDeviceDisconnected(String serialNumber, String deviceAddress);
    void onDeviceConnecting(String serialNumber, String deviceAddress);
    void onDeviceConnectionFailed(String serialNumber, String deviceAddress, int errorCode);
    
    // Pairing/Bonding events
    void onDevicePaired(String serialNumber, String deviceAddress);
    void onDeviceUnpaired(String serialNumber, String deviceAddress);
    void onDevicePairing(String serialNumber, String deviceAddress);
    
    // Scanning events
    void onScanStarted();
    void onScanStopped();
    void onDeviceFound(String serialNumber, String deviceAddress, String deviceName);
    
    // General Bluetooth status
    void onBluetoothEnabled();
    void onBluetoothDisabled();
    
    // Connection priority and quality events
    void onConnectionPriorityChanged(String serialNumber, String deviceAddress, int priority);
    void onConnectionUpdated(String serialNumber, String deviceAddress, int interval, int latency, int timeout);
}