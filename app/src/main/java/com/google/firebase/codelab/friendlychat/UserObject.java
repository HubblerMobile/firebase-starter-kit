package com.google.firebase.codelab.friendlychat;

/**
 * Created by hubbler-sudesh on 20/02/18.
 */

public class UserObject {

    private String id;
    private String displayName;
    private String photoUrl;
    private String imageUrl;

    public UserObject(String id, String displayName, String photoUrl, String imageUrl) {
        this.id = id;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.imageUrl = imageUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
