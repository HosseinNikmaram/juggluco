package tk.glucodata.headless;

/**
 * Headless model for sensor timeline information.
 */
public final class HeadlessSensorInfo {
    public final String serial;
    public final Long lastScannedMillis;     // may be null if not available
    public final Long lastStreamMillis;      // may be null if no stream yet
    public final Long sensorEndsMillis;      // official end from native
    public final Long expectedEndMillis;     // may equal official if unknown

    public HeadlessSensorInfo(String serial,
                              Long lastScannedMillis,
                              Long lastStreamMillis,
                              Long sensorEndsMillis,
                              Long expectedEndMillis) {
        this.serial = serial;
        this.lastScannedMillis = lastScannedMillis;
        this.lastStreamMillis = lastStreamMillis;
        this.sensorEndsMillis = sensorEndsMillis;
        this.expectedEndMillis = expectedEndMillis;
    }
}

