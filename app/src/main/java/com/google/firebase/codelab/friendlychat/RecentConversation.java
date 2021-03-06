package com.google.firebase.codelab.friendlychat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/// ABOUT SCRIPT: This script is responsible for managing your recent conversations. When a User sends a message to you or you send a
//                a message to someone the conversation can be accessed in recent chats.
//                If you are a logged in user. this is the default screen to begin with.

public class RecentConversation extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private DatabaseReference mFirebaseDatabaseReference;
    private DatabaseReference conversationsRef;
    private DatabaseReference recentChatsRef;

    private GoogleApiClient mGoogleApiClient;

    private String sender;
    private String mName;
    private String mEmail;
    private String mPhotoUrl;
    public static final String CONVERSATIONS = "conversations";

    private static final String KEY_TEXT_REPLY = "key_text_reply";
    int mRequestCode = 1000;


    private FirebaseRecyclerAdapter<FriendlyMessage, RecentChatViewHolder> mFirebaseAdapter;

    public static class RecentChatViewHolder extends RecyclerView.ViewHolder {
        TextView LastMessageTextView;
        TextView userDisplayNameTextView;
        CircleImageView userProfileImageView;
        View viewItem;

        public RecentChatViewHolder(View itemView) {
            super(itemView);
            LastMessageTextView = itemView.findViewById(R.id.recentChat_lastMessage);
            userDisplayNameTextView =  itemView.findViewById(R.id.recentChat_senderName);
            userProfileImageView =  itemView.findViewById(R.id.recentChat_profilePic);

            this.viewItem = itemView;
        }
    }

    public static final String RECENT_CHATS = "convesations";

    private SharedPreferences mSharedPreferences;

    private RecyclerView mRecentChatsRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private TextView txt_NoRecentConv;

    FloatingActionButton fab_selectChatUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recent_conversation);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        recentChatsRef = mFirebaseDatabaseReference.child(RECENT_CHATS);
        conversationsRef = mFirebaseDatabaseReference.child(CONVERSATIONS);            //Firebase message branch
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        txt_NoRecentConv = findViewById(R.id.txtv_noRecentConv);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

//        createNotification();
//        getCurrentUser();
        if(mFirebaseUser == null)
        {
            Intent intent = new Intent(RecentConversation.this,SignInActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        else {
            getCurrentUser();
        }

        Toolbar mToolbar = findViewById(R.id.recentConvToolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Recent Chats");
        //        mToolbar.setTitle("Recent Chats");
//        mToolbar.setNavigationIcon(R.drawable.ic_nav_back);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            mToolbar.setElevation(10f);
        }
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mProgressBar = findViewById(R.id.progressBar);
        mRecentChatsRecyclerView = findViewById(R.id.recentChats);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setReverseLayout(true);
        mLinearLayoutManager.setStackFromEnd(true);
        mRecentChatsRecyclerView.setLayoutManager(mLinearLayoutManager);

        fab_selectChatUser = findViewById(R.id.selectChatUser);
/**
 * Floating Action Button For selecting new user to chat if the user is not available in recent conversations.
 */
        fab_selectChatUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RecentConversation.this,SelectUserToChat.class));
            }
        });
//        CheckIfTokenIdExists();

//        final List<FriendlyMessage> recentChatObject = new ArrayList<>();

        SnapshotParser<FriendlyMessage> parser = new SnapshotParser<FriendlyMessage>() {
            @Override
            public FriendlyMessage parseSnapshot(DataSnapshot dataSnapshot) {

                FriendlyMessage msgObj;// = new FriendlyMessage();
                FriendlyMessage grpObj = new FriendlyMessage();
                boolean grpObjFound = false;

//                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

                if(dataSnapshot.getKey().toString().equals("Group"))
                {
                    for(DataSnapshot snapshot:dataSnapshot.getChildren())
                    {
                        grpObj = snapshot.getValue(FriendlyMessage.class);
                        grpObjFound = true;
                        return grpObj;
                    }
                    grpObjFound = false;
                }
                msgObj = dataSnapshot.getValue(FriendlyMessage.class);
                Log.i("Query", "parseSnapshot: "+dataSnapshot.getValue());
                if(!grpObjFound)
                    return msgObj;        // Testing
                else
                    return grpObj;
            }
        };

