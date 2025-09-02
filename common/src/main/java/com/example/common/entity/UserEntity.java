package com.example.common.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
@Setter
@Document(collection = "users")
public class UserEntity implements UserDetails {

    // ----------------------
    // Common User Fields
    // ----------------------
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;
    private String password;
    private String name;
    private String role; // "1300" (student), "1200" (lecturer), "1100" (admin)
    private String dateOfBirth;

    // ----------------------
    // General Profile Information
    // ----------------------
    private String profilePic;
    private String coverPic;
    private String title;
    private String university;
    private String bio;
    private String website;
    private String phoneNumber;
    private Map<String, String> socialLinks;

    // ----------------------
    // Student-Specific Fields
    // ----------------------
    private String academicYear;
    private String department;
    private String status;


    // ----------------------
    // Lecturer-Specific Fields
    // ----------------------
    private String specialization;
    private String employmentType;
    private String experience; // Years of Experience
    private Double rating; // Lecturer rating


    // ----------------------
    // System Metadata
    // ----------------------
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // --- UserDetails METHODS ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName;
        switch (this.role) {
            case "1100":
                roleName = "ROLE_ADMIN";
                break;
            case "1200":
                roleName = "ROLE_LECTURER";
                break;
            case "1300":
                roleName = "ROLE_STUDENT";
                break;
            default:
                roleName = "ROLE_USER";
                break;
        }
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}