package ma.Controle.gestionFactures.controllers;

import ma.Controle.gestionFactures.Security.JWTGenerator;
import ma.Controle.gestionFactures.dto.*;

import ma.Controle.gestionFactures.entities.Roles;
import ma.Controle.gestionFactures.entities.UserEntity;
import ma.Controle.gestionFactures.repositories.RoleRepository;
import ma.Controle.gestionFactures.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;



@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTGenerator jwtGenerator;
    private final JavaMailSender javaMailSender;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          JWTGenerator jwtGenerator,
                          JavaMailSender javaMailSender) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtGenerator = jwtGenerator;
        this.javaMailSender = javaMailSender;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginDto loginDto){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtGenerator.generateToken(authentication);
        return new ResponseEntity<>(new AuthResponseDto(token), HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterDto registerDto) {
        if (userRepository.existsByUsername(registerDto.getUsername())) {
            return new ResponseEntity<>("Username is taken", HttpStatus.BAD_REQUEST);
        }

        Optional<UserEntity> userByEmail = userRepository.findByEmail(registerDto.getEmail());
        if (userByEmail.isPresent()) {
            return new ResponseEntity<>("Email is taken", HttpStatus.BAD_REQUEST);
        }

        UserEntity user = new UserEntity();
        user.setUsername(registerDto.getUsername());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setEmail(registerDto.getEmail());

        Optional<Roles> optionalRole = roleRepository.findByName("USER");
        if (!optionalRole.isPresent()) {
            return new ResponseEntity<>("Role not found", HttpStatus.BAD_REQUEST);
        }

        Roles role = optionalRole.get();
        user.setRoles(Collections.singletonList(role));
        userRepository.save(user);

        return new ResponseEntity<>("User registered successfully!", HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody PasswordResetRequestDto requestDto) {
        // Find the user by email instead of username
        UserEntity user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur avec cet email non trouvé"));

        // Send a password reset email (this function is currently commented out)
        // sendPasswordResetEmail(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Un email de réinitialisation a été envoyé.");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    private void sendPasswordResetEmail(UserEntity user) {
        // Create a unique reset link with a token (can be handled by your frontend)
        String resetLink = "http://localhost:8080/api/auth/reset-password/confirm?email=" + user.getEmail();

        // Construct the email message
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Réinitialisation de votre mot de passe");
        message.setText("Bonjour,\n\nPour réinitialiser votre mot de passe, cliquez sur ce lien :\n" + resetLink);

        // Send the email
        try {
            javaMailSender.send(message);
            System.out.println("Email de réinitialisation envoyé à " + user.getEmail()); // Optional logging
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage()); // Optional logging for errors
            throw new RuntimeException("Erreur lors de l'envoi de l'email de réinitialisation.");
        }
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<String> confirmResetPassword(@RequestParam String email, @RequestBody String newPassword) {
        // Validate that the new password is not empty
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return new ResponseEntity<>("Le mot de passe ne peut pas être vide.", HttpStatus.BAD_REQUEST);
        }

        // Find the user by email
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur avec cet email non trouvé"));

        // Set the new password after encoding
        user.setPassword(passwordEncoder.encode(newPassword));

        // Save the updated user
        userRepository.save(user);

        return new ResponseEntity<>("Mot de passe réinitialisé avec succès.", HttpStatus.OK);
    }

}
