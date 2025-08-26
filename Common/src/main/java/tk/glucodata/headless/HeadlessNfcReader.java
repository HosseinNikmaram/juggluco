package tk.glucodata.headless;


import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import tk.glucodata.Log;
import tk.glucodata.Natives;

/**
 * Headless NFC reader that implements NfcAdapter.ReaderCallback
 * Can be used to scan NFC tags without requiring MainActivity
 */
public class HeadlessNfcReader extends Activity implements NfcAdapter.ReaderCallback {
    private NfcAdapter nfcAdapter;
    private boolean readerModeEnabled = false;
    private static    final int nfcflags=NfcAdapter.FLAG_READER_NFC_V | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK; // restrict to NfcV only
    private Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        enableReaderMode();
        handler.postDelayed(this::finishWithTimeout, 30000);

    }

    public boolean isNfcAvailable() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    private boolean enableReaderMode() {
        if (!isNfcAvailable()) {
            showToast("NFC not available or disabled");
            finish();
            return false;
        }

        if (readerModeEnabled) {
            disableReaderMode(); // Disable first to ensure clean state
        }

        try {
            final int flags=(Natives.nfcsound()?0:NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS)|nfcflags;
            nfcAdapter.enableReaderMode(this, this, flags, null);
            readerModeEnabled = true;
            showToast("NFC reader mode enabled - scan your Libre sensor");
            return true;
        } catch (Exception e) {
            showToast("Failed to enable NFC reader mode: " + e.getMessage());
            readerModeEnabled = false;
            finish();
            return false;
        }
    }

    public void disableReaderMode() {
        if (nfcAdapter != null && readerModeEnabled) {
            try {
                nfcAdapter.disableReaderMode(this);
                readerModeEnabled = false;
            } catch (Exception e) {
                // Ignore errors when disabling
            }
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        // Process synchronously to avoid Tag becoming out-of-date
        handler.removeCallbacksAndMessages(null);
        processTag(tag);
        runOnUiThread(this::finish);
    }
    private void finishWithTimeout() {
        // Toast.makeText(this, "Scan timed out", Toast.LENGTH_SHORT).show();
        runOnUiThread(this::finish);
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableReaderMode();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableReaderMode();
        handler.removeCallbacksAndMessages(null);
    }

    private void processTag(Tag tag) {
        showToast("NFC Tag Discovered");

        HeadlessNfcScanner.ScanResult result = HeadlessNfcScanner.scanTag(this, tag);

        if (result.success) {
            showToast("Scan successful: " + result.message);

            if (HeadlessNfcScanner.wasPermissionRequested()) {
                showToast("Bluetooth permission may be needed for streaming");
            }
        } else {
            showToast("Scan failed: " + result.message);
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d("HeadlessNfcReader", message);
        });
    }
}
