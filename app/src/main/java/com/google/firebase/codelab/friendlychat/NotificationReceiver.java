package com.google.firebase.codelab.friendlychat;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by hubbler-sudesh on 19/03/18.
 */

public class NotificationReceiver extends AppCompatActivity{


    // Key for the string that's delivered in the action's intent.
    private static final String KEY_TEXT_REPLY = "key_text_reply";
    private GroupChat groupChat;
    // mRequestCode allows you to update the notification.
    int mRequestCode = 1000;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        groupChat = new GroupChat();
        CharSequence returnMessage;
        returnMessage = getMessageText(getIntent());

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText("Message sent: "+returnMessage);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(mRequestCode, mBuilder.build());
    }

    private CharSequence getMessageText( Intent intent ) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        String groupName = intent.getStringExtra("groupName");
        String senderUid = intent.getStringExtra("senderUid");
        Log.i("groupName ", "Group Name: "+groupName);
        if (remoteInput != null) {


            CharSequence reply = remoteInput.getCharSequence(KEY_TEXT_REPLY);
            groupChat.addReplyFromNotification(reply.toString(),groupName,senderUid);

            return reply;
        }
        else {
            return null;
        }
    }

//    @Override
//    public void onReceive(Context context, Intent intent) {
//
//        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
//
//        if(remoteInput!=null)
//        {
//            CharSequence replyMsg = remoteInput.getCharSequence(MyFirebaseMessagingService.KEY_TEXT_REPLY);
//
//
//            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
//
//            notificationBuilder
//                    .setSmallIcon(R.mipmap.ic_launcher)
//                    .setContentTitle("Reply: "+replyMsg);
//
//            NotificationManager notificationManager = (NotificationManager) context.
//                    getSystemService(Context.NOTIFICATION_SERVICE);
//            notificationManager.notify(MyFirebaseMessagingService.NOTIFICATION_ID, notificationBuilder.build());
//        }
//
//    }
}
