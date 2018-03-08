package com.google.firebase.codelab.friendlychat;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import org.json.JSONArray;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by hubbler-sudesh on 07/03/18.
 */

public class CustomFirebaseChatAdapter extends FirebaseRecyclerAdapter {


    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;


        public MessageViewHolder(View v) {
            super(v);
            messageTextView =  itemView.findViewById(R.id.text);
            messageImageView =  itemView.findViewById(R.id.messageImageView);
            messengerTextView =  itemView.findViewById(R.id.userName);
            messengerImageView = itemView.findViewById(R.id.userProfilePic);
        }
    }

    public CustomFirebaseChatAdapter(FirebaseRecyclerOptions options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(RecyclerView.ViewHolder holder, int position, Object model) {

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType==0)
            return new OneToOneChat.MessageViewHolder(inflater.inflate(R.layout.item_message, parent, false));
        else
            return new OneToOneChat.MessageViewHolder(inflater.inflate(R.layout.item_message, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        if (position==0)
            return 0;
        else
            return 1;
    }
}
