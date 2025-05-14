package com.das.gaztemap;

public class LeaderboardUser {
    private int rank;
    private String username;
    private int points;
    private String profileImage;

    public LeaderboardUser(int rank, String username, int points, String profileImage) {
        this.rank = rank;
        this.username = username;
        this.points = points;
        this.profileImage = profileImage;
    }

    public int getRank() {
        return rank;
    }

    public String getUsername() {
        return username;
    }

    public int getPoints() {
        return points;
    }

    public String getProfileImage() {
        return profileImage;
    }
}