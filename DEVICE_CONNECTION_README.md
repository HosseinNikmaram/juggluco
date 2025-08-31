# Device Connection Listener - Juggluco Headless

## Overview

The `DeviceConnectionListener` provides comprehensive monitoring of Bluetooth device connection, pairing, and scanning events in the headless Juggluco system. This allows your application to track the real-time status of Libre sensors and respond to connection changes.

## Features

### 🔗 Connection Status Events
- **Device Connected**: When a sensor successfully connects
- **Device Disconnected**: When a sensor connection is lost
- **Device Connecting**: When a connection attempt starts
- **Connection Failed**: When a connection attempt fails with error code

### 🔐 Pairing/Bonding Events
- **Device Paired**: When a sensor is successfully paired
- **Device Unpaired**: When a sensor pairing is removed
- **Device Pairing**: When a pairing process starts

### 🔍 Scanning Events
- **Scan Started**: When Bluetooth scanning begins
- **Scan Stopped**: When Bluetooth scanning ends
- **Device Found**: When a new sensor is discovered

### 📱 Bluetooth Status Events
- **Bluetooth Enabled**: When Bluetooth is turned on
- **Bluetooth Disabled**: When Bluetooth is turned off

### ⚡ Connection Quality Events
- **Connection Priority Changed**: When connection priority is modified
- **Connection Updated**: When connection parameters are updated

## Usage

### 1. Basic Implementation

```java
public class MyActivity extends Activity {
    private HeadlessJugglucoManager jugglucoManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        jugglucoManager = new HeadlessJugglucoManager();
        jugglucoManager.init(this);
        
        // Set the device connection listener
        jugglucoManager.setDeviceConnectionListener(new DeviceConnectionListener() {
            @Override
            public void onDeviceConnected(String serialNumber, String deviceAddress) {
                Log.i("MyApp", "Sensor connected: " + serialNumber);
                // Handle successful connection
            }
            
            @Override
            public void onDeviceDisconnected(String serialNumber, String deviceAddress) {
                Log.w("MyApp", "Sensor disconnected: " + serialNumber);
                // Handle disconnection
            }
            
            // Implement other methods as needed...
        });
    }
}
```

### 2. Complete Implementation Example

```java
jugglucoManager.setDeviceConnectionListener(new DeviceConnectionListener() {
    @Override
    public void onDeviceConnected(String serialNumber, String deviceAddress) {
        String message = String.format("Device connected: %s at %s", serialNumber, deviceAddress);
        Log.i(TAG, message);
        showToast(message);
    }
    
    @Override
    public void onDeviceDisconnected(String serialNumber, String deviceAddress) {
        String message = String.format("Device disconnected: %s at %s", serialNumber, deviceAddress);
        Log.i(TAG, message);
        showToast(message);
    }
    
    @Override
    public void onDevicePaired(String serialNumber, String deviceAddress) {
        String message = String.format("Device paired: %s at %s", serialNumber, deviceAddress);
        Log.i(TAG, message);
        showToast(message);
    }
    
    @Override
    public void onDeviceUnpaired(String serialNumber, String deviceAddress) {
        String message = String.format("Device unpaired: %s at %s", serialNumber, deviceAddress);
        Log.i(TAG, message);
        showToast(message);
    }
    
    @Override
    public void onScanStarted() {
        Log.i(TAG, "Bluetooth scanning started");
        showToast("Bluetooth scanning started");
    }
    
    @Override
    public void onScanStopped() {
        Log.i(TAG, "Bluetooth scanning stopped");
        showToast("Bluetooth scanning stopped");
    }
    
    @Override
    public void onDeviceFound(String serialNumber, String deviceAddress, String deviceName) {
        String message = String.format("Device found: %s at %s (%s)", serialNumber, deviceAddress, deviceName);
        Log.i(TAG, message);
        showToast(message);
    }
    
    @Override
    public void onBluetoothEnabled() {
        Log.i(TAG, "Bluetooth enabled");
        showToast("Bluetooth enabled");
    }
    
    @Override
    public void onBluetoothDisabled() {
        Log.w(TAG, "Bluetooth disabled");
        showToast("Bluetooth disabled");
    }
    
    @Override
    public void onDeviceConnecting(String serialNumber, String deviceAddress) {
        Log.d(TAG, "Device connecting: " + serialNumber);
    }
    
    @Override
    public void onDeviceConnectionFailed(String serialNumber, String deviceAddress, int errorCode) {
        Log.e(TAG, "Device connection failed: " + serialNumber + " (Error: " + errorCode + ")");
    }
    
    @Override
    public void onDevicePairing(String serialNumber, String deviceAddress) {
        Log.d(TAG, "Device pairing: " + serialNumber);
    }
    
    @Override
    public void onConnectionPriorityChanged(String serialNumber, String deviceAddress, int priority) {
        Log.d(TAG, "Connection priority changed: " + serialNumber + " (Priority: " + priority + ")");
    }
    
    @Override
    public void onConnectionUpdated(String serialNumber, String deviceAddress, int interval, int latency, int timeout) {
        Log.d(TAG, "Connection updated: " + serialNumber + " (Interval: " + interval + ", Latency: " + latency + ", Timeout: " + timeout + ")");
    }
    
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MyActivity.this, message, Toast.LENGTH_SHORT).show());
    }
});
```

## Integration with Existing Code

The `DeviceConnectionListener` integrates seamlessly with existing Juggluco functionality:

- **Automatic Registration**: Listener is automatically registered when `HeadlessJugglucoManager.init()` is called
- **Error Handling**: All listener calls are wrapped in try-catch blocks for stability
- **Performance**: Events are delivered asynchronously without blocking the main thread
- **Multiple Listeners**: Support for multiple listeners if needed

