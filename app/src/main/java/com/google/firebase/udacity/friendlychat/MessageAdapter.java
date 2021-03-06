package com.google.firebase.udacity.friendlychat;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.udacity.chat.R;

import java.util.List;

public class MessageAdapter extends ArrayAdapter<FriendlyMessage> {
    public MessageAdapter(Context context, int resource, List<FriendlyMessage> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message, parent, false);
        }

        final ImageView photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
        TextView messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
        TextView authorTextView = (TextView) convertView.findViewById(R.id.nameTextView);

        final FriendlyMessage message = getItem(position);

        boolean isPhoto = message.getPhotoUrl() != null;
        if (isPhoto) {
            messageTextView.setVisibility(View.GONE);
            authorTextView.setText(message.getName());
            photoImageView.setVisibility(View.VISIBLE);
            convertView.findViewById(R.id.imageviewcontainer).setVisibility(View.VISIBLE);
            photoImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadImage(photoImageView, message);
                }
            });
            for (int i = 0; i < 100; i++) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        loadImage(photoImageView, message);
                    }
                }, 500);
            }
        } else {
            messageTextView.setVisibility(View.VISIBLE);
            photoImageView.setVisibility(View.GONE);
            messageTextView.setText(message.getText());
            authorTextView.setText(message.getName());
            convertView.findViewById(R.id.imageviewcontainer).setVisibility(View.GONE);
            if (messageTextView.getText().toString().trim().equals("")) {
                messageTextView.setTextSize(0);
                messageTextView.setVisibility(View.GONE);
            } else {
                messageTextView.setTextSize(27);
            }
        }


        return convertView;
    }

    private void loadImage(final ImageView photoImageView, final FriendlyMessage message) {
        Glide.with(photoImageView.getContext())
                .load(message.getPhotoUrl())
                .into(photoImageView);
    }
}
