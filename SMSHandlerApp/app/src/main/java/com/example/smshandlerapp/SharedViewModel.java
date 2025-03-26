package com.example.smshandlerapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Boolean> smsReceived = new MutableLiveData<>();

    public void notifySmsReceived() {
        smsReceived.setValue(true);
    }

    public LiveData<Boolean> getSmsReceived() {
        return smsReceived;
    }
}