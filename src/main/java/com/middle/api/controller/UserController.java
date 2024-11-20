package com.middle.api.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.entity.User;
import com.middle.api.repository.UserRepository;
import com.middle.api.service.UserService;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<Object> getUserById(@PathVariable Long userId) {
        Optional<User> user = userService.getUserById(userId);
        return user.<ResponseEntity<Object>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(404).body("User not found"));
    }

    @GetMapping("/admins")
    public ResponseEntity<List<User>> getAllAdmins() {
        List<User> admins = userService.getUsersByRole("ROLE_ADMIN");
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<User>> getAllDoctors() {
        List<User> doctors = userService.getUsersByRole("ROLE_DOCTOR");
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/nurses")
    public ResponseEntity<List<User>> getAllNurses() {
        List<User> nurses = userService.getUsersByRole("ROLE_NURSE");
        return ResponseEntity.ok(nurses);
    }

    @GetMapping("/prereaders")
    public ResponseEntity<List<User>> getAllPreReaders() {
        List<User> prereaders = userService.getUsersByRole("ROLE_PREREADER");
        return ResponseEntity.ok(prereaders);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUserById(@PathVariable Long id) {
        try {
            Optional<User> userOptional = userService.getUserById(id);

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }

            User user = userOptional.get();
            String practitionerId = user.getPractitionerId();

            // Delete the Practitioner from the FHIR server, if the practitionerId exists
            if (practitionerId != null && !practitionerId.isEmpty()) {
                try {
                    fhirClient.delete().resourceById(new IdType("Practitioner", practitionerId)).execute();
                    System.out.println("Deleted Practitioner with ID: " + practitionerId);
                } catch (Exception e) {
                    System.err.println("Error deleting Practitioner from FHIR server: " + e.getMessage());
                    // Return a response if Practitioner deletion fails
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to delete associated Practitioner resource.");
                }
            } else {
                System.out.println("No Practitioner ID found for user. Skipping Practitioner deletion.");
            }

            // Delete the user from the database
            userService.deleteUserById(id);
            System.out.println("Deleted User with ID: " + id);

            return ResponseEntity.ok("User and associated Practitioner deleted successfully.");
        } catch (Exception e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting the user.");
        }
    }
}