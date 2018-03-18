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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService{

    private static final String TAG = "MyFMService";

    @Override
    public void onCreate() {
//        super.onCreate();
//
//        if(type.equals("1001")) {
//            Handler handler = new Handler(Looper.getMainLooper());
//            handler.post(new Runnable() {
//                public void run() {
//                    MyFirebaseMessagingService common = new CommonClass(getApplication());
//                    CommonClass.MyTaskSendLog.execute(getApplicationContext(), DeviceDetails,lines);
//                }
//            });
//        }
    }

    public Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return getCircleBitmap(myBitmap);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

//    private Bitmap getCircleBitmap(Bitmap bitmap) {
//        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
//                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
//        final Canvas canvas = new Canvas(output);
//
//        final int color = Color.RED;
//        final Paint paint = new Paint();
//        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
//        final RectF rectF = new RectF(rect);
//
//        paint.setAntiAlias(true);
//        canvas.drawARGB(0, 0, 0, 0);
//        paint.setColor(color);
//        canvas.drawOval(rectF, paint);
//
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
//        canvas.drawBitmap(bitmap, rect, rect, paint);
//
//        bitmap.recycle();
//
//        return output;
//    }

    public void onMessageReceived(RemoteMessage remoteMessage) {


        String notification_title = remoteMessage.getData().get("title");
        String notification_msg = remoteMessage.getData().get("body");

//        String click_action = remoteMessage.getNotification().getClickAction();

        String senderId = remoteMessage.getData().get("sender");
        String receiverId = remoteMessage.getData().get("receiver");
        String grpName = remoteMessage.getData().get("groupName");
        String profilePictureLink = remoteMessage.getData().get("icon");

        Bitmap profilePicture = getBitmapFromURL(profilePictureLink);

//        DownloadThumbnail downloadThumbnail = new DownloadThumbnail();
//        new DownloadThumbnail(this).execute(profilePictureLink,profilePicture);


        Log.i(TAG, "onMessageReceived: Profile picture "+profilePictureLink);


        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,"notific id");
        notificationBuilder
                .setLargeIcon(profilePicture)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notification_title)
                .setContentText(notification_msg)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

//        Spannable sb = new SpannableString("Bold this and italic that.");
//        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        sb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 14, 20, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        inboxStyle.addLine(sb);

        notificationBuilder.setStyle(new NotificationCompat.InboxStyle()
            .addLine("Hi this is awesome")
            .addLine("this is amazing line let me see how it shows up in notification if it is long string character")
            .setBigContentTitle(grpName)
            .setSummaryText("45 messages from 2 chats")
            )
        .setGroup(grpName)
        .setGroupSummary(true);
        Intent resultIntent;


        if(senderId!=null || receiverId!=null)
        {
            resultIntent = new Intent(this,OneToOneChat.class);
            resultIntent.putExtra("sender",senderId);
            resultIntent.putExtra("receiver",receiverId);
        }
        else if( grpName!=null)
        {
            resultIntent = new Intent(this,GroupChat.class);
            resultIntent.putExtra("groupName",grpName);
        }
        else {
            resultIntent = new Intent();
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

    private Bitmap downloadBitmap(String strURL)
    {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return getCircleBitmap(myBitmap);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }



}


//public class DownloadThumbnail extends AsyncTask<String,Void,Bitmap> {
//
//    @Override
//    protected Bitmap doInBackground(String... strings) {
//
//        Bitmap image = downloadBitmap(strings[0]);
//        return image;
//    }
//
//    @Override
//    protected void onPostExecute(Bitmap bitmap) {
//
//        if(bitmap != null)
//        {
//
//        }
//        super.onPostExecute(bitmap);
//    }
//}
