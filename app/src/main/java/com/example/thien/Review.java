package com.example.thien;

// 1 đánh giá + bình luận của người nghe cho 1 món
public class Review {
    public final String account;
    public final String author;
    public final int rating;      // 1..5 sao
    public final String content;
    public final long createdAt;

    public Review(String account, String author, int rating, String content, long createdAt) {
        this.account = account;
        this.author = author;
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
    }
}