//        Query keyQuery = mFirebaseDatabaseReference.child("conversations").child("iUlC1L0fyYa5JsoUtYz8bGvMtFk2 chat with ZT0zxHibN2hdT2yRAQ48pIzW23i1").limitToLast(1);      //Todo: try to find how to order by timestamp.
        Query query = mFirebaseDatabaseReference.child("recent_chats").child(sender).orderByChild("timeStamp");
        DatabaseReference convesation_root = mFirebaseDatabaseReference.child("convesations");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChildren())
                {
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    txt_NoRecentConv.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        FirebaseRecyclerOptions<FriendlyMessage> options =
                new FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                        .setQuery(query,parser)
                        .build();

        //FIREBASE RECYLER ADAPTER
        mFirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, RecentChatViewHolder>(options) {
            @Override
            public RecentChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return new RecentChatViewHolder(inflater.inflate(R.layout.recent_chat_list_item, parent, false));
            }

            @Override
            protected void onBindViewHolder(final RecentChatViewHolder viewHolder,
                                            final int position,
                                            FriendlyMessage recentChatObject) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

                if (recentChatObject.getText() != null) {                                   //If status available, set and make it visible
                    viewHolder.LastMessageTextView.setText(recentChatObject.getText());
                    viewHolder.LastMessageTextView.setVisibility(TextView.VISIBLE);
                }

                viewHolder.userDisplayNameTextView.setText(recentChatObject.getName());

                viewHolder.viewItem.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        DatabaseReference firebaseAdapterRef = mFirebaseAdapter.getRef(position);

                        firebaseAdapterRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                String receiver = dataSnapshot.getKey();

                               if(dataSnapshot.hasChild("type"))
                               {
                                   if(dataSnapshot.child("type").getValue().equals("Group"))
                                   {
                                       Intent myIntent = new Intent(RecentConversation.this, GroupChat.class);
                                       myIntent.putExtra("groupName",dataSnapshot.child("name").getValue().toString());
                                       RecentConversation.this.startActivity(myIntent);
                                       return;
                                   }
                               }

                                Intent myIntent = new Intent(RecentConversation.this, OneToOneChat.class);
                                myIntent.putExtra("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                                myIntent.putExtra("receiver",receiver);
                                RecentConversation.this.startActivity(myIntent);
//                                finish();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Toast.makeText(RecentConversation.this, "Event was cancelled", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                if (recentChatObject.getPhotoUrl() == null) {                                 //Default profile pic
                    //Default Profile Icon
                    viewHolder.userProfileImageView.setImageDrawable(ContextCompat.getDrawable(RecentConversation.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(RecentConversation.this)                           // if user provides profile picture
                            .load(recentChatObject.getPhotoUrl())
                            .into(viewHolder.userProfileImageView);
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
                    mRecentChatsRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mRecentChatsRecyclerView.setAdapter(mFirebaseAdapter);
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

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.


        // TODO: Add code to check if user is signed in.
    }

    public void UpdateUserDetails() {

//        getCurrentUser();
        final Map<String,Object> userDetailsWithTokenId = new HashMap<>();

        final DatabaseReference temp_ref = FirebaseDatabase.getInstance().getReference().child("usersList");

        String deviceToken = FirebaseInstanceId.getInstance().getToken();
        userDetailsWithTokenId.put("email",mEmail);
        userDetailsWithTokenId.put("id",sender);
        userDetailsWithTokenId.put("name",mName);
        userDetailsWithTokenId.put("photoUrl",mPhotoUrl);
        userDetailsWithTokenId.put("status","Happy Hubblering");
        userDetailsWithTokenId.put("tokenId",deviceToken);

        temp_ref.child(sender).setValue(userDetailsWithTokenId);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
//                mUsername = "Guest";
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            case R.id.create_grp:
                createGrp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createGrp() {

//        showAlertDialogForGroupName();
        showActivityForGroupWithMultiselect();
    }

    private void showActivityForGroupWithMultiselect()
    {
        Intent myIntent = new Intent(RecentConversation.this, CreateGroup.class);
        RecentConversation.this.startActivity(myIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.main_menu,menu);


        return super.onCreateOptionsMenu(menu);
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d("RecentConversation", "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    public void getCurrentUser() {

            sender = mFirebaseUser.getUid();
            mEmail = mFirebaseUser.getEmail();
            mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            mName = mFirebaseUser.getDisplayName();
            UpdateUserDetails();
    }
}
