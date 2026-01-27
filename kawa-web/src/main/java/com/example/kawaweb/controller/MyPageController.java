package com.example.kawaweb.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.kawaweb.model.Post;
import com.example.kawaweb.model.User;
import com.example.kawaweb.repository.PostRepository;
import com.example.kawaweb.repository.UserRepository;

@Controller
public class MyPageController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    // マイページ表示
    @GetMapping("/mypage")
    public String myPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        System.out.println("=== マイページアクセス ===");
        
        // セッションからログインユーザーを取得
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        // ログインチェック
        if (loggedInUser == null) {
            System.out.println("ログインしていません");
            redirectAttributes.addFlashAttribute("error", "ログインが必要です");
            return "redirect:/login";
        }
        
        System.out.println("ログインユーザー: " + loggedInUser.getUsername());
        
        // データベースから最新のユーザー情報を取得
        Optional<User> userOpt = userRepository.findById(loggedInUser.getId());
        if (userOpt.isEmpty()) {
            System.out.println("ユーザーが見つかりません");
            redirectAttributes.addFlashAttribute("error", "ユーザー情報が見つかりません");
            return "redirect:/";
        }
        
        User user = userOpt.get();
        
        // ユーザーの全投稿を取得
        List<Post> allPosts = new ArrayList<>();
        List<Post> userPosts = new ArrayList<>();  // 親投稿のみ
        List<Post> userReplies = new ArrayList<>(); // 返信のみ
        
        try {
            allPosts = postRepository.findByUserOrderByCreatedAtDesc(user);
            
            System.out.println("=== 投稿データ詳細 ===");
            System.out.println("総投稿数: " + allPosts.size());
            
            // 各投稿の詳細をログ出力
            for (Post post : allPosts) {
                System.out.println("投稿ID: " + post.getId() + 
                                 ", parent: " + (post.getParent() != null ? post.getParent().getId() : "null") +
                                 ", isReply: " + (post.getParent() != null));
            }
            
            // 投稿と返信を分離
            userPosts = allPosts.stream()
                    .filter(post -> post.getParent() == null)  // 親投稿のみ
                    .collect(Collectors.toList());
            
            userReplies = allPosts.stream()
                    .filter(post -> post.getParent() != null)   // 返信のみ
                    .collect(Collectors.toList());
            
            System.out.println("親投稿数: " + userPosts.size());
            System.out.println("返信数: " + userReplies.size());
            System.out.println("===================");
        } catch (Exception e) {
            System.out.println("投稿取得エラー: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 利用日数を計算
        long daysSinceRegistration = java.time.temporal.ChronoUnit.DAYS.between(
            user.getCreatedAt().toLocalDate(),
            java.time.LocalDate.now()
        );
        
        // モデルに追加
        model.addAttribute("user", user);
        model.addAttribute("userPosts", userPosts);           // 親投稿のみ
        model.addAttribute("userReplies", userReplies);       // 返信のみ
        model.addAttribute("postCount", userPosts.size());    // 親投稿の数
        model.addAttribute("replyCount", userReplies.size()); // 返信の数
        model.addAttribute("totalCount", allPosts.size());    // 全投稿数
        model.addAttribute("daysSinceRegistration", daysSinceRegistration);
        model.addAttribute("loggedInUser", user);
        model.addAttribute("isLoggedIn", true);
        
        System.out.println("=== マイページ表示 ===");
        return "mypage";
    }
    
    // プロフィール編集画面
    @GetMapping("/mypage/edit")
    public String editProfile(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "ログインが必要です");
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userRepository.findById(loggedInUser.getId());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ユーザー情報が見つかりません");
            return "redirect:/";
        }
        
        model.addAttribute("user", userOpt.get());
        model.addAttribute("loggedInUser", userOpt.get());
        model.addAttribute("isLoggedIn", true);
        
        return "mypage-edit";
    }
    
    // プロフィール更新
    @PostMapping("/mypage/update")
    public String updateProfile(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "ログインが必要です");
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userRepository.findById(loggedInUser.getId());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ユーザー情報が見つかりません");
            return "redirect:/";
        }
        
        User user = userOpt.get();
        boolean updated = false;
        
        // メールアドレス更新
        if (email != null && !email.trim().isEmpty()) {
            Optional<User> existingUser = userRepository.findByEmail(email.trim());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "そのメールアドレスは既に使用されています");
                return "redirect:/mypage/edit";
            }
            user.setEmail(email.trim());
            updated = true;
        } else {
            user.setEmail(null);
            updated = true;
        }
        
        // パスワード更新
        if (currentPassword != null && !currentPassword.isEmpty() && 
            newPassword != null && !newPassword.isEmpty()) {
            
            if (!user.getPassword().equals(currentPassword)) {
                redirectAttributes.addFlashAttribute("error", "現在のパスワードが間違っています");
                return "redirect:/mypage/edit";
            }
            
            if (newPassword.length() < 8) {
                redirectAttributes.addFlashAttribute("error", "新しいパスワードは8文字以上で設定してください");
                return "redirect:/mypage/edit";
            }
            
            user.setPassword(newPassword);
            updated = true;
        }
        
        if (updated) {
            userRepository.save(user);
            session.setAttribute("loggedInUser", user);
            redirectAttributes.addFlashAttribute("success", "プロフィールを更新しました");
        } else {
            redirectAttributes.addFlashAttribute("info", "変更内容がありませんでした");
        }
        
        return "redirect:/mypage";
    }
    
    // アカウント削除確認
    @GetMapping("/mypage/delete-confirm")
    public String deleteConfirm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "ログインが必要です");
            return "redirect:/login";
        }
        
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("isLoggedIn", true);
        
        return "mypage-delete";
    }
    
    // アカウント削除
    @PostMapping("/mypage/delete")
    public String deleteAccount(
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "ログインが必要です");
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userRepository.findById(loggedInUser.getId());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ユーザー情報が見つかりません");
            return "redirect:/";
        }
        
        User user = userOpt.get();
        
        if (!user.getPassword().equals(password)) {
            redirectAttributes.addFlashAttribute("error", "パスワードが間違っています");
            return "redirect:/mypage/delete-confirm";
        }
        
        userRepository.delete(user);
        session.invalidate();
        
        redirectAttributes.addFlashAttribute("success", "アカウントを削除しました");
        return "redirect:/";
    }
}