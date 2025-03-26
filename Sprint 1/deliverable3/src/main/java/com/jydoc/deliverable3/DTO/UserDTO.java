// This DTO accepts user input to fill variables, then is converted by
// UserService into a UserModel.
// Also used for business logic.
//test
package com.jydoc.deliverable3.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

//TODO: Authentication needs to be fixed here
@Data
public class UserDTO {  //@Data applies Getters, Setters, NoArgsConstructor, and AllArgsConstructor

    private int id;
    private boolean admin;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,}$", 
             message = "Password must contain at least one letter and one number")
    private String password;

    @NotBlank(message = "First name cannot be empty")
    @Pattern(regexp = "^[\\p{L}'-]+$", message = "First name can only contain letters, hyphens, and apostrophes")
    @Size(min = 2, max = 30, message = "First name must be between 2 and 30 characters")
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    @Pattern(regexp = "^[\\p{L}'-]+$", message = "Last name can only contain letters, hyphens, and apostrophes")
    @Size(min = 2, max = 30, message = "Last name must be between 2 and 30 characters")
    private String lastName;
}
