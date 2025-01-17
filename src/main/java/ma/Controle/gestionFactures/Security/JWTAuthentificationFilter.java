package ma.Controle.gestionFactures.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JWTAuthentificationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthentificationFilter.class); // Logger for debugging

    private final JWTGenerator tokenGenerator;
    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public JWTAuthentificationFilter(JWTGenerator tokenGenerator, CustomUserDetailsService customUserDetailsService) {
        this.tokenGenerator = tokenGenerator;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            filterChain.doFilter(request, response); // Skip filtering for OPTIONS requests
            return;
        }

        String token = getJWTFromRequest(request);

        if (StringUtils.hasText(token) && tokenGenerator.validateToken(token)) {
            String username = tokenGenerator.getUsernameFromJWT(token);
            logger.info("Token validated for user: {}", username); // Log username associated with valid token

            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            logger.warn("Invalid or missing token"); // Log invalid/missing token
        }

        filterChain.doFilter(request, response);
    }

    private String getJWTFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Extract token by removing "Bearer " prefix
        }
        return null; // Return null if no valid token is found
    }
}
