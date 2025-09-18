package com.example.kawaweb.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;

@Entity
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String content;
    
    // ユーザーとの関連（多対一）
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // デフォルトコンストラクタ
    public Post() {}
    
    // コンストラクタ
    public Post(String content, User user) {
        this.content = content;
        this.user = user;
    }
    
    // getter/setter
    public Long getId() {
        return id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    // ユーザー名を取得する便利メソッド（HTMLテンプレートで使用）
    public String getUsername() {
        return user != null ? user.getUsername() : "匿名";
    }
    
    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", username='" + getUsername() + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}