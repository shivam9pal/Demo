package com.example.smshandlerapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    private static SharedViewModel viewModel;

    public static void setViewModel(SharedViewModel model) {
        viewModel = model;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                    String sender = sms.getOriginatingAddress();
                    String message = sms.getMessageBody();

                    DatabaseHelper dbHelper = new DatabaseHelper(context);
                    dbHelper.insertMessage(sender, message);

                    String response = dbHelper.getResponse(sender);
                    dbHelper.insertResponse(sender, response);

                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(sender, null, response, null, null);

                    // Notify MainActivity via ViewModel
                    if (viewModel != null) {
                        viewModel.notifySmsReceived();
                    }
                }
            }
        }
    }
}