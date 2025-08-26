package tk.glucodata.headless;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcV;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.widget.Toast;
import java.util.Arrays;
import static android.content.Context.VIBRATOR_SERVICE;
import static tk.glucodata.Log.doLog;
import tk.glucodata.AlgNfcV;
import tk.glucodata.Log;
import tk.glucodata.Natives;
import tk.glucodata.SensorBluetooth;

/**
 * Headless NFC scanner for Libre sensors without requiring UI components
 */
public final class HeadlessNfcScanner {
    private static final String LOG_ID = "HeadlessNfcScanner";

    private static byte[] newDeviceUid = null;
    private static boolean askPermission = false;
    static final float mgdLmult=18.0f;

    public static final class ScanResult {
        public final boolean success;
        public final int glucoseValue;
        public final int returnCode;
        public final String serialNumber;
        public final String message;

        public ScanResult(boolean success, int glucoseValue, int returnCode, String serialNumber, String message) {
            this.success = success;
            this.glucoseValue = glucoseValue;
            this.returnCode = returnCode;
            this.serialNumber = serialNumber;
            this.message = message;
        }
    }

    public static ScanResult scanTag(Context context, Tag tag) {
        askPermission = false;
        if (!Natives.gethaslibrary()) {
            return new ScanResult(false, 0, 0x100000, null, "Library not available");
        }
        // Ensure NfcV tech is present
        if (NfcV.get(tag) == null) {
            return new ScanResult(false, 0, 17, null, "Unsupported tag tech (need NfcV)");
        }
        Vibrator vibrator = getVibrator(context);
        startVibration(vibrator);
        try {
            byte[] uid = tag.getId();
            if (doLog) {
                String sensId = "";
                for (var e : uid) sensId = String.format("%02X", (0xFF & e)) + sensId;
                Log.i(LOG_ID, "TAG::sensid=" + sensId);
            }
            boolean isLibre3 = uid.length == 8 && uid[6] != 7;
            byte[] info = AlgNfcV.nfcinfotimes(tag, (isLibre3 || doLog) ? 1 : 15);
            if (info == null || info.length != 6) {
                if (isLibre3) {
                    return libre3Scan(context, vibrator, tag);
                } else {
                    vibrator.cancel();
                    return new ScanResult(false, 0, 17, null, "Read Tag Info Error");
                }
            } else {
                byte[] data;
                try {
                    if ((data = AlgNfcV.readNfcTag(tag, uid, info)) != null) {
                        Log.d(LOG_ID, "Read Tag");
                        int uit = Natives.nfcdata(uid, info, data);
                        int value = uit & 0xFFFF;
                        Log.format("glucose=%.1f\n", (float) value / mgdLmult);
                        int ret = uit >> 16;
                        String serialNumber = Natives.getserial(uid, info);
                        if (newDeviceUid != null && Arrays.equals(newDeviceUid, uid)) {
                            if (value != 0 || (ret & 0xFF) == 5 || (ret & 0xFF) == 7) {
                                if (SensorBluetooth.resetDevice(serialNumber)) askPermission = true;
                                newDeviceUid = null;
                            }
                        }
                        vibrator.cancel();
                        switch (ret & 0xFF) {
                            case 8: {
                                boolean streamingEnabled = mayEnableStreaming(tag, uid, info);
                                if (streamingEnabled) showToast(context, "Streaming enabled for " + serialNumber);
                                return new ScanResult(true, value, 0, serialNumber, "Streaming enabled");
                            }
                            case 9: {
                                if (SensorBluetooth.resetDevice(serialNumber)) askPermission = true;
                                showToast(context, "Streaming enabled for " + serialNumber);
                                return new ScanResult(true, value, 0, serialNumber, "Streaming enabled");
                            }
                            case 4:
                                SensorBluetooth.sensorEnded(serialNumber);
                                showToast(context, "Sensor ended: " + serialNumber);
                                return new ScanResult(true, value, ret, serialNumber, "Sensor ended");
                            case 3: {
                                if (value == 0) {
                                    boolean actSuccess = AlgNfcV.activate(tag, info, uid);
                                    if (actSuccess) {
                                        newDeviceUid = uid;
                                        showToast(context, "Sensor activated successfully");
                                        return new ScanResult(true, value, ret, serialNumber, "Sensor activated");
                                    } else {
                                        failure(vibrator);
                                        showToast(context, "Sensor activation failed");
                                        return new ScanResult(false, value, ret, serialNumber, "Activation failed");
                                    }
                                }
                                break;
                            }
                            case 0x85:
                                mayEnableStreaming(tag, uid, info);
                                ret &= ~0x80;
                            case 5: {
                                final long[] newsensorWait = {50, 300, 100, 10};
                                if (android.os.Build.VERSION.SDK_INT < 26) vibrator.vibrate(newsensorWait, -1);
                                else vibrator.vibrate(VibrationEffect.createWaveform(newsensorWait, -1));
                                showToast(context, "New sensor detected: " + serialNumber);
                                return new ScanResult(true, value, ret, serialNumber, "New sensor");
                            }
                            case 0x87:
                                mayEnableStreaming(tag, uid, info);
                                ret &= ~0x80;
                            case 7: {
                                final long[] newsensorVib = {50, 150, 50, 50, 12, 8, 15, 73};
                                if (android.os.Build.VERSION.SDK_INT < 26) vibrator.vibrate(newsensorVib, -1);
                                else vibrator.vibrate(VibrationEffect.createWaveform(newsensorVib, -1));
                                showToast(context, "New sensor detected: " + serialNumber);
                                return new ScanResult(true, value, ret, serialNumber, "New sensor");
                            }
                        }
                        showToast(context, "Glucose: " + (float) value / mgdLmult + " mg/dL");
                        return new ScanResult(true, value, ret, serialNumber, "Scan successful");
                    } else {
                        vibrator.cancel();
                        return new ScanResult(false, 0, 17, null, "Failed to read tag data");
                    }
                } catch (TagLostException | IllegalStateException te) {
                    vibrator.cancel();
                    Log.d(LOG_ID, "Tag lost or invalid during read: " + te);
                    return new ScanResult(false, 0, 0x100000, null, "Scan error: Tag out of date");
                }
            }
        } catch (Exception e) {
            vibrator.cancel();
            Log.stack(LOG_ID, "scanTag error", e);
            return new ScanResult(false, 0, 0x100000, null, "Scan error: " + e.getMessage());
        }
    }

