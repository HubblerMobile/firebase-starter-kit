package com.google.firebase.codelab.friendlychat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by hubbler-sudesh on 02/03/18.
 */

public class GroupChat extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener  {

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private DatabaseReference mFirebaseDatabaseReference;
    private DatabaseReference conversationsRef;
    private DatabaseReference typingStatusRef;
    private DatabaseReference grpChatRef;
    private DatabaseReference receiverChatRef;
    private DatabaseReference recentChats;
    private DatabaseReference notificationRef;
    private DatabaseReference usersRef;


    private FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> mFirebaseAdapter;
    private int timeDelayForCheck = 1;
    private boolean user_typing = false;
    private List<String> usersTyping = new ArrayList<String>();
    private Map<String,String> userWhoAreTyping = new HashMap<>();

    private static final String TAG = "GroupChat";
    public static final String CONVERSATIONS = "conversations";
    public static final String TYPING_STATUS = "users_typing";
    public static final String RECENT_CHATS = "recent_chats";
    public static final String USERS_LIST = "usersList";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 100;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";

    private String mUid;    // userId of the user using device.
    private String mUsername;
    private String mPhotoUrl;

    private String senderUid;   //passed from prv activity through intent.

    private String recieverUid;
    private String recieverUsername;
    private String recieverPhotoUrl;

    private String grpName;

    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;
    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";

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
            messageTextView =  itemView.findViewById(R.id.text);
            messageImageView =  itemView.findViewById(R.id.messageImageView);
            messengerTextView =  itemView.findViewById(R.id.userName);
            messengerImageView = itemView.findViewById(R.id.userProfilePic);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        conversationsRef = mFirebaseDatabaseReference.child(CONVERSATIONS);            //Firebase message branch
//        conversationsRef.keepSynced(true);
        notificationRef = mFirebaseDatabaseReference.child("Notification");
        typingStatusRef = mFirebaseDatabaseReference.child(TYPING_STATUS);          // Firebase typing stauts branch
        recentChats = mFirebaseDatabaseReference.child(RECENT_CHATS);
        usersRef = mFirebaseDatabaseReference.child(USERS_LIST);
        Intent intent = getIntent();
        grpName = intent.getStringExtra("grpName");
        grpChatRef = conversationsRef.child(grpName);

//            usersRef.child(recieverUid).addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot dataSnapshot) {
//
//                    recieverUsername = dataSnapshot.child("grpName").getValue().toString();
//                    recieverPhotoUrl = dataSnapshot.child("photoUrl").getValue().toString();
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//
//                }
//            });

            Toast.makeText(this, grpName+" is current chat group", Toast.LENGTH_SHORT).show();

        CheckTypingStatus();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        getCurrentUser();

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.progressBar);
        mMessageRecyclerView = findViewById(R.id.messageRecyclerView);
        typingStatus = findViewById(R.id.typingStatus);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        SnapshotParser<FriendlyMessage> parser = new SnapshotParser<FriendlyMessage>() {
            @Override
            public FriendlyMessage parseSnapshot(DataSnapshot dataSnapshot) {
                FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                if (friendlyMessage != null) {
                    friendlyMessage.setId(dataSnapshot.getKey());
                }
                return friendlyMessage;
            }
        };

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

        FirebaseRecyclerOptions<FriendlyMessage> options =
                new FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                        .setQuery(grpChatRef, parser)
                        .build();


        mFirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(options) {
            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder,
                                            int position,
                                            FriendlyMessage friendlyMessage) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (friendlyMessage.getText() != null) {
                    viewHolder.messageTextView.setText(friendlyMessage.getText());
                    viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                    viewHolder.messageImageView.setVisibility(ImageView.GONE);
                } else {
//                  Handle Image view.
                }


                viewHolder.messengerTextView.setText(friendlyMessage.getName());
                if (friendlyMessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(GroupChat.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(GroupChat.this)
                            .load(friendlyMessage.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
        mMessageEditText =  findViewById(R.id.messageEditText);

        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                Log.i(TAG, "beforeTextChanged: ");
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(!isDelayCheckRunning)
                {
                    userIsTyping(true);
                }
            }
        });


        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = new
                        FriendlyMessage(mMessageEditText.getText().toString(),
                        mUsername,
                        mPhotoUrl,
                        null /* no image */);

                //                 Add message to notification branch
                HashMap<String,String> notification_msg = new HashMap<>();
                notification_msg.put("uid",mFirebaseUser.getUid());
                notification_msg.put("email",mFirebaseUser.getEmail());
                notification_msg.put("type","Message");
                notification_msg.put("msg",mMessageEditText.getText().toString());

                grpChatRef.push().setValue(friendlyMessage);
