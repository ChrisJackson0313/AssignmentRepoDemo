//The User Model is used to interact and create the database


package com.jydoc.deliverable3.Model;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor

public class UserModel {
//Each row in the database is a unique user containing all of these columns
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="userid")
    private int id;


    @Column(name="userisadmin")
    private boolean admin;

    @NotBlank
    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    @Pattern(regexp = "^[\\p{L}'-]+$", message = "First name can only contain letters, hyphens, and apostrophes")
    @Column(name="userfirstname")
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    @Pattern(regexp = "^[\\p{L}'-]+$", message = "Last name can only contain letters, hyphens, and apostrophes")
    @Column(name="userlastname")
    private String lastName;

    @NotBlank
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    @Column(name="email", unique=true)
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,}$",
            message = "Password must contain at least one letter and one number")
    @NotBlank(message = "Password cannot be blank")
    @Column(name="userpassword")
    private String password;


}
