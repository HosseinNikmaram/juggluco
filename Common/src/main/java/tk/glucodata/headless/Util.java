package tk.glucodata.headless;


import tk.glucodata.Applic;
import tk.glucodata.Natives;
import tk.glucodata.SensorBluetooth;

public class Util {
    /**
     * Handles the scanned tag input and performs appropriate actions
     * based on the tag type and sensor version.
     *
     * @param scanTag The scanned NFC tag string.
     */
    static void  handleScanTag(String scanTag) {
        // Check if it's a MirrorJuggluco tag
        if (scanTag.endsWith("MirrorJuggluco")) {
            return;
        }
        String name = Natives.addSIscangetName(scanTag);
        if (name == null) {
            return;
        }
        var dataPtr = Natives.getdataptr(name);
        int type = Natives.getLibreVersion(dataPtr);
        Natives.freedataptr(dataPtr);
        boolean devicesUpdated = SensorBluetooth.updateDevices();
        Applic.wakemirrors();
    }

}
