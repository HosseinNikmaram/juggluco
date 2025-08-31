package tk.glucodata.headless;

/**
 * Listener interface for device connection, pairing, and scanning events
 * Provides comprehensive monitoring of Bluetooth device status
 */
public interface DeviceConnectionListener {
    
    // Connection status events
    void onDeviceConnected(String serialNumber, String deviceAddress);
    void onDeviceDisconnected(String serialNumber, String deviceAddress);
    void onDeviceConnectionFailed(String serialNumber, String deviceAddress, int errorCode);
    
    // Pairing/Bonding events
    void onDevicePaired(String serialNumber, String deviceAddress);
    void onDeviceUnpaired(String serialNumber, String deviceAddress);

    void onDeviceFound(String serialNumber, String deviceAddress);
    
    // General Bluetooth status
    void onBluetoothEnabled();
    void onBluetoothDisabled();

}