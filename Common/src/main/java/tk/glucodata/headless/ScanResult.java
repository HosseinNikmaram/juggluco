package tk.glucodata.headless;

/**
 * Result of an NFC scan operation
 * Contains information about the scan success, glucose value, return code, serial number, and message
 */
public class ScanResult {
    private final boolean success;
    private final int glucoseValue;
    private final int returnCode;
    private final String serialNumber;
    private final String message;
    
    /**
     * Create a new ScanResult
     * @param success Whether the scan was successful
     * @param glucoseValue The glucose value in mg/dL (0 if not available)
     * @param returnCode The return code from the NFC scan
     * @param serialNumber The sensor serial number
     * @param message A human-readable message describing the result
     */
    public ScanResult(boolean success, int glucoseValue, int returnCode, String serialNumber, String message) {
        this.success = success;
        this.glucoseValue = glucoseValue;
        this.returnCode = returnCode;
        this.serialNumber = serialNumber;
        this.message = message;
    }
    
    /**
     * Check if the scan was successful
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Get the glucose value from the scan
     * @return Glucose value in mg/dL, or 0 if not available
     */
    public int getGlucoseValue() {
        return glucoseValue;
    }
    
    /**
     * Get the return code from the NFC scan
     * @return Return code indicating the scan result type
     */
    public int getReturnCode() {
        return returnCode;
    }
    
    /**
     * Get the sensor serial number
     * @return Serial number of the scanned sensor
     */
    public String getSerialNumber() {
        return serialNumber;
    }
    
    /**
     * Get a human-readable message describing the scan result
     * @return Description of what happened during the scan
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get a formatted string representation of the scan result
     * @return String representation of the ScanResult
     */
    @Override
    public String toString() {
        return String.format("ScanResult{success=%s, glucoseValue=%d mg/dL, returnCode=%d, serialNumber='%s', message='%s'}", 
            success, glucoseValue, returnCode, serialNumber, message);
    }
    
    /**
     * Check if this result contains a valid glucose reading
     * @return true if glucose value is greater than 0
     */
    public boolean hasGlucoseReading() {
        return glucoseValue > 0;
    }
    
    /**
     * Get the glucose value as a float for more precise calculations
     * @return Glucose value as float
     */
    public float getGlucoseValueFloat() {
        return (float) glucoseValue;
    }
    
    /**
     * Get the return code description
     * @return Human-readable description of the return code
     */
    public String getReturnCodeDescription() {
        switch (returnCode & 0xFF) {
            case 0: return "Success";
            case 3: return "Sensor needs activation";
            case 4: return "Sensor ended";
            case 5: return "New sensor";
            case 7: return "New sensor (V2)";
            case 8: return "Streaming enabled";
            case 9: return "Streaming already enabled";
            case 0x85: return "Streaming enabled (V2)";
            case 0x87: return "Streaming enabled (V2)";
            case 17: return "Read Tag Info Error";
            case 18: return "Read Tag Data Error";
            case 19: return "Unknown error";
            default: return "Unknown return code: " + returnCode;
        }
    }
}