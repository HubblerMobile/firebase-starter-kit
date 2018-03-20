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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyFirebaseMessagingService extends FirebaseMessagingService{

    private static final String TAG = "MyFMService";
    public static final String KEY_TEXT_REPLY = "key_text_reply";
    public static final int NOTIFICATION_ID = 200;
    int mRequestCode = 1000;

    @Override
    public void onCreate() {
        super.onCreate();

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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public void onMessageReceived(RemoteMessage remoteMessage) {


        String notification_title = remoteMessage.getData().get("title");
        String notification_msg = remoteMessage.getData().get("body");

//        String click_action = remoteMessage.getNotification().getClickAction();

        String senderId = remoteMessage.getData().get("sender");
        String receiverId = remoteMessage.getData().get("receiver");
        String grpName = remoteMessage.getData().get("groupName");
        String profilePictureLink = remoteMessage.getData().get("icon");

        Bitmap profilePicture = getBitmapFromURL(profilePictureLink);

        createNotification(notification_title,notification_msg,profilePicture);


        Log.i(TAG, "onMessageReceived: Profile picture "+profilePictureLink);

        Intent resultIntent;

        Intent replyIntent = new Intent(this, NotificationReceiver.class);
        replyIntent.putExtra("groupName",grpName);
        replyIntent.putExtra("senderUid",senderId);


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(NotificationReceiver.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(replyIntent);

//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(
//                        0,
//                        PendingIntent.FLAG_UPDATE_CURRENT
//                );

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


//        Intent replyIntent = new Intent( this, NotificationReceiver.class );


        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

//        PendingIntent resultPendingIntent =
//                PendingIntent.getActivity(
//                        this,
//                        0,
//                        resultIntent,
//                        PendingIntent.FLAG_UPDATE_CURRENT
//                );



        PendingIntent replyPendingIntent = PendingIntent.getActivity(this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT );

//        NotificationCompat.Action sendAction =
//                new NotificationCompat.Action.Builder(R.drawable.ic_send_arrow, "Reply", replyPendingIntent)
//                        .addRemoteInput(remoteInput)
//                        .build();

//        NotificationCompat.Action muteAction =
//                new NotificationCompat.Action.Builder(R.drawable.ic_send_arrow, "Mute", resultPendingIntent)
//                        .addRemoteInput(remoteInput)
//                        .build();

//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
//        notificationBuilder
//                .setLargeIcon(profilePicture)
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle(notification_title)
//                .setContentText(notification_msg)
//                .setGroup("New abcd")
//                .setGroupSummary(true)
//                .setColor(ContextCompat.getColor(this, R.color.skyBlue))
//                .setAutoCancel(true)
////                .addAction(sendAction)
////                .addAction(muteAction)
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

//        NotificationCompat.Builder notificationBuilderTwo = new NotificationCompat.Builder(this);
//        notificationBuilder
//                .setLargeIcon(profilePicture)
//                .setContentTitle(notification_title)
//                .setContentText(notification_msg)
//                .setColor(ContextCompat.getColor(this, R.color.skyBlue))
//                .setAutoCancel(true)
//                .addAction(action)
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

//        Spannable sb = new SpannableString("Bold this and italic that.");
//        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        sb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 14, 20, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        inboxStyle.addLine(sb);

//        notificationBuilder.setStyle(new NotificationCompat.InboxStyle()
//            .addLine("1st Notification")
//            .addLine("this is amazing line let me see how it shows up in notification if it is long string character")
//            .setBigContentTitle(grpName)
//            .setSummaryText("45 messages from 2 chats")
//            );

//        notificationBuilderTwo.setStyle(new NotificationCompat.InboxStyle()
//            .addLine("2nd Notification")
//        .addLine("another line of 2nd notification")
//        .setBigContentTitle("damn Group")
//        .setSummaryText("Greedings")
//        )
//                .setGroup("New abcd")
//                .setGroupSummary(true);


//        notificationBuilder.setContentIntent(resultPendingIntent);
//        notificationBuilderTwo.setContentIntent(resultPendingIntent);
        int mNotificationId = (int) System.currentTimeMillis();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Show it
//        mNotificationManager.notify(mRequestCode, notificationBuilder.build());

//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            notificationManager.notify(NOTIFICATION_ID,notificationBuilder.build());
//            notificationManager.notify(mNotificationId,notificationBuilderTwo.build());

//        Bundle bundle = RemoteInput.getResultsFromIntent(resultIntent);
//        String replyFromNotification;
//
//        if (remoteInput != null) {
//            replyFromNotification = bundle.getCharSequence(KEY_TEXT_REPLY).toString();
//            Toast.makeText(this, replyFromNotification, Toast.LENGTH_SHORT).show();
//        }
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

    public void createNotification(String notificationTitle, String notificationMsg, Bitmap profilePicture)
    {
        String replyLabel = "Reply Message";
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel(replyLabel)
                .build();

        Intent resultIntent = new Intent(this, NotificationReceiver.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(NotificationReceiver.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(R.mipmap.ic_launcher, "Reply", resultPendingIntent)
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .addAction(action)
                        .setLargeIcon(profilePicture)
                        .setColor(ContextCompat.getColor(this,R.color.skyBlue))
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(notificationTitle)
                        .setContentText(notificationMsg);

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Show it
        mNotificationManager.notify(mRequestCode, mBuilder.build());
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
