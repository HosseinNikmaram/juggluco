package tk.glucodata;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import tk.glucodata.headless.UsageExample;

public class MyActivity extends AppCompatActivity{
        private UsageExample jugglucoExample;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Initialize Juggluco
            jugglucoExample = UsageExample.getInstance();
            jugglucoExample.initializeJuggluco(this);
            jugglucoExample.startBluetoothScanning();
            jugglucoExample.startNfcScanning();

        }


        @Override
        protected void onResume() {
            super.onResume();
        }

        @Override
        protected void onPause() {
            super.onPause();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            // Clean up
            jugglucoExample.cleanup();
        }

}