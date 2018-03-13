/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.codelab.friendlychat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService{

    private static final String TAG = "MyFMService";
    Bitmap bitmap = getBitmapFromURL("Your URL");

    public Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void onMessageReceived(RemoteMessage remoteMessage) {


        String notification_title = remoteMessage.getNotification().getTitle();
        String notification_msg = "WD "+remoteMessage.getNotification().getBody();

        String click_action = remoteMessage.getNotification().getClickAction();

        String senderId = remoteMessage.getData().get("sender");
        String receiverId = remoteMessage.getData().get("receiver");
        String grpName = remoteMessage.getData().get("groupName");
        String notification_icon = remoteMessage.getNotification().getIcon();
//        Bitmap myIconBitMap = getBitmapFromURL(notification_icon);

//        Bundle bundle = getIntent().getExtras();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,"notific id");
        notificationBuilder
//                .setLargeIcon(myIconBitMap)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notification_title)
                .setContentText(notification_msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Intent resultIntent = new Intent(click_action);

        if(senderId!=null || receiverId!=null)
        {
            resultIntent.putExtra("sender",senderId);
            resultIntent.putExtra("receiver",receiverId);
        }
        else if( grpName!=null)
        {
            resultIntent.putExtra("grpName",grpName);
        }


        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        notificationBuilder.setContentIntent(resultPendingIntent);
        int mNotificationId = (int) System.currentTimeMillis();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(mNotificationId,notificationBuilder.build());

    }

    private void showNotification(Map<String, String> payload) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Log.i(TAG, "showNotification: Showing Notification....PLZ CHECK");
    }



}
