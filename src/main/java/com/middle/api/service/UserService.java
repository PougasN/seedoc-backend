package com.middle.api.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.dto.RegisterRequest;
import com.middle.api.entity.Role;
import com.middle.api.entity.User;
import com.middle.api.repository.UserRepository;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null); // Returns null if user is not found
    }


    public User createUser(RegisterRequest request) {
        // Create the User entity and save to the database
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // assuming a password encoder is used
        user.setRole(request.getRole());

        userRepository.save(user);

        // Create Practitioner in FHIR server
        Practitioner practitioner = new Practitioner();
        practitioner.addName().setFamily(user.getUsername()); // Adjust to add more details if needed

        MethodOutcome outcome = fhirClient.create().resource(practitioner).execute();
        String practitionerId = outcome.getId().getIdPart();

        System.out.println("This is the practitioner Id i am getting: " + practitionerId);

        // Store Practitioner ID in User entity and save again
        user.setPractitionerId(practitionerId);
        userRepository.save(user);

        return user;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public List<User> getUsersByRole(String roleName) {
        // Remove "ROLE_" prefix if present
        String formattedRoleName = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
        Role role = Role.valueOf(formattedRoleName); // Convert to Role enum
        return userRepository.findByRole(role);
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }



}