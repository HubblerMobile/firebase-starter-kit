package com.google.firebase.codelab.friendlychat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateGroup extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,View.OnLongClickListener{

    private DatabaseReference mFirebaseDatabaseReference;
    private DatabaseReference listOfUsersRef;
    private DatabaseReference groupChatStatus;
    private DatabaseReference conversationsRef;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseRecyclerAdapter<UserObject, CreateGroup.UserViewHolder> mFirebaseAdapter;
    private Map<String,Object> map_groupMembers = new HashMap<>();

    private String mUid;
    private UserObject grpCreator = new UserObject();
    private TextInputLayout textInp_groupName;
    FloatingActionButton fab_createGroup;
    ActionMode actionMode;
    Toolbar toolbar;



    @Override
    public boolean onLongClick(View view) {

        toolbar.getMenu().clear();
        return false;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView StatusTextView;
        TextView userDisplayNameTextView;
        CircleImageView userProfileImageView;
//        int row_index = -1;
        CreateGroup createGroup;
        View viewItem;

        public UserViewHolder(View itemView) {
            super(itemView);
            StatusTextView = itemView.findViewById(R.id.text);
            userDisplayNameTextView =  itemView.findViewById(R.id.userName);
            userProfileImageView =  itemView.findViewById(R.id.userProfilePic);
            this.createGroup = createGroup;
            itemView.setOnLongClickListener(createGroup);
            this.viewItem = itemView;
        }
    }

    private static final String TAG = "MainActivity";
    public static final String LIST_OF_USERS = "usersList";

    private SharedPreferences mSharedPreferences;

    private RecyclerView mSelectMemberRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        listOfUsersRef = mFirebaseDatabaseReference.child(LIST_OF_USERS);            //Firebase message branch
        conversationsRef = mFirebaseDatabaseReference.child("conversations");
        mFirebaseAuth = FirebaseAuth.getInstance();
        mUid = mFirebaseAuth.getCurrentUser().getUid();
//        listOfUsersRef.keepSynced(true);
        textInp_groupName = findViewById(R.id.ip_create_grp);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        final TextInputLayout input = findViewById(R.id.ip_create_grp);
        fab_createGroup = findViewById(R.id.fab_createGrp);

        mProgressBar = findViewById(R.id.progressBar);
        mSelectMemberRecyclerView = findViewById(R.id.recentChatsRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mSelectMemberRecyclerView.setLayoutManager(mLinearLayoutManager);

        Toolbar mToolbar = findViewById(R.id.createGrpToolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Create Group");
//        getSupportActionBar().setSubtitle("Add Participants");
//        mToolbar.setTitle("Create Group");
//        mToolbar.setNavigationIcon(R.drawable.ic_nav_back);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            mToolbar.setElevation(10f);
        }

        fab_createGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String groupName = textInp_groupName.getEditText().getText().toString();
                setGrouUsers(map_groupMembers,groupName);
                AddGroupToDb(groupName);

            }
        });

        SnapshotParser<UserObject> parser = new SnapshotParser<UserObject>() {
            @Override
            public UserObject parseSnapshot(DataSnapshot dataSnapshot) {

                UserObject userObject = dataSnapshot.getValue(UserObject.class);

                return userObject;
            }
        };

//        mSelectMemberRecyclerView

// Remove your name from the chat user list so that only other users are visible.
        Query query = listOfUsersRef.orderByKey();

        Log.i(TAG, "onCreate: Parsed Query "+query);
//        Log.i(TAG, "onCreate: "+parse);

        FirebaseRecyclerOptions<UserObject> options =
                new FirebaseRecyclerOptions.Builder<UserObject>()
                        .setQuery(query, parser)
                        .build();

        //FIREBASE RECYLER ADAPTER
        mFirebaseAdapter = new FirebaseRecyclerAdapter<UserObject, UserViewHolder>(options) {
            @Override
            public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

                Log.i(TAG, "onCreateViewHolder: "+viewType);
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return new UserViewHolder(inflater.inflate(R.layout.select_user_item, parent, false));
            }


            @Override
            protected void onBindViewHolder(final UserViewHolder viewHolder,
                                            final int position,
                                            UserObject userObject) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

                //Todo: Temporary hack. not try to find how to remove it from the the list itself.
                if(userObject.getId().equals(mUid))
                {
                    viewHolder.viewItem.setVisibility(View.GONE);
                }
                else {
                    viewHolder.viewItem.setVisibility(View.VISIBLE);
                }
                // QuicHack ends here
                if (userObject.getStatus() != null) {                                   //If status available, set and make it visible
                    viewHolder.StatusTextView.setText(userObject.getStatus());
                    viewHolder.StatusTextView.setVisibility(TextView.VISIBLE);
                }

                viewHolder.userDisplayNameTextView.setText(userObject.getName());

                viewHolder.viewItem.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                         getSupportActionBar().setSubtitle("select users");
                         getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                         PrepareSelection(view,viewHolder.getAdapterPosition());
