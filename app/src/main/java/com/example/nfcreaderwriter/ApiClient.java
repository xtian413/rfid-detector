package com.example.nfcreaderwriter;

import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    // 🔹 Replace with your PC's local IP and backend port
    private static final String BASE_URL = "http://192.168.100.10:5000/api/users";
    private static final String TAG = "ApiClient";

    public static void sendUser(String rfid, String name, String balance, String type) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject json = new JSONObject();
                json.put("rfid", rfid);
                json.put("name", name);
                json.put("balance", balance.isEmpty() ? 0 : Double.parseDouble(balance));
                json.put("type", type);

                Log.d(TAG, "Sending JSON: " + json.toString());

                // Use PUT method to update/create user by RFID
                URL url = new URL(BASE_URL + "/" + rfid);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000); // 10 seconds timeout
                conn.setReadTimeout(10000); // 10 seconds timeout

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                InputStream is = responseCode < HttpURLConnection.HTTP_BAD_REQUEST ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder sb = new StringBuilder();
                int ch;
                while ((ch = is.read()) != -1) sb.append((char) ch);
                is.close();

                Log.d(TAG, "Response body: " + sb.toString());

                if (responseCode >= 200 && responseCode < 300) {
                    Log.d(TAG, "✅ User data sent successfully!");
                } else {
                    Log.e(TAG, "❌ Server returned error: " + responseCode);
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error sending user data", e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}