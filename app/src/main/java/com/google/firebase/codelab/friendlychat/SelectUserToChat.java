package com.google.firebase.codelab.friendlychat;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class SelectUserToChat extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private DatabaseReference mFirebaseDatabaseReference;
    private DatabaseReference usersRef;

    private FirebaseRecyclerAdapter<UserObject, UserViewHolder> mFirebaseAdapter;


    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView StatusTextView;
        TextView userDisplayNameTextView;
        CircleImageView userProfileImageView;
        View viewItem;

        public UserViewHolder(View itemView) {
            super(itemView);
            StatusTextView = itemView.findViewById(R.id.status);
            userDisplayNameTextView =  itemView.findViewById(R.id.userName);
            userProfileImageView =  itemView.findViewById(R.id.userProfilePic);

            this.viewItem = itemView;
        }

        public void bindToUserViewHolder(UserObject userObject, View.OnClickListener clickListener) {

            // viewItem.setOnClickListener(clickListener);
        }
    }

    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "usersList";

    private SharedPreferences mSharedPreferences;

    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        usersRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD);            //Firebase message branch
//        usersRef.keepSynced(true);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user_to_chat);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        mProgressBar = findViewById(R.id.progressBar);
        mMessageRecyclerView = findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        SnapshotParser<UserObject> parser = new SnapshotParser<UserObject>() {
            @Override
            public UserObject parseSnapshot(DataSnapshot dataSnapshot) {
                UserObject userObject = dataSnapshot.getValue(UserObject.class);
                if (userObject != null) {
                    userObject.setId(dataSnapshot.getKey());
                }
                return userObject;
            }
        };

        FirebaseRecyclerOptions<UserObject> options =
                new FirebaseRecyclerOptions.Builder<UserObject>()
                        .setQuery(usersRef, parser)
                        .build();

//FIREBASE RECYLER ADAPTER
        mFirebaseAdapter = new FirebaseRecyclerAdapter<UserObject, UserViewHolder>(options) {
            @Override
            public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return new UserViewHolder(inflater.inflate(R.layout.user_list_item, parent, false));
            }


            @Override
            protected void onBindViewHolder(final UserViewHolder viewHolder,
                                            final int position,
                                            UserObject userObject) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

                if (userObject.getStatus() != null) {                                   //If status available, set and make it visible
                    viewHolder.StatusTextView.setText(userObject.getStatus());
                    viewHolder.StatusTextView.setVisibility(TextView.VISIBLE);
                }

                viewHolder.userDisplayNameTextView.setText(userObject.getName());

                viewHolder.viewItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DatabaseReference firebaseAdapterRef = mFirebaseAdapter.getRef(position);

                        firebaseAdapterRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                String name = dataSnapshot.child("name").getValue().toString();
                                Toast.makeText(SelectUserToChat.this, "Name is "+name, Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Toast.makeText(SelectUserToChat.this, "Event was cancelledd", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                viewHolder.bindToUserViewHolder(userObject, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
                if (userObject.getPhotoUrl() == null) {                                 //Default profile pic
                    //Default Profile Icon
                    viewHolder.userProfileImageView.setImageDrawable(ContextCompat.getDrawable(SelectUserToChat.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(SelectUserToChat.this)                           // if user provides profile picture
                            .load(userObject.getPhotoUrl())
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
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }
}
