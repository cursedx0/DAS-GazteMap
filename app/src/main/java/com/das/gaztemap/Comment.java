package com.das.gaztemap;

public class Comment {
    private String commentId;
    private String userId;
    private String userName;
    private String content;
    private long timestamp;

    public Comment(String commentId, String userId, String userName, String content, long timestamp) {
        this.commentId = commentId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getCommentId() { return commentId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}