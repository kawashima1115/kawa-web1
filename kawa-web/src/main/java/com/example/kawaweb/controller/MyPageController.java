package com.example.kawaweb.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
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
    
    // ユーザーIDの正規表現パターン
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_.]+$");
    
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
    
    // 基本情報更新（ユーザー名・ユーザーID・自己紹介）
    @PostMapping("/mypage/update-profile")
    public String updateBasicProfile(
            @RequestParam String username,
            @RequestParam String userId,
            @RequestParam(required = false) String bio,
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
        
        // ユーザー名のバリデーション
        if (username == null || username.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ユーザー名を入力してください");
            return "redirect:/mypage/edit";
        }
        
        if (username.length() < 3) {
            redirectAttributes.addFlashAttribute("error", "ユーザー名は3文字以上で入力してください");
            return "redirect:/mypage/edit";
        }
        
        // ユーザーIDのバリデーション
        if (userId == null || userId.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ユーザーIDを入力してください");
            return "redirect:/mypage/edit";
        }
        
        if (userId.length() < 6 || userId.length() > 20) {
            redirectAttributes.addFlashAttribute("error", "ユーザーIDは6文字以上20文字以内で入力してください");
            return "redirect:/mypage/edit";
        }
        
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            redirectAttributes.addFlashAttribute("error", "ユーザーIDはアルファベット、数字、_、.のみ使用できます");
            return "redirect:/mypage/edit";
        }
        
        // 自己紹介のバリデーション
        if (bio != null && bio.length() > 500) {
            redirectAttributes.addFlashAttribute("error", "自己紹介は500文字以内で入力してください");
            return "redirect:/mypage/edit";
        }
        
        // ユーザー名の変更チェック
        if (!username.equals(user.getUsername())) {
            // 他のユーザーが使用していないかチェック
            if (userRepository.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("error", "そのユーザー名は既に使用されています");
                return "redirect:/mypage/edit";
            }
            user.setUsername(username);
            updated = true;
        }
        
        // ユーザーIDの変更チェック
        if (!userId.equals(user.getUserId())) {
            // 他のユーザーが使用していないかチェック
            Optional<User> existingUser = userRepository.findByUserId(userId);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "そのユーザーIDは既に使用されています");
                return "redirect:/mypage/edit";
            }
            user.setUserId(userId);
            updated = true;
        }
        
        // 自己紹介の変更チェック
        String currentBio = user.getBio() == null ? "" : user.getBio();
        String newBio = bio == null ? "" : bio.trim();
        
        if (!currentBio.equals(newBio)) {
            user.setBio(newBio.isEmpty() ? null : newBio);
            updated = true;
        }
        
        if (updated) {
            userRepository.save(user);
            session.setAttribute("loggedInUser", user);
            redirectAttributes.addFlashAttribute("success", "基本情報を更新しました");
        } else {
            redirectAttributes.addFlashAttribute("info", "変更内容がありませんでした");
        }
        
        return "redirect:/mypage/edit";
    }
    
    // アイコン更新
    @PostMapping("/mypage/update-icon")
    public String updateIcon(
            @RequestParam MultipartFile iconFile,
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
        
        if (iconFile == null || iconFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "画像ファイルを選択してください");
            return "redirect:/mypage/edit";
        }
        
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
            
            // 古いアイコンファイルを削除
            if (user.getIconPath() != null && !user.getIconPath().isEmpty()) {
                try {
                    Path oldFile = Paths.get(UPLOAD_DIR + user.getIconPath());
                    Files.deleteIfExists(oldFile);
                } catch (IOException e) {
                    System.out.println("古いアイコンの削除に失敗: " + e.getMessage());
                }
            }
            
            user.setIconPath(filename);
            userRepository.save(user);
            session.setAttribute("loggedInUser", user);
            
            redirectAttributes.addFlashAttribute("success", "アイコンを変更しました");
        } catch (IOException e) {
            System.out.println("アイコンアップロードエラー: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "アイコンのアップロードに失敗しました");
        }
        
        return "redirect:/mypage/edit";
    }
    
    // パスワード更新
    @PostMapping("/mypage/update-password")
    public String updatePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
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
        
        // 現在のパスワードチェック
        if (!user.getPassword().equals(currentPassword)) {
            redirectAttributes.addFlashAttribute("error", "現在のパスワードが間違っています");
            return "redirect:/mypage/edit";
        }
        
        // 新しいパスワードのバリデーション
        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "新しいパスワードは8文字以上で設定してください");
            return "redirect:/mypage/edit";
        }
        
        // パスワード確認チェック
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "新しいパスワードと確認用パスワードが一致しません");
            return "redirect:/mypage/edit";
        }
        
        user.setPassword(newPassword);
        userRepository.save(user);
        session.setAttribute("loggedInUser", user);
        
        redirectAttributes.addFlashAttribute("success", "パスワードを変更しました");
        return "redirect:/mypage/edit";
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