//                receiverChatRef.push().setValue(friendlyMessage);
//                notificationRef.child(mUid).child(recieverUid).setValue(notification_msg);


                final Map<String,Object> lastChatMessage = new HashMap<>();

                Map<String, String> timeStamp = ServerValue.TIMESTAMP;
                lastChatMessage.put("type", "Group");
                lastChatMessage.put("senderUid", mUid);
                lastChatMessage.put("timeStamp", timeStamp);
                lastChatMessage.put("name", grpName);
                lastChatMessage.put("text","\u2713\u2713 "+mMessageEditText.getText().toString());
                lastChatMessage.put("photoUrl", "https://image.freepik.com/free-icon/group-profile-users_318-41953.jpg");

                recentChats.child(mUid).child(grpName).setValue(lastChatMessage);

                recentChats.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for(DataSnapshot userSnap:dataSnapshot.getChildren())
                        {
                            userSnap.child(grpName).getRef().setValue(lastChatMessage);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                lastChatMessage.put("name",grpName);
//                lastChatMessage.put("photoUrl",);
//                recentChats.child(recieverUid).child("Group").child(grpName).setValue(lastChatMessage);

//                if( !mUid.equals(senderUid)) {
//
//                    recentChats.child(mUid).child(recieverUid).setValue(lastChatMessage);
//                }
                mMessageEditText.setText("");
            }
        });

        mAddMessageImageView = (ImageView) findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        });




    }

    private void SetOptionsFirebaseAdapter(DatabaseReference queryBranch) {


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

    public String getDataTimeStamp(long timestamp){
        java.util.Date time=new java.util.Date(timestamp);
//        SimpleDateFormat pre = new SimpleDateFormat("EEE MM dd HH:mm:ss zzz yyyy");
        SimpleDateFormat pre = new SimpleDateFormat("HH:mm:ss");

        //Hear Define your returning date formate
        return pre.format(time);
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

    public void getCurrentUser() {
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {

            mUid = mFirebaseUser.getUid();
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();
                    Log.d(TAG, "Uri: " + uri.toString());

                    FriendlyMessage tempMessage = new FriendlyMessage(null, mUsername, mPhotoUrl,
                            LOADING_IMAGE_URL);
                    mFirebaseDatabaseReference.child(CONVERSATIONS).push()
                            .setValue(tempMessage, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError,
                                                       DatabaseReference databaseReference) {
                                    if (databaseError == null) {
                                        String key = databaseReference.getKey();
                                        StorageReference storageReference =
                                                FirebaseStorage.getInstance()
                                                        .getReference(mFirebaseUser.getUid())
                                                        .child(key)
                                                        .child(uri.getLastPathSegment());

                                        putImageInStorage(storageReference, uri, key);
                                    } else {
                                        Log.w(TAG, "Unable to write message to database.",
                                                databaseError.toException());
                                    }
                                }
                            });
                }
            }
        }
    }

    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key) {
        storageReference.putFile(uri).addOnCompleteListener(GroupChat.this,
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            FriendlyMessage friendlyMessage =
                                    new FriendlyMessage(null, mUsername, mPhotoUrl,
                                            task.getResult().getMetadata().getDownloadUrl()
                                                    .toString());
                            mFirebaseDatabaseReference.child(CONVERSATIONS).child(key)
                                    .setValue(friendlyMessage);
                        } else {
                            Log.w(TAG, "Image upload task was not successful.",
                                    task.getException());
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }
}