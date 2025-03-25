//This controller "listens" for user responses.
//TODO: We need to figure out how to set the user input into a DTO, then convert to Model and add into database

package com.jydoc.deliverable3.Controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;




@Controller
public class indexController {

    // This method maps to the root URL ("/")
    @GetMapping("/")
    public String index(Model model) {
        // Add the user's name to the model (this can be dynamic)
        model.addAttribute("userName", "Guest");

        // Add the current date to the model
        model.addAttribute("currentDate", LocalDate.now());

        // Return the name of the Thymeleaf template ("index")
        return "index";
    }

@GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@Valid @ModelAttribute("userDTO") UserDTO userDTO, Model model) {
        boolean isAuthenticated = userService.authenticate(userDTO.getEmail(), userDTO.getPassword());

        if (isAuthenticated) {
            model.addAttribute("username", userDTO.getEmail());
            return "loginSuccess";
        } else {
            model.addAttribute("error", "Invalid email or password");
            return "login"; // Return back to login page with error message
        }
    }
}
