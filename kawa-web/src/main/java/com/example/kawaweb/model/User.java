package com.example.kawaweb.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")  // DBテーブル名を「app_users」に指定
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    // 主キー（自動採番）
    private Long id;
    
    @Column(unique = true, nullable = false)
    // ユーザー名（重複不可、null不可）
    private String username;
    
    @Column(nullable = false)
    // パスワード（必須）
    private String password;
    
    // メールアドレス（必須ではない）
    private String email;
    
    // 登録日時
    private LocalDateTime createdAt;
    
    // ユーザーが作成した投稿（Post エンティティ）との 1対多 関係
    // mappedBy="user" → Post 側にある「user」フィールドが外部キーを持つ
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Post> posts;
    
    // データ保存前に自動で現在日時をセット
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // デフォルトコンストラクタ（JPAが内部的に利用）
    public User() {}
    
    // 任意の初期値を入れるためのコンストラクタ
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    
    // getter/setter（プロパティアクセス用）
    public Long getId() {
        return id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public List<Post> getPosts() {
        return posts;
    }
    
    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        // ログ出力やデバッグ用にユーザー情報を文字列化
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}