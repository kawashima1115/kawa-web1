package com.example.kawaweb.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.kawaweb.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ユーザー名で検索
    Optional<User> findByUsername(String username);
    
    // ユーザー名の存在確認
    boolean existsByUsername(String username);
    
    // メールアドレスで検索
    Optional<User> findByEmail(String email);
    
    // メールアドレスの存在確認
    boolean existsByEmail(String email);
}