package com.example.kawaweb.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 投稿内容（HTMLを含むリッチテキスト）- TEXTタイプに変更して長文対応
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 投稿日時
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 投稿者（Userエンティティとの多対一の関係）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 親投稿（返信の場合のみ設定される）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Post parent;

    // 子投稿（この投稿への返信）
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> replies = new ArrayList<>();

    // 保存前に自動で現在日時をセット
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

    // 返信用コンストラクタ
    public Post(String content, User user, Post parent) {
        this.content = content;
        this.user = user;
        this.parent = parent;
    }

    // Getter/Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    // Thymeleafテンプレートで使用するヘルパーメソッド
    @Transient
    public String getUsername() {
        return user != null ? user.getUsername() : "Unknown";
    }

    @Transient
    public int getReplyCount() {
        return replies != null ? replies.size() : 0;
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                ", userId=" + (user != null ? user.getId() : null) +
                ", parentId=" + (parent != null ? parent.getId() : null) +
                '}';
    }
}