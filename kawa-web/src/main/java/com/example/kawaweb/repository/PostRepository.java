package com.example.kawaweb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.kawaweb.model.Post;
import com.example.kawaweb.model.User;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    // 親投稿のみを作成日時の降順で取得（返信は除外）
    List<Post> findByParentIsNullOrderByCreatedAtDesc();
    
    // 全投稿を作成日時の降順で取得
    List<Post> findAllByOrderByCreatedAtDesc();
    
    // 特定ユーザーの投稿を作成日時の降順で取得
    List<Post> findByUserOrderByCreatedAtDesc(User user);
    
    // 特定ユーザーの投稿数をカウント
    long countByUser(User user);
    
    // 特定の投稿への返信を作成日時の昇順で取得
    List<Post> findByParentOrderByCreatedAtAsc(Post parent);
    
    // 特定の投稿への返信数をカウント
    long countByParent(Post parent);
}