package ma.Controle.gestionFactures.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthEntryPoint authEntryPoint;
    private final CustomUserDetailsService userDetailsService;
    private final JWTGenerator jwtGenerator;

    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthEntryPoint authEntryPoint,
                          JWTGenerator jwtGenerator) {
        this.userDetailsService = userDetailsService;
        this.authEntryPoint = authEntryPoint;
        this.jwtGenerator = jwtGenerator;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless authentication
                .cors(Customizer.withDefaults()) // Enable CORS using a CORS configuration source
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authEntryPoint)) // Unauthorized entry point
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless session management
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Allow preflight requests
                        .requestMatchers("/factures", "/paiements", "/factures-echeance-proche", "/paiements-categories", "/addfacture","/factures/**","/delete-facture/").authenticated()
                        .requestMatchers("/", "/login", "/register", "/api/auth/**").permitAll()   // Public endpoints
                        .anyRequest().authenticated()) // All other endpoints require authentication
                .addFilterBefore(jwtAuthentificationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JWTAuthentificationFilter jwtAuthentificationFilter() {
        return new JWTAuthentificationFilter(jwtGenerator, userDetailsService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:3000"); // Your React app's origin
        configuration.addAllowedMethod("*"); // Allow all HTTP methods
        configuration.addAllowedHeader("*"); // Allow all headers
        configuration.setAllowCredentials(true); // Allow credentials

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
