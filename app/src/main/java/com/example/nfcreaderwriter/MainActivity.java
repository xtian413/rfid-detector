package com.example.nfcreaderwriter;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private TextView statusText, readDataText;
    private EditText editName, editBalance;
    private Spinner spinnerType;
    private Button writeButton, clearButton;

    private boolean writeMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI
        statusText = findViewById(R.id.statusText);
        readDataText = findViewById(R.id.readDataText);
        editName = findViewById(R.id.editName);
        editBalance = findViewById(R.id.editBalance);
        spinnerType = findViewById(R.id.spinnerType);
        writeButton = findViewById(R.id.writeButton);
        clearButton = findViewById(R.id.clearButton);

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
            statusText.setText("Ready. Tap a tag to read or write.");
        }

        writeButton.setOnClickListener(v -> {
            if (editName.getText().toString().isEmpty() ||
                    editBalance.getText().toString().isEmpty()) {
                Toast.makeText(this, "Enter Name and Balance", Toast.LENGTH_SHORT).show();
            } else {
                writeMode = true;
                Toast.makeText(this, "Tap NFC tag to write", Toast.LENGTH_SHORT).show();
            }
        });

        clearButton.setOnClickListener(v -> {
            editName.setText("");
            editBalance.setText("");
            spinnerType.setSelection(0);
        });
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

            if (writeMode) {
                String name = editName.getText().toString();
                String balance = editBalance.getText().toString();
                String type = spinnerType.getSelectedItem().toString();

                String data = "Name: " + name + "\nBalance: " + balance + "\nType: " + type;

                writeNfcTag(tag, data);
                writeMode = false;
            } else {
                readNfcTag(tag);
            }
        }
    }

    private void writeNfcTag(Tag tag, String data) {
        try {
            NdefRecord record = NdefRecord.createTextRecord("en", data);
            NdefMessage message = new NdefMessage(new NdefRecord[]{record});

            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag is read-only", Toast.LENGTH_SHORT).show();
                    return;
                }
                ndef.writeNdefMessage(message);
                Toast.makeText(this, "Data written to tag!", Toast.LENGTH_SHORT).show();
                statusText.setText("Last Write: " + data);
                ndef.close();
            } else {
                Toast.makeText(this, "NDEF not supported on this tag", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Write failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void readNfcTag(Tag tag) {
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                readDataText.setText("NDEF not supported by this Tag.");
                return;
            }

            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            if (ndefMessage != null) {
                String result = new String(ndefMessage.getRecords()[0].getPayload(), StandardCharsets.UTF_8);

                // Remove language code prefix (first 3 characters for text records)
                if (result.length() > 3) {
                    result = result.substring(3);
                }

                readDataText.setText(result);

                // Parse the data and send to API
                parseAndSendToApi(tag, result);

            } else {
                readDataText.setText("No data found on tag");
            }
            ndef.close();
        } catch (Exception e) {
            readDataText.setText("Read failed: " + e.getMessage());
        }
    }

    private void parseAndSendToApi(Tag tag, String data) {
        try {
            // Get RFID from tag
            String rfid = bytesToHex(tag.getId());

            // Parse the data format: "Name: John\nBalance: 100.0\nType: Student"
            String name = "";
            String balance = "";
            String type = "";

            String[] lines = data.split("\n");
            for (String line : lines) {
                if (line.startsWith("Name: ")) {
                    name = line.substring(6).trim();
                } else if (line.startsWith("Balance: ")) {
                    balance = line.substring(9).trim();
                } else if (line.startsWith("Type: ")) {
                    type = line.substring(6).trim();
                }
            }

            // Send to API
            if (!name.isEmpty() && !balance.isEmpty() && !type.isEmpty()) {
                ApiClient.sendUser(rfid, name, balance, type);
                Toast.makeText(this, "Data updated on server!", Toast.LENGTH_SHORT).show();
                statusText.setText("Data updated - RFID: " + rfid + " | Name: " + name);
            } else {
                Toast.makeText(this, "Invalid data format on tag", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error parsing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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