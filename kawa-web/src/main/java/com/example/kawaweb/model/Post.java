package com.example.kawaweb.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
    
    // 親投稿との関連（返信の場合）
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Post parent;
    
    // 返信リスト（この投稿に対する返信）
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Post> replies = new ArrayList<>();
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // デフォルトコンストラクタ
    public Post() {}
    
    // 通常の投稿用コンストラクタ
    public Post(String content, User user) {
        this.content = content;
        this.user = user;
    }
    
    // 返信用コンストラクタ
    public Post(String content, User user, Post parent) {
        this.content = content;
        this.user = user;
        this.parent = parent;
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
    
    public Post getParent() {
        return parent;
    }
    
    public void setParent(Post parent) {
        this.parent = parent;
    }
    
    public List<Post> getReplies() {
        return replies;
    }
    
    public void setReplies(List<Post> replies) {
        this.replies = replies;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    // ユーザー名を取得する便利メソッド
    public String getUsername() {
        return user != null ? user.getUsername() : "匿名";
    }
    
    // 返信かどうかを判定
    public boolean isReply() {
        return parent != null;
    }
    
    // 返信数を取得
    public int getReplyCount() {
        return replies != null ? replies.size() : 0;
    }
    
    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", username='" + getUsername() + '\'' +
                ", parentId=" + (parent != null ? parent.getId() : null) +
                ", replyCount=" + getReplyCount() +
                ", createdAt=" + createdAt +
                '}';
    }
}