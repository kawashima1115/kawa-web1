package com.example.kawaweb.controller;

import java.util.List;

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

@Controller
public class PostController {
    
    @Autowired
    private PostRepository postRepository;
    
    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("posts", posts);
        
        // ログインユーザー情報をモデルに追加
        User loggedInUser = UserController.getLoggedInUser(session);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("isLoggedIn", UserController.isLoggedIn(session));
        
        return "index"; // src/main/resources/templates/index.html
    }
    
    // 新しい投稿処理（ログインユーザー対応）
    @PostMapping("/post")
    public String createPost(@RequestParam String content, 
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        
        // ログイン確認
        User loggedInUser = UserController.getLoggedInUser(session);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("error", "投稿するにはログインが必要です");
            return "redirect:/login";
        }
        
        // 投稿作成・保存
        Post post = new Post(content, loggedInUser);
        postRepository.save(post);
        
        redirectAttributes.addFlashAttribute("success", "投稿しました");
        return "redirect:/";
    }
    
    // 旧バージョンとの互換性を保つためのメソッド（オプション）
    @PostMapping("/post-guest")
    public String createGuestPost(@RequestParam String username, 
                                @RequestParam String content,
                                RedirectAttributes redirectAttributes) {
        
        // ゲスト投稿の場合、Userオブジェクトなしで投稿
        // ※実際の運用では推奨しませんが、移行期間用
        redirectAttributes.addFlashAttribute("error", "ゲスト投稿は廃止されました。ログインして投稿してください");
        return "redirect:/login";
    }
}