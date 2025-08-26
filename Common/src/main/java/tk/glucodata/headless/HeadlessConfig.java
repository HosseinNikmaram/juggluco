package tk.glucodata.headless;

public final class HeadlessConfig {
    private static volatile boolean headlessNfcEnabled = false;
    private static volatile boolean bleEnabled = true;

    private HeadlessConfig() {}

    public static void enableHeadlessNfc() {
        headlessNfcEnabled = true;
    }

    public static void disableHeadlessNfc() {
        headlessNfcEnabled = false;
    }

    public static boolean isHeadlessNfcEnabled() {
        return headlessNfcEnabled;
    }

    public static void setBleEnabled(boolean enabled) { bleEnabled = enabled; }
    public static boolean isBleEnabled() { return bleEnabled; }
}