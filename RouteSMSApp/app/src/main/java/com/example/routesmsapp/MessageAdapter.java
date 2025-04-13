package com.example.routesmsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<String> messages = new ArrayList<>();
    private static final String TAG = "MessageAdapter";

    public void updateMessages(List<String> newMessages) {
        Log.d(TAG, "Updating messages: " + (newMessages != null ? newMessages.toString() : "null"));
        if (newMessages != null) {
            synchronized (messages) {
                messages.clear();
                int sizeToAdd = Math.min(newMessages.size(), 5); // Limit to 5 messages
                messages.addAll(newMessages.subList(newMessages.size() - sizeToAdd, newMessages.size()));
            }
            notifyDataSetChanged();
            Log.d(TAG, "Messages updated, notifying adapter with " + messages.size() + " items");
        } else {
            Log.w(TAG, "newMessages is null, skipping update");
        }
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        synchronized (messages) {
            if (position >= 0 && position < messages.size()) {
                holder.messageTextView.setText(messages.get(position));
            } else {
                Log.w(TAG, "Position " + position + " out of bounds for messages size " + messages.size());
                holder.messageTextView.setText("Invalid message");
            }
        }
    }

    @Override
    public int getItemCount() {
        synchronized (messages) {
            return messages.size();
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            if (messageTextView == null) {
                Log.e("MessageViewHolder", "messageTextView not found in list_item layout");
            }
        }
    }
}