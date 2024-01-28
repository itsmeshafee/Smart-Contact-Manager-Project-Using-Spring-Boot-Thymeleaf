package com.smart.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.repository.ContactRepository;
import com.smart.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;
	
	//method to add common data
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USER NAME " + userName);
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER: " + user);
		model.addAttribute("user",user);
	}
	
	@GetMapping("/index")
	public String dashboard(Model model, Principal principal) {
		
		model.addAttribute("title","User Dashboard");
		return "user_dashboard";
	}
	
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact", new Contact());
		return "add_contact_form";
	}
	
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, 
								@RequestParam("profileImage") MultipartFile file,
								Principal principal,
								HttpSession session) {
		try {
		System.out.println("DATA " + contact);
		String name = principal.getName();
		User user = userRepository.getUserByUserName(name);
		
		//image processing
		if (file.isEmpty()) {
			System.out.println("File is Emplty");
			contact.setImageUrl("contact.png");
			
		}else {
			contact.setImageUrl(file.getOriginalFilename());
			File saveFile = new ClassPathResource("static/image").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING );
		}
		
		contact.setUser(user);		
		user.getContacts().add(contact);
		userRepository.save(user);
		System.out.println("ADDED TO THE DATABASE");
		session.setAttribute("message",new Message("Your Contact is Added", "success"));
		}catch(Exception e) {
			System.out.println("ERROR " + e.getMessage());
			e.printStackTrace();
			session.setAttribute("message",new Message("Something Wrong !! Try Again", "danger"));
		}
		return "add_contact_form";
	}
	
	//per page =5
	//current page =0
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model model, Principal principal) {
		model.addAttribute("title", "Show User Contacts");
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		// List<Contact> contacts = user.getContacts();

		Pageable pageable = PageRequest.of(page,2);
		Page<Contact> contacts = contactRepository.findContactsByUser(user.getId(),pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());

		return "show_contacts";
	}

	//showing particular contact details
	@RequestMapping("/{cid}/contact")
	public String showContactDetail(@PathVariable("cid") Integer cId, Model model, Principal principal){
		System.out.println(cId);
		Contact contact = contactRepository.findById(cId).get();

		//check
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);

		if (user.getId() ==contact.getUser().getId()) {
			model.addAttribute("contact", contact);	
			model.addAttribute("title", contact.getName());
		}
		
		return "contact_detail";
	}

	@GetMapping("/delete/{cid}")
	public String deleteContact(
								@PathVariable("cid") Integer cid, 
								Model model, 
								Principal principal,
								HttpSession session){
		
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		Contact contact = contactRepository.findById(cid).get();
		if (user.getId()==contact.getUser().getId()) {

			user.getContacts().remove(contact);
			userRepository.save(user);
			session.setAttribute("message",new Message("Contact Deleted Successfully !!", "success"));
		}		
		return "redirect:/user/show-contacts/0";

	}

	//update form
	@PostMapping("/update-contact/{cid}")
	public String updateContact(@PathVariable("cid") Integer cid ,Model model){
		model.addAttribute("title", "Update Contact");
		Contact contact = contactRepository.findById(cid).get();
		model.addAttribute("contact", contact);
		return "update_contact";
	}

	@PostMapping("/process-update")
	public String processUpdate(@ModelAttribute("contact") Contact contact,
								Principal principal,
								@RequestParam("profileImage") MultipartFile file,
								HttpSession session,
								Model model) {
		
		try {
			Contact oldContactDetail = contactRepository.findById(contact.getCid()).get();
			if (!file.isEmpty()) {
				//delete old photo
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deleteFile,oldContactDetail.getImageUrl());
				file1.delete();

				//update new photo
				File saveFile = new ClassPathResource("static/image").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING );
			contact.setImageUrl(file.getOriginalFilename());
			}else{
				contact.setImageUrl(oldContactDetail.getImageUrl());
			}

			User user = userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			contactRepository.save(contact);
			session.setAttribute("message", new Message("Your Contact is Updated", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("CONTACT NAME " + contact.getName());
		System.out.println("CONTACT ID "+ contact.getCid());
		return "redirect:/user/"+contact.getCid()+"/contact";
	}

	//your Profile Hadler
	@GetMapping("/profile")
	public String yourProfile(Model model){
		model.addAttribute("title", "Profile Page");
		return "profile";
	}
	
	

}