//                        notifyDataSetChanged();
                    }
                });


                if (userObject.getPhotoUrl() == null) {                                 //Default profile pic
                    //Default Profile Icon
                    viewHolder.userProfileImageView.setImageDrawable(ContextCompat.getDrawable(CreateGroup.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(CreateGroup.this)                           // if user provides profile picture
                            .load(userObject.getPhotoUrl())
                            .into(viewHolder.userProfileImageView);
                }

            }

            public void PrepareSelection(View view, int position)
            {
                String viewName = getResources().getResourceEntryName(R.id.userName);
               UserObject userObject = mFirebaseAdapter.getItem(position);
                Log.i(TAG, "PrepareSelection: "+userObject);
                ManageGroupUserList(view,userObject);
                Toast.makeText(CreateGroup.this, "Selected: "+userObject.getName(), Toast.LENGTH_SHORT).show();
            }

            public void ManageGroupUserList(View view,UserObject userObject)
            {
                if(!map_groupMembers.containsKey(userObject.getId()))
                {
                    map_groupMembers.put(userObject.getId(),userObject);
                    view.setBackgroundColor(Color.parseColor("#BDBDBD"));
                    Toast.makeText(CreateGroup.this, "New Selection", Toast.LENGTH_SHORT).show();
                }
                else {
                    map_groupMembers.remove(userObject.getId());
                    view.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    Toast.makeText(CreateGroup.this, "Old selection", Toast.LENGTH_SHORT).show();
                }
                UpdateToolbarSubtitle(map_groupMembers.size());
            }

            public void UpdateToolbarSubtitle(int noOfUsers)
            {
                if(noOfUsers==0)
                    getSupportActionBar().setSubtitle("Select users");
                else {
                    getSupportActionBar().setSubtitle(noOfUsers+ " users selected");
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
                    mSelectMemberRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mSelectMemberRecyclerView.setAdapter(mFirebaseAdapter);
    }



//    private android.support.v7.view.ActionMode mActionModeCallback = new ActionMode.Callback() {
//
//        @Override
//        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
//
//            actionMode.getMenuInflater().inflate(R.menu.menu_action_mode,menu);
//            return true;
//        }
//
//        @Override
//        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
//            return false;
//        }
//
//        @Override
//        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
//            return false;
//        }
//
//        @Override
//        public void onDestroyActionMode(ActionMode actionMode) {
//
//        }
//    };

//    private ActionMode.Callback CreateGroupCallback = new ActionMode.Callback() {
//
//        @Override
//        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
//
//            actionMode.getMenuInflater().inflate(R.menu.menu_action_mode,menu);
//            actionMode.setTitle("DFS");
//            actionMode.setSubtitle("Participants selected");
//
//            return true;
//        }
//
//        @Override
//        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
//
////            actionMode.setTitle("Create Group");
////            actionMode.setSubtitle("0 of 10 selected") ;
//            return false;
//        }
//
//        @Override
//        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
//
//            Toast.makeText(CreateGroup.this, "Trigger in Contextual Menu", Toast.LENGTH_SHORT).show();
//            return false;
//        }
//
//        @Override
//        public void onDestroyActionMode(ActionMode actionMode) {
//            actionMode = null;
//        }
//    };

    private void setGrouUsers(final Map<String,Object> grpUsers,String grpName)
    {
        Log.i(TAG, "setGrouUsers: "+grpUsers);

        mFirebaseDatabaseReference.child("GroupUserDetails").child(grpName).setValue(grpUsers);
        AddAdmintoNotificationDetails(grpName);
    }
    private void AddAdmintoNotificationDetails(final String grpName)
    {
        final Map<String,String> admin = new HashMap<>();

        mFirebaseDatabaseReference.child("usersList").child(mUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                grpCreator = dataSnapshot.getValue(UserObject.class);
                admin.put("id",dataSnapshot.child("id").getValue().toString());
                admin.put("name",dataSnapshot.child("name").getValue().toString());
                admin.put("photoUrl",dataSnapshot.child("photoUrl").getValue().toString());
                admin.put("status","admin");

                mFirebaseDatabaseReference.child("GroupUserDetails").child(grpName).child(mUid).setValue(admin);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }
    private void AddGroupMembersToGroupSttus(String grpName) {

//        groupChatStatus.child(grpName).child()
        groupChatStatus.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    // Called by createGrp function
    private void AddGroupToDb(final String grpName) {

        conversationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(! dataSnapshot.hasChild(grpName))
                {
                    conversationsRef.child(grpName).setValue("No conversations Yet");
                    Intent myIntent = new Intent(CreateGroup.this, GroupChat.class);
//                    myIntent.putExtra("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    myIntent.putExtra("groupName",grpName);
                    CreateGroup.this.startActivity(myIntent);
                }
                else {
                    Toast.makeText(CreateGroup.this, "Group already exist!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }
}
