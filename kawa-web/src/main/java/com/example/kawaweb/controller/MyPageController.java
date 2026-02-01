package com.example.kawaweb.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
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
    
    // アップロードされた画像を保存するディレクトリ
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/icons/";
    
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
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) MultipartFile iconFile,
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
        
        // アイコン画像の処理
        if (iconFile != null && !iconFile.isEmpty()) {
            try {
                // アップロードディレクトリを作成
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                // ファイル名をユニークにする
                String originalFilename = iconFile.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String filename = UUID.randomUUID().toString() + extension;
                
                // ファイルを保存
                Path filePath = uploadPath.resolve(filename);
                Files.write(filePath, iconFile.getBytes());
                
                // 古いアイコンファイルを削除（デフォルトアイコンでない場合）
                if (user.getIconPath() != null && !user.getIconPath().isEmpty()) {
                    try {
                        Path oldFile = Paths.get(UPLOAD_DIR + user.getIconPath());
                        Files.deleteIfExists(oldFile);
                    } catch (IOException e) {
                        System.out.println("古いアイコンの削除に失敗: " + e.getMessage());
                    }
                }
                
                // データベースに保存するのはファイル名のみ
                user.setIconPath(filename);
                updated = true;
                
                System.out.println("アイコンアップロード成功: " + filename);
            } catch (IOException e) {
                System.out.println("アイコンアップロードエラー: " + e.getMessage());
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "アイコンのアップロードに失敗しました");
                return "redirect:/mypage/edit";
            }
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
    
    // アイコン削除
    @PostMapping("/mypage/delete-icon")
    public String deleteIcon(HttpSession session, RedirectAttributes redirectAttributes) {
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
        
        if (user.getIconPath() != null && !user.getIconPath().isEmpty()) {
            try {
                // ファイルを削除
                Path filePath = Paths.get(UPLOAD_DIR + user.getIconPath());
                Files.deleteIfExists(filePath);
                
                // データベースからも削除
                user.setIconPath(null);
                userRepository.save(user);
                session.setAttribute("loggedInUser", user);
                
                redirectAttributes.addFlashAttribute("success", "アイコンを削除しました");
            } catch (IOException e) {
                System.out.println("アイコン削除エラー: " + e.getMessage());
                redirectAttributes.addFlashAttribute("error", "アイコンの削除に失敗しました");
            }
        } else {
            redirectAttributes.addFlashAttribute("info", "削除するアイコンがありません");
        }
        
        return "redirect:/mypage/edit";
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
        
        // アイコンファイルを削除
        if (user.getIconPath() != null && !user.getIconPath().isEmpty()) {
            try {
                Path filePath = Paths.get(UPLOAD_DIR + user.getIconPath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.out.println("アイコン削除エラー: " + e.getMessage());
            }
        }
        
        userRepository.delete(user);
        session.invalidate();
        
        redirectAttributes.addFlashAttribute("success", "アカウントを削除しました");
        return "redirect:/";
    }
    
    // 投稿を全て削除
    @PostMapping("/mypage/delete-all-posts")
    public String deleteAllPosts(HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "ログインが必要です");
            return "redirect:/login";
        }
        
        try {
            // 親投稿のみを取得して削除
            List<Post> posts = postRepository.findByUserOrderByCreatedAtDesc(loggedInUser);
            List<Post> parentPosts = posts.stream()
                    .filter(post -> post.getParent() == null)
                    .collect(Collectors.toList());
            
            postRepository.deleteAll(parentPosts);
            redirectAttributes.addFlashAttribute("success", "全ての投稿を削除しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "削除中にエラーが発生しました");
        }
        
        return "redirect:/mypage";
    }
    
    // 返信を全て削除
    @PostMapping("/mypage/delete-all-replies")
    public String deleteAllReplies(HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "ログインが必要です");
            return "redirect:/login";
        }
        
        try {
            // 返信のみを取得して削除
            List<Post> posts = postRepository.findByUserOrderByCreatedAtDesc(loggedInUser);
            List<Post> replies = posts.stream()
                    .filter(post -> post.getParent() != null)
                    .collect(Collectors.toList());
            
            postRepository.deleteAll(replies);
            redirectAttributes.addFlashAttribute("success", "全ての返信を削除しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "削除中にエラーが発生しました");
        }
        
        return "redirect:/mypage";
    }
    
    // 投稿と返信を全て削除
    @PostMapping("/mypage/delete-all-content")
    public String deleteAllContent(HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "ログインが必要です");
            return "redirect:/login";
        }
        
        try {
            List<Post> allPosts = postRepository.findByUserOrderByCreatedAtDesc(loggedInUser);
            postRepository.deleteAll(allPosts);
            redirectAttributes.addFlashAttribute("success", "全ての投稿と返信を削除しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "削除中にエラーが発生しました");
        }
        
        return "redirect:/mypage";
    }
}