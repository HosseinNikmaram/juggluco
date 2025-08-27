package tk.glucodata.headless;

public final class HeadlessHooks {
    public interface Provider {
        void toast(int resId);
        boolean canUseBluetooth();
        boolean mayScanBluetooth();
        boolean hasBluetoothPermission();
    }

    private static volatile Provider provider;

    private HeadlessHooks() {}

    public static void install(Provider p) {
        provider = p;
    }

    public static void uninstall() {
        provider = null;
    }

    public static Provider getProvider() {
        return provider;
    }
}