    public static void markNewDevice(byte[] uid) { newDeviceUid = uid; }
    public static boolean wasPermissionRequested() { return askPermission; }

    private static boolean mayEnableStreaming(Tag tag, byte[] uid, byte[] info) {
        if (!Natives.streamingAllowed()) {
            Log.d(LOG_ID, "!Natives.streamingAllowed()");
            return false;
        }
        if (!AlgNfcV.enableStreaming(tag, info)) {
            Log.d(LOG_ID, "Enable streaming failed");
            return false;
        }
        String sensorIdent = Natives.getserial(uid, info);
        Log.d(LOG_ID, "Streaming enabled, resetDevice " + sensorIdent);
        if (SensorBluetooth.resetDevice(sensorIdent)) askPermission = true;
        return true;
    }

    private static ScanResult libre3Scan(Context context, Vibrator vibrator, Tag tag) {
        vibrator.cancel();
        return new ScanResult(false, 0, 0x100000, null, "Libre3 scan not implemented");
    }

    private static Vibrator getVibrator(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vibratorManager.getDefaultVibrator();
        } else {
            return (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        }
    }

    private static void startVibration(Vibrator vibrator) {
        final long[] vibrationPattern = {0, 100, 50, 100};
        if (android.os.Build.VERSION.SDK_INT < 26) vibrator.vibrate(vibrationPattern, -1);
        else vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1));
    }

    private static void failure(Vibrator vibrator) {
        final long[] vibrationPatternFailure = {0, 500};
        if (android.os.Build.VERSION.SDK_INT < 26) vibrator.vibrate(vibrationPatternFailure, -1);
        else vibrator.vibrate(VibrationEffect.createWaveform(vibrationPatternFailure, -1));
    }

    private static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
