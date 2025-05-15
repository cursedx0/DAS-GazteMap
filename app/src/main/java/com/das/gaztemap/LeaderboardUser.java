package com.das.gaztemap;

public class LeaderboardUser {
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
}