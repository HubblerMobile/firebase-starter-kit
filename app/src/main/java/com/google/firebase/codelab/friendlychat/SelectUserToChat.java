package com.google.firebase.codelab.friendlychat;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class SelectUserToChat extends AppCompatActivity {

    private FirebaseRecyclerAdapter<UserObject, MessageViewHolder> mFirebaseAdapter;
    private RecyclerView usersRecylerView;
    private DatabaseReference usersListReference;
    private DatabaseReference mFirebaseDatabaseReference;

    public static final String USERS_LIST = "usersList";

    // VIEWHOLDER FOR FIREBASE USER LIST
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView user_displayName;
        ImageView messageImageView;
        CircleImageView profilePicture;

        public MessageViewHolder(View v) {
            super(v);
            user_displayName = (TextView) v.findViewById(R.id.user_displayName);
            messageImageView = (ImageView) v.findViewById(R.id.messageImageView);
            profilePicture = (CircleImageView) v.findViewById(R.id.profilePicture);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user_to_chat);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        usersListReference = mFirebaseDatabaseReference.child(USERS_LIST);            //Firebase message branch
        usersListReference.keepSynced(true);

        usersRecylerView = findViewById(R.id.messageRecyclerView);
        SnapshotParser<UserObject> parser = new SnapshotParser<UserObject>() {
            @Override
            public UserObject parseSnapshot(DataSnapshot dataSnapshot) {
                UserObject friendlyMessage = dataSnapshot.getValue(UserObject.class);
                if (friendlyMessage != null) {
                    friendlyMessage.setId(dataSnapshot.getKey());
                }
                return friendlyMessage;
            }
        };

        FirebaseRecyclerOptions<UserObject> options =
                new FirebaseRecyclerOptions.Builder<UserObject>()
                        .setQuery(usersListReference, parser)
                        .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<UserObject, MessageViewHolder>(options) {
            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.user_list_item, viewGroup, false));
            }


            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder,
                                            int position,
                                            UserObject userObject) {
                if (userObject.getDisplayName() != null) {
                    viewHolder.user_displayName.setText(userObject.getDisplayName());
                    viewHolder.user_displayName.setVisibility(TextView.VISIBLE);
                    viewHolder.messageImageView.setVisibility(ImageView.GONE);
                } else {
                    String imageUrl = userObject.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Glide.with(viewHolder.messageImageView.getContext())
                                                    .load(downloadUrl)
                                                    .into(viewHolder.messageImageView);
                                        } else {
                                            Log.w("selectUserMsg", "Getting download url was not successful.",
                                                    task.getException());
                                        }
                                    }
                                });
                    } else {
                        Glide.with(viewHolder.messageImageView.getContext())
                                .load(userObject.getImageUrl())
                                .into(viewHolder.messageImageView);
                    }
                    viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                    viewHolder.user_displayName.setVisibility(TextView.GONE);
                }


                viewHolder.user_displayName.setText(userObject.getDisplayName());
                if (userObject.getPhotoUrl() == null) {
                    viewHolder.profilePicture.setImageDrawable(ContextCompat.getDrawable(SelectUserToChat.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(SelectUserToChat.this)
                            .load(userObject.getPhotoUrl())
                            .into(viewHolder.profilePicture);
                }

            }
        };

        usersRecylerView.setAdapter(mFirebaseAdapter);
    }
}
