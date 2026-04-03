package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> allUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    public User updateUser(Integer id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        if (updatedUser.getFullName() != null) {
            user.setFullName(updatedUser.getFullName());
        }
        if (updatedUser.getPhone() != null) {
            user.setPhone(updatedUser.getPhone());
        }
        if (updatedUser.getLocation() != null) {
            user.setLocation(updatedUser.getLocation());
        }
        if (updatedUser.getBio() != null) {
            user.setBio(updatedUser.getBio());
        }
        if (updatedUser.getAvatar() != null) {
            user.setAvatar(updatedUser.getAvatar());
        }
        if (updatedUser.getExperience() != null) {
            user.setExperience(updatedUser.getExperience());
        }
        if (updatedUser.getRating() != null) {
            user.setRating(updatedUser.getRating());
        }
        if (updatedUser.getTotalDeals() != null) {
            user.setTotalDeals(updatedUser.getTotalDeals());
        }
        if (updatedUser.getVerified() != null) {
            user.setVerified(updatedUser.getVerified());
        }
        if (updatedUser.getEnabled() != null) {
            user.setEnabled(updatedUser.getEnabled());
        }
        
        return userRepository.save(user);
    }

    public User activateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User deactivateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setEnabled(false);
        return userRepository.save(user);
    }

    /**
     * Sets a new bcrypt password for a user (super-admin only at controller layer).
     */
    public User setPasswordByAdmin(Integer id, String rawPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }
}
