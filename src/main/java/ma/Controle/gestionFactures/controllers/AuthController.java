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
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequestDto requestDto) {
        // Find the user by email instead of username
        UserEntity user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur avec cet email non trouvé"));

        // Send a password reset email
        sendPasswordResetEmail(user);

        return new ResponseEntity<>("Un email de réinitialisation a été envoyé.", HttpStatus.OK);
    }

    private void sendPasswordResetEmail(UserEntity user) {
        // Create a unique reset link (you can use your frontend to trigger this flow)
        String resetLink = "http://localhost:8080/api/auth/reset-password/confirm?email=" + user.getEmail();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Réinitialisation de votre mot de passe");
        message.setText("Bonjour,\n\nPour réinitialiser votre mot de passe, cliquez sur ce lien :\n" + resetLink);

        javaMailSender.send(message);
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<String> confirmResetPassword(@RequestParam String email, @RequestBody String newPassword) {
        // Find the user by email instead of username
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur avec cet email non trouvé"));

        // Set the new password
        user.setPassword(passwordEncoder.encode(newPassword));

        // Save the updated user
        userRepository.save(user);

        return new ResponseEntity<>("Mot de passe réinitialisé avec succès.", HttpStatus.OK);
    }
}
