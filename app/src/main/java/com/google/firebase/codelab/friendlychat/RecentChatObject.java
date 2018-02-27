package com.google.firebase.codelab.friendlychat;

/**
 * Created by hubbler-sudesh on 23/02/18.
 */

public class RecentChatObject {

    private String id;
    private String text;
    private String name;
    private String photoUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getText() {
        return text;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
