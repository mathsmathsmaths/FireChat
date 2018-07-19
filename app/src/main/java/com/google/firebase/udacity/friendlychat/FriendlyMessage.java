package com.google.firebase.udacity.friendlychat;

public class FriendlyMessage {

    private String msg;
    private String name;
    private String photoUrl;
    private long unixTimestamp;

    public FriendlyMessage() {
    }

    public FriendlyMessage(String text, String name, long unixTimestamp, String photoUrl) {
        this.unixTimestamp = unixTimestamp;
        if (!text.equals("")) {
            this.msg = text.trim();
        } else {
            this.msg = text;
        }
        this.name = name.trim();
        this.photoUrl = photoUrl;
    }

    public String getText() {
        return msg;
    }

    public void setText(String text) {
        if (!text.trim().equals("")) {
            this.msg = text.trim();
        }
    }

    public void getUnixTimestamp(long unixTimestamp) {
        this.unixTimestamp = unixTimestamp;
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
