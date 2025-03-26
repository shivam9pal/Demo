package com.example.smshandlerapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private ListView receivedListView;
    private ListView sentListView;
    private ArrayAdapter<String> receivedAdapter;
    private ArrayAdapter<String> sentAdapter;
    private DatabaseHelper dbHelper;
    private SharedViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receivedListView = findViewById(R.id.receivedListView);
        sentListView = findViewById(R.id.sentListView);
        dbHelper = new DatabaseHelper(this);

        receivedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dbHelper.getReceivedMessages());
        sentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dbHelper.getSentResponses());
        receivedListView.setAdapter(receivedAdapter);
        sentListView.setAdapter(sentAdapter);

        // Initialize ViewModel and pass to SmsReceiver
        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        SmsReceiver.setViewModel(viewModel);

        // Observe LiveData for SMS updates
        viewModel.getSmsReceived().observe(this, received -> {
            if (received != null && received) {
                updateLists();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS
            }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateLists();
        }
    }

    private void updateLists() {
        receivedAdapter.clear();
        receivedAdapter.addAll(dbHelper.getReceivedMessages());
        receivedAdapter.notifyDataSetChanged();

        sentAdapter.clear();
        sentAdapter.addAll(dbHelper.getSentResponses());
        sentAdapter.notifyDataSetChanged();
    }
}