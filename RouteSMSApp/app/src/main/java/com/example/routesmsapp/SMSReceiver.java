package com.example.routesmsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.ArrayList;

public class SMSReceiver extends BroadcastReceiver {
    private static ArrayList<String> receivedMessages = new ArrayList<>();
    private static MainActivity mainActivity;
    private RequestQueue requestQueue;

    public static void setMainActivity(MainActivity activity) {
        mainActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        requestQueue = Volley.newRequestQueue(context);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (Object pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                String message = sms.getMessageBody();
                String mobile = sms.getOriginatingAddress();

                if (message != null && message.toLowerCase().startsWith("route")) {
                    receivedMessages.add(message);
                    if (mainActivity != null) {
                        mainActivity.updateReceivedMessages(receivedMessages);
                    }

                    String origin = extractAfter(message, "from");
                    String destination = extractAfter(message, "to");
                    saveToDatabase(mobile, origin, destination);
                }
            }
        }
    }

    private String extractAfter(String message, String keyword) {
        int index = message.toLowerCase().indexOf(keyword) + keyword.length();
        if (index >= keyword.length() && index < message.length()) {
            return message.substring(index).trim();
        }
        return "";
    }

    private void saveToDatabase(String mobile, String origin, String destination) {
        String url = "https://r8v29pu3kd.execute-api.ap-south-1.amazonaws.com/sms_entry";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("sender_mob", mobile);
            jsonBody.put("source", origin.isEmpty() ? "Unknown" : origin);
            jsonBody.put("destination", destination.isEmpty() ? "Unknown" : destination);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> Log.d("SMSReceiver", "Successfully saved to database: " + response.toString()),
                error -> Log.e("SMSReceiver", "Error saving to database: " + error.getMessage()));
        requestQueue.add(request);
    }
}