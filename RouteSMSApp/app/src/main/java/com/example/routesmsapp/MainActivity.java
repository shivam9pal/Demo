package com.example.routesmsapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private RecyclerView receivedRecyclerView, responseRecyclerView;
    private MessageAdapter receivedAdapter, responseAdapter;
    private List<String> responseMessages = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    private RequestQueue requestQueue;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";

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

        SMSReceiver.setMainActivity(this); // Ensure this is called

        checkAndRequestPermissions();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkResponses();
                handler.postDelayed(this, 30000);
            }
        }, 30000);

        Log.d(TAG, "MainActivity initialized and RecyclerViews set up");
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS
        };
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
        Log.d(TAG, "Updating received messages: " + messages.toString());
        runOnUiThread(() -> {
            receivedAdapter.updateMessages(messages);
            Log.d(TAG, "UI updated with new messages");
        });
    }

    private void checkResponses() {
        String url = "YOUR_API_ENDPOINT_2"; // Replace with actual API endpoint
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            String mobileNo = obj.getString("mobile_no");
                            String reply = obj.getString("reply");
                            boolean replied = obj.getBoolean("replied");

                            if (!replied) {
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(mobileNo, null, reply, null, null);
                                responseMessages.add(reply);
                                responseAdapter.updateMessages(responseMessages);
                                updateResponseInDatabase(mobileNo, reply);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Toast.makeText(MainActivity.this, "Error fetching responses: " + error.getMessage(), Toast.LENGTH_SHORT).show());
        requestQueue.add(request);
    }

    private void updateResponseInDatabase(String mobileNo, String reply) {
        String url = "YOUR_API_ENDPOINT_3"; // Replace with actual API endpoint
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("mobile_no", mobileNo);
            jsonBody.put("reply", reply);
            jsonBody.put("replied", true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, jsonBody,
                response -> {},
                error -> Toast.makeText(this, "Error updating response: " + error.getMessage(), Toast.LENGTH_SHORT).show());
        requestQueue.add(request);
    }
}