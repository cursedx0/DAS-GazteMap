package com.das.gaztemap;

import android.os.Parcel;
import android.os.Parcelable;

public class Comment implements Parcelable {
    private String id;
    private String userId;
    private String userName;
    private String content;
    private long timestamp;
    private String profileImageUrl;

    public Comment(String id, String userId, String userName, String content, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.timestamp = timestamp;
    }

    protected Comment(Parcel in) {
        id = in.readString();
        userId = in.readString();
        userName = in.readString();
        content = in.readString();
        timestamp = in.readLong();
        profileImageUrl = in.readString();
    }

    public static final Creator<Comment> CREATOR = new Creator<Comment>() {
        @Override
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userId);
        dest.writeString(userName);
        dest.writeString(content);
        dest.writeLong(timestamp);
        dest.writeString(profileImageUrl);
    }
}