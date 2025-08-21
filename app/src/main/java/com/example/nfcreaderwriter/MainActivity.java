package com.example.nfcreaderwriter;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NFCReaderWriter";

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;

    private TextView statusText;
    private TextView readDataText;
    private EditText writeDataEdit;
    private Button writeButton;
    private Button clearButton;

    private boolean isWriteMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeNFC();
        setupClickListeners();
    }

    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        readDataText = findViewById(R.id.readDataText);
        writeDataEdit = findViewById(R.id.writeDataEdit);
        writeButton = findViewById(R.id.writeButton);
        clearButton = findViewById(R.id.clearButton);
    }

    private void initializeNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            statusText.setText("NFC not supported on this device");
            writeButton.setEnabled(false);
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            statusText.setText("NFC is disabled. Please enable it in settings.");
            writeButton.setEnabled(false);
            return;
        }

        statusText.setText("NFC Ready - Bring NFC card close to device");

        // Create pending intent
        pendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE
        );

        // Setup intent filters
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }

        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);

        intentFiltersArray = new IntentFilter[] {ndef, tech, tag};

        // Setup tech lists
        techListsArray = new String[][] {
                new String[] { Ndef.class.getName() },
                new String[] { NdefFormatable.class.getName() }
        };
    }

    private void setupClickListeners() {
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = writeDataEdit.getText().toString().trim();
                if (data.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter data to write", Toast.LENGTH_SHORT).show();
                    return;
                }

                isWriteMode = true;
                statusText.setText("Write mode ON - Bring NFC card close to write: " + data);
                Toast.makeText(MainActivity.this, "Ready to write. Touch NFC card to device.", Toast.LENGTH_SHORT).show();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readDataText.setText("No data read yet");
                writeDataEdit.setText("");
                isWriteMode = false;
                statusText.setText("NFC Ready - Bring NFC card close to device");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
        }
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
        handleNfcIntent(intent);
    }

    private void handleNfcIntent(Intent intent) {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (isWriteMode) {
                writeNfcTag(tag);
            } else {
                readNfcTag(intent, tag);
            }
        }
    }

    private void readNfcTag(Intent intent, Tag tag) {
        String tagId = bytesToHex(tag.getId());
        Log.d(TAG, "Tag ID: " + tagId);

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
                processNdefMessages(msgs, tagId);
                return;
            }
        }

        // Try to read NDEF from tag directly
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                if (ndefMessage != null) {
                    processNdefMessages(new NdefMessage[]{ndefMessage}, tagId);
                } else {
                    readDataText.setText("Tag ID: " + tagId + "\nNo NDEF data found");
                }
                ndef.close();
            } catch (Exception e) {
                Log.e(TAG, "Error reading NDEF message", e);
                readDataText.setText("Tag ID: " + tagId + "\nError reading tag: " + e.getMessage());
            }
        } else {
            readDataText.setText("Tag ID: " + tagId + "\nTag is not NDEF compatible");
        }

        statusText.setText("Tag read - Ready for next operation");
    }

    private void processNdefMessages(NdefMessage[] msgs, String tagId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tag ID: ").append(tagId).append("\n\n");

        for (NdefMessage msg : msgs) {
            NdefRecord[] records = msg.getRecords();
            for (NdefRecord record : records) {
                sb.append("Record Type: ").append(new String(record.getType())).append("\n");

                if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                        java.util.Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {

                    String text = readTextRecord(record);
                    sb.append("Text: ").append(text).append("\n");
                } else {
                    sb.append("Payload: ").append(new String(record.getPayload())).append("\n");
                }
                sb.append("\n");
            }
        }

        readDataText.setText(sb.toString());
    }

    private String readTextRecord(NdefRecord record) {
        byte[] payload = record.getPayload();

        // Get the Text Encoding
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

        // Get the Language Code
        int languageCodeLength = payload[0] & 0063;

        // Get the Text
        return new String(payload, languageCodeLength + 1,
                payload.length - languageCodeLength - 1,
                Charset.forName(textEncoding));
    }

    private void writeNfcTag(Tag tag) {
        String dataToWrite = writeDataEdit.getText().toString();

        try {
            NdefRecord textRecord = createTextRecord(dataToWrite, "en");
            NdefMessage ndefMessage = new NdefMessage(textRecord);

            boolean success = writeNdefMessage(tag, ndefMessage);

            if (success) {
                statusText.setText("Write successful! Data written: " + dataToWrite);
                Toast.makeText(this, "Data written successfully!", Toast.LENGTH_SHORT).show();
            } else {
                statusText.setText("Write failed - Please try again");
                Toast.makeText(this, "Failed to write data", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error writing to NFC tag", e);
            statusText.setText("Write error: " + e.getMessage());
            Toast.makeText(this, "Write error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        isWriteMode = false;
    }

    private NdefRecord createTextRecord(String text, String languageCode) {
        byte[] languageBytes = languageCode.getBytes(Charset.forName("US-ASCII"));
        byte[] textBytes = text.getBytes(Charset.forName("UTF-8"));

        int languageLength = languageBytes.length;
        int textLength = textBytes.length;

        byte[] payload = new byte[1 + languageLength + textLength];
        payload[0] = (byte) languageLength;

        System.arraycopy(languageBytes, 0, payload, 1, languageLength);
        System.arraycopy(textBytes, 0, payload, 1 + languageLength, textLength);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }

    private boolean writeNdefMessage(Tag tag, NdefMessage message) {
        try {
            Ndef ndef = Ndef.get(tag);

            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Log.e(TAG, "Tag is not writable");
                    ndef.close();
                    return false;
                }

                int size = message.toByteArray().length;
                if (size > ndef.getMaxSize()) {
                    Log.e(TAG, "Message is too large for tag");
                    ndef.close();
                    return false;
                }

                ndef.writeNdefMessage(message);
                ndef.close();
                return true;

            } else {
                // Try to format the tag
                NdefFormatable formatable = NdefFormatable.get(tag);
                if (formatable != null) {
                    formatable.connect();
                    formatable.format(message);
                    formatable.close();
                    return true;
                } else {
                    Log.e(TAG, "Tag is not NDEF compatible");
                    return false;
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "IOException while writing NDEF message", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception while writing NDEF message", e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}