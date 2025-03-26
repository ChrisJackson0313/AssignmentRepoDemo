package com.jydoc.deliverable3.Service;

import com.jydoc.deliverable3.DTO.UserDTO;
import com.jydoc.deliverable3.Model.UserModel;
import com.jydoc.deliverable3.Repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean authenticate(String email, String password) {
        // Get the user from repository
        Optional<UserModel> userOptional = userRepository.findByEmail(email);

        // If no user found with this email
        if (userOptional.isEmpty()) {
            return false;
        }

        // Get the user from Optional
        UserModel user = userOptional.get();

        // Compare passwords (plain text comparison)
        return password.equals(user.getPassword());
    }

    public void registerUser(UserDTO userDTO) {
        UserModel user = new UserModel();
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        userRepository.save(user);
    }

    public boolean passwordMatches(
            @NotBlank(message = "Password cannot be empty")
            @Size(min = 6, message = "Password must be at least 6 characters")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,}$",
                    message = "Password must contain at least one letter and one number")
            String rawPassword,

            @NotBlank(message = "Encoded password cannot be empty")
            String encodedPassword) {

        // Simple plain text comparison (for development only)
        return rawPassword.equals(encodedPassword);
    }
}