## Event Flow

```
Bluetooth Event → SensorBluetooth → DeviceConnectionListener → Your App
     ↓                    ↓                    ↓              ↓
Connection Change → notifyConnectionListeners → onDeviceConnected → Handle Event
```

## Best Practices

### 1. UI Updates
Always use `runOnUiThread()` when updating UI from listener callbacks:

```java
@Override
public void onDeviceConnected(String serialNumber, String deviceAddress) {
    runOnUiThread(() -> {
        updateConnectionStatus(true);
        showConnectedIndicator();
    });
}
```

### 2. Error Handling
Implement proper error handling for connection failures:

```java
@Override
public void onDeviceConnectionFailed(String serialNumber, String deviceAddress, int errorCode) {
    Log.e(TAG, "Connection failed for " + serialNumber + " with error: " + errorCode);
    
    switch (errorCode) {
        case 8: // GATT_INTERNAL_ERROR
            // Handle internal error
            break;
        case 19: // GATT_ERROR
            // Handle general GATT error
            break;
        default:
            // Handle unknown error
            break;
    }
}
```

### 3. State Management
Track device states in your application:

```java
private Map<String, DeviceState> deviceStates = new HashMap<>();

@Override
public void onDeviceConnected(String serialNumber, String deviceAddress) {
    deviceStates.put(serialNumber, DeviceState.CONNECTED);
    updateDeviceList();
}

@Override
public void onDeviceDisconnected(String serialNumber, String deviceAddress) {
    deviceStates.put(serialNumber, DeviceState.DISCONNECTED);
    updateDeviceList();
}
```

## Troubleshooting

### Common Issues

1. **Listener Not Receiving Events**
   - Ensure `jugglucoManager.init()` is called before setting the listener
   - Check that Bluetooth permissions are granted
   - Verify the listener is properly implemented

2. **Events Not Firing**
   - Check logcat for any error messages
   - Ensure SensorBluetooth is properly initialized
   - Verify Bluetooth is enabled on the device

3. **Performance Issues**
   - Avoid heavy operations in listener callbacks
   - Use background threads for data processing
   - Implement proper cleanup in `onDestroy()`

### Debug Logging

Enable debug logging to troubleshoot issues:

```java
// In your listener implementation
@Override
public void onDeviceConnected(String serialNumber, String deviceAddress) {
    Log.d(TAG, "=== DEVICE CONNECTION EVENT ===");
    Log.d(TAG, "Serial: " + serialNumber);
    Log.d(TAG, "Address: " + deviceAddress);
    Log.d(TAG, "Timestamp: " + System.currentTimeMillis());
    // ... handle the event
}
```

## API Reference

### DeviceConnectionListener Interface

```java
public interface DeviceConnectionListener {
    // Connection events
    void onDeviceConnected(String serialNumber, String deviceAddress);
    void onDeviceDisconnected(String serialNumber, String deviceAddress);
    void onDeviceConnecting(String serialNumber, String deviceAddress);
    void onDeviceConnectionFailed(String serialNumber, String deviceAddress, int errorCode);
    
    // Pairing events
    void onDevicePaired(String serialNumber, String deviceAddress);
    void onDeviceUnpaired(String serialNumber, String deviceAddress);
    void onDevicePairing(String serialNumber, String deviceAddress);
    
    // Scanning events
    void onScanStarted();
    void onScanStopped();
    void onDeviceFound(String serialNumber, String deviceAddress, String deviceName);
    
    // Bluetooth status
    void onBluetoothEnabled();
    void onBluetoothDisabled();
    
    // Connection quality
    void onConnectionPriorityChanged(String serialNumber, String deviceAddress, int priority);
    void onConnectionUpdated(String serialNumber, String deviceAddress, int interval, int latency, int timeout);
}
```

### HeadlessJugglucoManager Methods

```java
public class HeadlessJugglucoManager {
    // Set the device connection listener
    public void setDeviceConnectionListener(DeviceConnectionListener listener);
    
    // Check Bluetooth streaming status
    public boolean isBluetoothStreamingActive();
    
    // Control Bluetooth scanning
    public void startBluetoothScanning();
    public void stopBluetoothScanning();
}
```

## Example Use Cases

### 1. Connection Monitoring Dashboard
```java
// Display real-time connection status for all sensors
@Override
public void onDeviceConnected(String serialNumber, String deviceAddress) {
    updateConnectionStatus(serialNumber, "Connected", Color.GREEN);
}

@Override
public void onDeviceDisconnected(String serialNumber, String deviceAddress) {
    updateConnectionStatus(serialNumber, "Disconnected", Color.RED);
}
```

### 2. Automatic Reconnection
```java
@Override
public void onDeviceDisconnected(String serialNumber, String deviceAddress) {
    // Attempt to reconnect after a delay
    new Handler().postDelayed(() -> {
        jugglucoManager.startBluetoothScanning();
    }, 5000);
}
```

### 3. User Notifications
```java
@Override
public void onDevicePaired(String serialNumber, String deviceAddress) {
    showNotification("Sensor Paired", "New sensor " + serialNumber + " has been paired successfully");
}

@Override
public void onDeviceConnectionFailed(String serialNumber, String deviceAddress, int errorCode) {
    showNotification("Connection Failed", "Failed to connect to sensor " + serialNumber);
}
```

This comprehensive listener system provides full visibility into the Bluetooth device lifecycle, enabling robust applications that can respond appropriately to all connection scenarios.