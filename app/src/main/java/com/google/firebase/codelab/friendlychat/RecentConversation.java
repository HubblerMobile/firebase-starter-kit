package com.google.firebase.codelab.friendlychat;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class RecentConversation extends AppCompatActivity {

    FloatingActionButton fab_selectChatUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_conversation);

        fab_selectChatUser = findViewById(R.id.selectChatUser);

        fab_selectChatUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RecentConversation.this,SelectUserToChat.class));
            }
        });


    }
}
