package tk.glucodata.headless;

public final class HeadlessConfig {
    private static volatile boolean headlessNfcEnabled = false;

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
}