package com.example.routesmsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SMSReceiver extends BroadcastReceiver {
    private static MainActivity mainActivity;
    private static RequestQueue requestQueue;

    public static void setMainActivityAndRequestQueue(MainActivity activity, RequestQueue queue) {
        mainActivity = activity;
        requestQueue = queue;
        if (queue == null) Log.e("SMSReceiver", "RequestQueue is null during setup");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SMSReceiver", "SMS received intent: " + (intent != null ? intent.getAction() : "null"));
        if (intent != null && intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    List<String> formattedMessages = new ArrayList<>();
                    for (Object pdu : pdus) {
                        SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                        String senderMob = sms.getOriginatingAddress();
                        String message = sms.getMessageBody();
                        Log.d("SMSReceiver", "Received SMS - Sender: " + senderMob + ", Message: " + message);

                        if (message != null && (message.toLowerCase().startsWith("route") || message.toLowerCase().startsWith("route"))) {
                            String lowerMessage = message.toLowerCase(); // Case-insensitive parsing
                            int fromIndex = lowerMessage.indexOf("from");
                            int toIndex = lowerMessage.indexOf("to", fromIndex + 1);

                            if (fromIndex >= 0 && toIndex > fromIndex) {
                                // Check for multiple "to" instances
                                int nextToIndex = lowerMessage.indexOf("to", toIndex + 1);
                                if (nextToIndex == -1) {
                                    String source = lowerMessage.substring(fromIndex + 5, toIndex).trim(); // Skip "from"
                                    String destination = lowerMessage.substring(toIndex + 2).trim(); // Skip "to"
                                    if (!source.isEmpty() && !destination.isEmpty()) {
                                        String formattedMessage = "From: " + senderMob + "\nMessage: " + message;
                                        formattedMessages.add(formattedMessage);
                                        saveToDatabase(senderMob, source, destination);
                                    } else {
                                        Log.w("SMSReceiver", "Empty source or destination, skipping: " + message);
                                    }
                                } else {
                                    Log.w("SMSReceiver", "Multiple 'to' found, skipping ambiguous message: " + message);
                                }
                            } else {
                                Log.w("SMSReceiver", "Missing or invalid 'from' or 'to', skipping: " + message);
                            }
                        } else {
                            Log.d("SMSReceiver", "Message does not start with 'Route' or 'route', skipping: " + message);
                        }
                    }
                    if (!formattedMessages.isEmpty() && mainActivity != null) {
                        mainActivity.updateReceivedMessages(formattedMessages);
                    } else {
                        Log.w("SMSReceiver", "No valid messages to update or mainActivity is null: " + (mainActivity == null ? "null" : "not null"));
                    }
                } else {
                    Log.w("SMSReceiver", "pdus is null, no SMS data");
                }
            } else {
                Log.w("SMSReceiver", "Bundle is null, no SMS data");
            }
        }
    }

    private void saveToDatabase(String senderMob, String source, String destination) {
        String url = "https://r8v29pu3kd.execute-api.ap-south-1.amazonaws.com/sms_entry";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("sender_mob", senderMob);
            jsonBody.put("source", source);
            jsonBody.put("destination", destination);
        } catch (Exception e) {
            Log.e("SMSReceiver", "Error creating JSON for database save: " + e.getMessage());
            return;
        }

        Log.d("SMSReceiver", "Sending save request to " + url + " with body: " + jsonBody.toString());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    try {
                        String messageResponse = response.getString("message");
                        Log.d("SMSReceiver", "Successfully saved to database: " + messageResponse);
                    } catch (Exception e) {
                        Log.e("SMSReceiver", "Error parsing save response: " + e.getMessage());
                    }
                },
                error -> Log.e("SMSReceiver", "Error saving to database: " + error.getMessage() + ", status: " + (error.networkResponse != null ? error.networkResponse.statusCode : "null")));
        if (requestQueue != null) {
            requestQueue.add(request);
        } else {
            Log.e("SMSReceiver", "RequestQueue is null, cannot send save request");
        }
    }
}