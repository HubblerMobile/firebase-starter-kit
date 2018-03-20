/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener {

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private DatabaseReference mFirebaseDatabaseReference;
    private DatabaseReference messagesRef;
    private DatabaseReference typingStatusRef;
    private DatabaseReference notificationRef;
    private DatabaseReference usersRef;

    private FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> mFirebaseAdapter;
    private int timeDelayForCheck = 1;
    private boolean user_typing = false;
    private List<String> usersTyping = new ArrayList<String>();
    private Map<String,String> userWhoAreTyping = new HashMap<>();

    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    public static final String TYPING_STATUS = "users_typing";
    public static final String USERS_AVAILABLE = "usersList";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 100;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private String mUsername;
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;
    int mRequestCode = 1000;
    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";

    private static final String KEY_TEXT_REPLY = "key_text_reply";

    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;
    private TextView typingStatus;
    boolean isDelayCheckRunning = false;

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;


        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.text);
            messageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            messengerTextView = (TextView) itemView.findViewById(R.id.userName);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.userProfilePic);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        messagesRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD);            //Firebase message branch
        messagesRef.keepSynced(true);
        notificationRef = mFirebaseDatabaseReference.child("Notification");
        typingStatusRef = mFirebaseDatabaseReference.child(TYPING_STATUS);          // Firebase typing stauts branch
        usersRef = mFirebaseDatabaseReference.child(USERS_AVAILABLE);


        CheckTypingStatus();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.progressBar);
        mMessageRecyclerView = findViewById(R.id.messageRecyclerView);
        typingStatus = findViewById(R.id.typingStatus);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);


        typingStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                typingStatus.setVisibility(View.VISIBLE);
                if(usersTyping==null)
                    return;
                userWhoAreTyping = (Map<String, String>) dataSnapshot.getValue();
                Log.i(TAG, "onDataChange: dict_usersTyping "+userWhoAreTyping);
                Log.i(TAG, "onDataChange: User who are typing "+userWhoAreTyping);


                if(userWhoAreTyping==null)
                {
                    typingStatus.setText("");
                    return;
                }
//                userWhoAreTyping.remove("hubbler");
                if(userWhoAreTyping.containsKey(mUsername))     // remove our name from list for the right no. of users typing.
                {
                    userWhoAreTyping.remove(mUsername);
                }
                Object[] userTyping_arr = userWhoAreTyping.keySet().toArray();//java.lang.indexoutofboundException: invalid array index
                Log.i(TAG, "onDataChange: Usertyping array is "+userTyping_arr.length);
//                Collections.reverse(Arrays.asList(userTyping_arr));
//                Log.i(TAG, "onDataChange: UserTyping array is "+userTyping_arr);
                switch (userTyping_arr.length) {
                    case 0:
                        Log.i(TAG, "onDataChange: the array now is 0");
                        typingStatus.setText("");
                        break;
                    case 1:
                        String temp_users = new String();
                        for(Object s: userTyping_arr)
                        {
                            temp_users += s.toString();
                        }
                        typingStatus.setText(temp_users+" is typing...");
//                        typingStatus.setVisibility(View.INVISIBLE);
                        Log.i(TAG, "onDataChange: The array has "+userTyping_arr.length);
                        break;
                    case 2:
                        Log.i(TAG, "onDataChange: 1 users is typing "+userTyping_arr);
                        temp_users = new String();
                        for(Object s: userTyping_arr)
                        {
                            temp_users += s.toString();
                            temp_users+=" ";
                        }
//                        String entry = userTyping_arr[1].toString();
                        typingStatus.setText(temp_users+" is typing");
                            break;
                    case 3:
                        Log.i(TAG, "onDataChange: 2 users are typing: "+ userTyping_arr[1].toString()+"  "+userTyping_arr[2].toString());
                        temp_users = new String();
                        for(Object s: userTyping_arr)
                        {
                            temp_users += s.toString();
                            temp_users+=" ";
                        }
                        typingStatus.setText(temp_users+" is typing...");
                            break;
//                    case 2:
//                        Log.i(TAG, "onDataChange: 2 users are typing: "+usersTyping.get(0)+ " "+usersTyping.get(1) );
//                        typingStatus.setText(usersTyping.get(0)+", "+usersTyping.get(1)+" are typing");
//                            break;
                    default:
                        Log.i(TAG, "onDataChange: "+userTyping_arr.length+" users are typing");
                        typingStatus.setText(userTyping_arr.length+" users are typing...");
                        break;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void AddRemoteMessage()
    {
        String replyLabel = getString(R.string.reply_label);
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
                new NotificationCompat.Action.Builder(R.drawable.ic_send_arrow, replyLabel, resultPendingIntent)
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .addAction(action)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_send_arrow)
                        .setContentTitle("DevDeeds Says")
                        .setContentText("Do you like my tutorials ?");

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Show it
        mNotificationManager.notify(mRequestCode, mBuilder.build());
    }

    private void userIsTyping(boolean isUserTyping) {

        if(isUserTyping) {

            timeDelayForCheck=1;

            typingStatusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot snapshot: dataSnapshot.getChildren()) {
                            if(snapshot.getValue() == mUsername)
                                return;
                        }
                    if(usersTyping!=null) {
                        typingStatusRef.child(mUsername).setValue("1");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            CheckTypingStatus();
        }
    }

    public void CheckTypingStatus() {

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(timeDelayForCheck > 0) {

                    timeDelayForCheck--;
                    isDelayCheckRunning=true;
                    Log.i(TAG, "run: Timer "+timeDelayForCheck);
                    handler.postDelayed(this,1000);
                }
                else{
                    typingStatus.setVisibility(View.INVISIBLE);
                    isDelayCheckRunning=false;
                    Log.i(TAG, "run: Timer completed");
                    typingStatusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            Log.i(TAG, "onDataChange: Datasnapshot get it "+dataSnapshot);
                            for(DataSnapshot snapshot: dataSnapshot.getChildren()) {
                                if(snapshot.getKey() == mUsername)
                                {
                                    typingStatusRef.child(mUsername).removeValue();
                                    return;
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    @Override
    public void onPause() {
        mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        mFirebaseAdapter.startListening();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }
}
