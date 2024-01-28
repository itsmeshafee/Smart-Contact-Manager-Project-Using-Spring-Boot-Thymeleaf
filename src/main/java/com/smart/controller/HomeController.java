package com.smart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("title","Home - Smart Contact Manager");
		return "home";
	}
	
	@GetMapping("/signin")
	public String login(@ModelAttribute User user ,Model model) {
		model.addAttribute("title","Login - Smart Contact Manager");
		return "login";
	}
	
	@GetMapping("/about")
	public String about(Model model) {
		model.addAttribute("title","About - Smart Contact Manager");
		return "about";
	}
	
	@GetMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title","Register - Smart Contact Manager");
		model.addAttribute("user", new User());
		return "signup";
	}
	//this handler for register user
	@PostMapping("/do_register")
	public String registerUuser(
			@Valid
			@ModelAttribute("user") User user,
			BindingResult bindingResult,
			@RequestParam(name = "agreement", defaultValue = "false") boolean agreement,
			Model model,
			HttpSession session) {
		try {
			if (!agreement) {
				System.out.println("you have not agreed");
				throw new Exception("you have not agreed");
			}
		if (bindingResult.hasErrors()) {
			
			System.out.println("ERROR" + bindingResult.toString());
			model.addAttribute("user",user);
			return "signup";
		}
		user.setRole("ROLE_USER");
		user.setEnabled(true);
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		User result = userRepository.save(user);
		System.out.println("User" + user);
		System.out.println("results" + result);
		System.out.println("Agreement" + agreement);
		model.addAttribute("user", new User());
		session.setAttribute("message", new Message("Successfully registered", "alert-success"));
		return "signup";
		}catch(Exception e) {
			e.printStackTrace();
			model.addAttribute("user", user);
			session.setAttribute("message", new Message("Something went wrong !!"+e.getMessage(),"alert-danger"));
			return "signup";			
		}
		
	}
}
