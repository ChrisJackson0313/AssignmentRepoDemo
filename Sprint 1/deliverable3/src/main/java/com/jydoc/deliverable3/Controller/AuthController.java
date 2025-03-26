package com.jydoc.deliverable3.Controller;

import com.jydoc.deliverable3.DTO.UserDTO;
import com.jydoc.deliverable3.Model.UserModel;
import com.jydoc.deliverable3.Repository.UserRepository;
import com.jydoc.deliverable3.Service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        return "auth/login";  // Must match template path
    }

    @PostMapping("/login")
    public String handleLogin(@Valid @ModelAttribute("userDTO") UserDTO userDTO,
                              BindingResult result,
                              Model model,
                              HttpSession session) {

        // Always add userDTO to maintain form data
        model.addAttribute("userDTO", userDTO);

        if (result.hasErrors()) {
            return "auth/login";
        }

        // Check if user exists
        Optional<UserModel> userOpt = userRepository.findByEmail(userDTO.getEmail());
        if (userOpt.isEmpty()) {
            System.out.println("User not found - should display error");

            model.addAttribute("authError", "Account not found");
            return "auth/login";
        }

        // Verify password
        UserModel user = userOpt.get();
        if (!user.getPassword().equals(userDTO.getPassword())) {
            model.addAttribute("authError", "Incorrect password");
            return "auth/login";
        }

        // Successful login
        session.setAttribute("user", user);
        return "redirect:/dashboard";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        return "auth/register";
    }
}