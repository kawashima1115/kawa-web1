package com.example.kawaweb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.kawaweb.model.Post;
import com.example.kawaweb.model.User;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    // 全投稿を作成日時の降順で取得
    List<Post> findAllByOrderByCreatedAtDesc();
    
    // 特定ユーザーの投稿を作成日時の降順で取得（これを追加）
    List<Post> findByUserOrderByCreatedAtDesc(User user);
    
    // 特定ユーザーの投稿数をカウント（これも追加）
    long countByUser(User user);
}