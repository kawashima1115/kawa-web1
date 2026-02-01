package com.example.kawaweb.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.kawaweb.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ユーザー名で検索
    Optional<User> findByUsername(String username);
    
    // ユーザーIDで検索
    Optional<User> findByUserId(String userId);
    
    // メールアドレスで検索
    Optional<User> findByEmail(String email);
    
    // ユーザー名の重複チェック
    boolean existsByUsername(String username);
    
    // ユーザーIDの重複チェック
    boolean existsByUserId(String userId);
    
    // メールアドレスの重複チェック
    boolean existsByEmail(String email);
}