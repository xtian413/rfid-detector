package com.example.nfcreaderwriter;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SimulateActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private TextView statusText, tapInfoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulate);

        // UI
        statusText = findViewById(R.id.statusText);
        tapInfoText = findViewById(R.id.tapInfoText);

        // NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            statusText.setText("Enable NFC in settings");
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        } else {
            statusText.setText("Ready. Tap an NFC tag to simulate usage.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        IntentFilter[] filters = new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            handleNfcTap(tag);
        }
    }

    private void handleNfcTap(Tag tag) {
        try {
            // Get RFID UID from tag
            String rfid = bytesToHex(tag.getId());

            tapInfoText.setText("Tapped RFID: " + rfid);
            statusText.setText("Processing tap...");

            // Send tap data to API
            ApiClient.sendTap(rfid);

            Toast.makeText(this, "Tap recorded!", Toast.LENGTH_SHORT).show();
            statusText.setText("Tap sent to server - RFID: " + rfid);

        } catch (Exception e) {
            Toast.makeText(this, "Error processing tap: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            statusText.setText("Error occurred");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}