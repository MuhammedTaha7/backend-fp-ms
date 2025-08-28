package com.example.common.config;

import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PasswordMigration implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔄 Starting password migration...");

        try {
            List<UserEntity> users = userRepository.findAll();
            int updated = 0;

            for (UserEntity user : users) {
                String currentPassword = user.getPassword();

                // Check if password is already encrypted (BCrypt hashes start with $2a$, $2b$, or $2y$)
                if (currentPassword != null &&
                        !currentPassword.startsWith("$2a$") &&
                        !currentPassword.startsWith("$2b$") &&
                        !currentPassword.startsWith("$2y$")) {

                    // Encrypt the plain text password
                    String encryptedPassword = passwordEncoder.encode(currentPassword);
                    user.setPassword(encryptedPassword);
                    userRepository.save(user);
                    updated++;
                    System.out.println("✅ Updated password for user: " + user.getEmail());
                }
            }

            System.out.println("🎉 Migration completed successfully! Updated " + updated + " passwords.");

        } catch (Exception e) {
            System.err.println("❌ Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}