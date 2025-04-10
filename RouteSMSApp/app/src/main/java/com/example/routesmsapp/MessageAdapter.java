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
        Log.d(TAG, "Updating messages: " + newMessages.toString());
        if (newMessages.size() > 5) {
            messages = new ArrayList<>(newMessages.subList(newMessages.size() - 5, newMessages.size()));
        } else {
            messages = new ArrayList<>(newMessages);
        }
        notifyDataSetChanged(); // Ensure this notifies the RecyclerView
        Log.d(TAG, "Messages updated, notifying adapter");
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        holder.messageTextView.setText(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView; // Corrected variable name to match the ID

        MessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView); // Updated to match
        }
    }
}   