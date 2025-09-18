package com.example.kawaweb.controller;

import java.util.Optional;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.kawaweb.model.User;
import com.example.kawaweb.repository.UserRepository;

@Controller
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    // 新規登録画面表示
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register"; // templates/register.html
    }
    
    // 新規登録処理
    @PostMapping("/register")
    public String registerUser(@RequestParam String username, 
                              @RequestParam String password,
                              @RequestParam String email,
                              RedirectAttributes redirectAttributes) {
        
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
        User user = new User(username, password, email);
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "登録が完了しました。ログインしてください。");
        return "redirect:/login";
    }
    
    // ログイン画面表示
    @GetMapping("/login")
    public String loginForm(Model model) {
        return "login"; // templates/login.html
    }
    
    // ログイン処理
    @PostMapping("/login")
    public String loginUser(@RequestParam String username,
                           @RequestParam String password,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        
        // ユーザー認証
        Optional<User> userOpt = userRepository.findByUsername(username);
        
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
        
        redirectAttributes.addFlashAttribute("error", "ユーザー名またはパスワードが間違っています");
        return "redirect:/login";
    }
    
    // ログアウト処理
    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute("loggedInUser");
        redirectAttributes.addFlashAttribute("success", "ログアウトしました");
        return "redirect:/";
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