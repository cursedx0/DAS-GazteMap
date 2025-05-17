package com.das.gaztemap;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class LeaderboardUser implements Parcelable {
    private int rank;
    private boolean isCurrentUser;

    private String name;
    private int points;
    private String profileImageUrl;
    private boolean isOnline;

    public LeaderboardUser(int rank, String name, int points, String profileImageUrl) {
        this.rank = rank;
        this.name = name;
        this.points = points;
        this.profileImageUrl = profileImageUrl;
        this.isOnline = false;
    }

    public LeaderboardUser(int rank, String name, int points, String profileImageUrl, boolean isOnline) {
        this.rank = rank;
        this.name = name;
        this.points = points;
        this.profileImageUrl = profileImageUrl;
        this.isOnline = isOnline;
    }

    protected LeaderboardUser(Parcel in) {
        rank = in.readInt();
        isCurrentUser = in.readByte() != 0;
        name = in.readString();
        points = in.readInt();
        profileImageUrl = in.readString();
        isOnline = in.readByte() != 0;
    }

    public static final Creator<LeaderboardUser> CREATOR = new Creator<LeaderboardUser>() {
        @Override
        public LeaderboardUser createFromParcel(Parcel in) {
            return new LeaderboardUser(in);
        }

        @Override
        public LeaderboardUser[] newArray(int size) {
            return new LeaderboardUser[size];
        }
    };

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;

    }

    public boolean isCurrentUser() {
        return isCurrentUser;
    }

    public void setCurrentUser(boolean currentUser) {
        isCurrentUser = currentUser;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(rank);
        dest.writeString(name);
        dest.writeInt(points);
        dest.writeString(profileImageUrl);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dest.writeBoolean(isOnline);
        }
    }
}