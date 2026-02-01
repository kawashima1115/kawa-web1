package com.example.kawaweb.controller;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.kawaweb.model.Post;
import com.example.kawaweb.model.User;
import com.example.kawaweb.repository.PostRepository;
import com.example.kawaweb.repository.UserRepository;

@Controller
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    // ユーザーIDの正規表現パターン（アルファベット、数字、_、.のみ）
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_.]+$");
    
    // 新規登録画面表示
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register"; // templates/register.html
    }
    
    // 新規登録処理
    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                              @RequestParam String userId,
                              @RequestParam String password,
                              @RequestParam(required = false) String email,
                              RedirectAttributes redirectAttributes) {
        
        // ユーザー名のバリデーション
        if (username == null || username.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ユーザー名を入力してください");
            return "redirect:/register";
        }
        
        // ユーザー名の長さチェック（3文字以上）
        if (username.length() < 3) {
            redirectAttributes.addFlashAttribute("error", "ユーザー名は3文字以上で入力してください");
            return "redirect:/register";
        }
        
        // ユーザーIDのバリデーション
        if (userId == null || userId.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ユーザーIDを入力してください");
            return "redirect:/register";
        }
        
        // ユーザーIDの長さチェック
        if (userId.length() < 6 || userId.length() > 20) {
            redirectAttributes.addFlashAttribute("error", "ユーザーIDは6文字以上20文字以内で入力してください");
            return "redirect:/register";
        }
        
        // ユーザーIDの文字種チェック
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            redirectAttributes.addFlashAttribute("error", "ユーザーIDはアルファベット、数字、_、.のみ使用できます");
            return "redirect:/register";
        }
        
        // ユーザーID重複チェック
        if (userRepository.existsByUserId(userId)) {
            redirectAttributes.addFlashAttribute("error", "そのユーザーIDは既に使用されています");
            return "redirect:/register";
        }
        
        // ユーザー名重複チェック
        if (userRepository.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("error", "そのユーザー名は既に使用されています");
            return "redirect:/register";
        }
        
        // メールアドレス重複チェック
        if (email != null && !email.isEmpty() && userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "そのメールアドレスは既に使用されています");
            return "redirect:/register";
        }
        
        // 新規ユーザー作成・保存
        User user = new User(username, userId, password, email);
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "登録が完了しました。ログインしてください。");
        return "redirect:/login";
    }
    
    // ログイン画面表示
    @GetMapping("/login")
    public String loginForm(Model model) {
        return "login"; // templates/login.html
    }
    
    // ログイン処理（ユーザーIDとパスワードのみ）
    @PostMapping("/login")
    public String loginUser(@RequestParam String userId,
                           @RequestParam String password,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        
        // ユーザーIDで認証
        Optional<User> userOpt = userRepository.findByUserId(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 簡単なパスワードチェック（実際にはハッシュ化が必要）
            if (user.getPassword().equals(password)) {
                // セッションにユーザー情報を保存
                session.setAttribute("loggedInUser", user);
                redirectAttributes.addFlashAttribute("success", "ログインしました");
                return "redirect:/";
            }
        }
        
        redirectAttributes.addFlashAttribute("error", "ユーザーIDまたはパスワードが間違っています");
        return "redirect:/login";
    }
    
    // ログアウト処理
    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute("loggedInUser");
        redirectAttributes.addFlashAttribute("success", "ログアウトしました");
        return "redirect:/";
    }
    
    // ユーザープロフィール閲覧
    @GetMapping("/user/{userId}")
    public String viewProfile(@PathVariable String userId,
                             HttpSession session,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        
        // ユーザーIDで検索
        Optional<User> userOpt = userRepository.findByUserId(userId);
        
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ユーザーが見つかりませんでした");
            return "redirect:/";
        }
        
        User user = userOpt.get();
        User loggedInUser = getLoggedInUser(session);
        
        // 自分のプロフィールの場合はマイページにリダイレクト
        if (loggedInUser != null && loggedInUser.getId().equals(user.getId())) {
            return "redirect:/mypage";
        }
        
        // ユーザーの投稿を取得
        List<Post> allPosts = postRepository.findByUserOrderByCreatedAtDesc(user);
        
        // 親投稿と返信を分離
        List<Post> userPosts = allPosts.stream()
                .filter(post -> post.getParent() == null)
                .collect(Collectors.toList());
        
        List<Post> userReplies = allPosts.stream()
                .filter(post -> post.getParent() != null)
                .collect(Collectors.toList());
        
        // 利用日数を計算
        long daysSinceRegistration = java.time.temporal.ChronoUnit.DAYS.between(
            user.getCreatedAt().toLocalDate(),
            java.time.LocalDate.now()
        );
        
        model.addAttribute("user", user);
        model.addAttribute("userPosts", userPosts);
        model.addAttribute("userReplies", userReplies);
        model.addAttribute("postCount", userPosts.size());
        model.addAttribute("replyCount", userReplies.size());
        model.addAttribute("totalCount", allPosts.size());
        model.addAttribute("daysSinceRegistration", daysSinceRegistration);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("isLoggedIn", isLoggedIn(session));
        
        return "user-profile";
    }
    
    // ログイン状態チェックのヘルパーメソッド
    public static User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
    
    // ログイン済みかチェック
    public static boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedInUser") != null;
    }
}