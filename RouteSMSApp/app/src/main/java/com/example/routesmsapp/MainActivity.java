package com.example.routesmsapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private RecyclerView receivedRecyclerView, responseRecyclerView;
    private MessageAdapter receivedAdapter, responseAdapter;
    private List<String> receivedMessages = new ArrayList<>();
    private List<String> responseMessages = new ArrayList<>();
    private RequestQueue requestQueue;
    private ScheduledExecutorService executorService;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";
    private static final String HARD_CODED_TOKEN = "";
    private static final int MAX_RETRIES = 3;
    private static final String API2_URL = "https://9nvd916obg.execute-api.ap-south-1.amazonaws.com/router_app_reply";
    private static final String API3_URL = "https://x19u4o3wj5.execute-api.ap-south-1.amazonaws.com/router_reply_read";
    private static final String API1_URL = "https://r8v29pu3kd.execute-api.ap-south-1.amazonaws.com/sms_entry";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receivedRecyclerView = findViewById(R.id.receivedRecyclerView);
        responseRecyclerView = findViewById(R.id.responseRecyclerView);

        receivedAdapter = new MessageAdapter();
        responseAdapter = new MessageAdapter();

        receivedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        responseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        receivedRecyclerView.setAdapter(receivedAdapter);
        responseRecyclerView.setAdapter(responseAdapter);

        requestQueue = Volley.newRequestQueue(this);
        if (requestQueue == null) Log.e(TAG, "RequestQueue initialization failed");

        SMSReceiver.setMainActivityAndRequestQueue(this, requestQueue);

        checkAndRequestPermissions();

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::checkResponses, 0, 30, TimeUnit.SECONDS);

        Log.d(TAG, "MainActivity initialized and RecyclerViews set up");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS};
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission " + permissions[i] + " denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void updateReceivedMessages(List<String> messages) {
        Log.d(TAG, "Received messages to update UI: " + (messages != null ? messages.toString() : "null"));
        if (messages != null && !messages.isEmpty()) {
            synchronized (receivedMessages) {
                receivedMessages.clear();
                receivedMessages.addAll(messages);
            }
            runOnUiThread(() -> {
                if (receivedAdapter != null) {
                    receivedAdapter.updateMessages(new ArrayList<>(receivedMessages));
                    Log.d(TAG, "UI updated with received messages: " + receivedMessages.toString());
                } else {
                    Log.e(TAG, "receivedAdapter is null, cannot update UI");
                }
            });
        } else {
            Log.w(TAG, "No messages to update UI, list is empty or null");
        }
    }

    private void checkResponses() {
        Log.d(TAG, "Checking responses at: " + System.currentTimeMillis());
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, API2_URL, null,
                response -> {
                    Log.d(TAG, "Received response from API #2: " + response.toString());
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            String senderMob = obj.getString("sender_mob");
                            String reply = obj.getString("reply");
                            int id = obj.getInt("id");
                            Log.d(TAG, "Processing id: " + id + ", sender: " + senderMob + ", reply: " + reply);
                            new SendSmsTask().execute(new SmsData(senderMob, reply));
                            synchronized (responseMessages) {
                                responseMessages.add(reply);
                            }
                            if (responseAdapter != null) {
                                runOnUiThread(() -> responseAdapter.updateMessages(new ArrayList<>(this.responseMessages)));
                            } else {
                                Log.e(TAG, "responseAdapter is null, cannot update UI");
                            }
                            updateResponseInDatabase(id, 0);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing response at index " + i + ": " + e.getMessage());
                        }
                    }
                },
                error -> Log.e(TAG, "Error fetching responses: " + error.getMessage() + ", status: " + (error.networkResponse != null ? error.networkResponse.statusCode : "null")));
        if (requestQueue != null) {
            requestQueue.add(request);
        } else {
            Log.e(TAG, "RequestQueue is null, cannot add request");
        }
    }

    private void updateResponseInDatabase(int id, int retryCount) {
        String url = API3_URL;
        String token = HARD_CODED_TOKEN;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("id", id);
            jsonBody.put("token", token);
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON for id " + id + ": " + e.getMessage());
            return;
        }

        Log.d(TAG, "Sending POST update request for id " + id + ", retry: " + retryCount + ", body: " + jsonBody.toString());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    try {
                        String message = response.getString("message");
                        int updatedId = response.getInt("id");
                        if ("Update successful".equals(message)) {
                            Log.d(TAG, "Update successful for id " + updatedId + ": " + message);
                        } else {
                            Log.w(TAG, "Unexpected success response for id " + updatedId + ": " + response.toString());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing success response for id " + id + ": " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "API #3 error for id " + id + ": " + error.getMessage() + ", status: " + (error.networkResponse != null ? error.networkResponse.statusCode : "null"));
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String errorMsg = new JSONObject(new String(error.networkResponse.data)).getString("error");
                            Log.e(TAG, "Update failed for id " + id + ": " + errorMsg + ", retry: " + retryCount);
                            if (retryCount < MAX_RETRIES && "Wrong credentials".equals(errorMsg)) {
                                new android.os.Handler().postDelayed(() -> updateResponseInDatabase(id, retryCount + 1), 1000 * (retryCount + 1));
                            } else if (retryCount >= MAX_RETRIES) {
                                Toast.makeText(this, "Failed to update id " + id + " after " + MAX_RETRIES + " retries: " + errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error response for id " + id + ": " + e.getMessage());
                        }
                    } else {
                        if (retryCount < MAX_RETRIES) {
                            new android.os.Handler().postDelayed(() -> updateResponseInDatabase(id, retryCount + 1), 1000 * (retryCount + 1));
                        } else {
                            Toast.makeText(this, "Failed to update id " + id + " after " + MAX_RETRIES + " retries: Connection issue", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        if (requestQueue != null) {
            requestQueue.add(request);
        } else {
            Log.e(TAG, "RequestQueue is null, cannot add request");
        }
    }

    private static class SmsData {
        String mobileNo, message;
        SmsData(String mobileNo, String message) {
            this.mobileNo = mobileNo;
            this.message = message;
        }
    }

    private class SendSmsTask extends AsyncTask<SmsData, Void, Void> {
        @Override
        protected Void doInBackground(SmsData... params) {
            if (params[0] != null) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(params[0].mobileNo, null, params[0].message, null, null);
                Log.d(TAG, "SMS sent to " + params[0].mobileNo + ": " + params[0].message);
            } else {
                Log.e(TAG, "SmsData is null, cannot send SMS");
            }
            return null;
        }
    }
}