package com.google.firebase.udacity.friendlychat;

public class FriendlyMessage {

    private String text;
    private String name;
    private String photoUrl;

    public FriendlyMessage() {
    }

    public FriendlyMessage(String text, String name, String photoUrl) {
        if (!text.equals("")) {
            this.text = text.trim();
        } else {
            this.text = text;
        }
        this.name = name.trim();
        this.photoUrl = photoUrl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (!text.trim().equals("")) {
            this.text = text.trim();
        }
    }

    public String getName() {
        return name.trim() + "";
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
