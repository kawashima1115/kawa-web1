package com.example.kawaweb.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.kawaweb.model.Post;
import com.example.kawaweb.repository.PostRepository;

@Controller
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @GetMapping("/")
    public String index(Model model) {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("posts", posts);
        return "index"; // src/main/resources/templates/index.html
    }
    
    //aaaaa
    
    @PostMapping("/post")
    public String createPost(@RequestParam String username, @RequestParam String content) {
        Post post = new Post();
        post.setUsername(username);
        post.setContent(content);
        postRepository.save(post);
        return "redirect:/";
    }
}
