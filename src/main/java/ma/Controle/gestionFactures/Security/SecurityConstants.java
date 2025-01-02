package ma.Controle.gestionFactures.Security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;

public class SecurityConstants {
    public static final long JWT_Expiration = 70000;
    public static final Key JWT_secret = Keys.secretKeyFor(SignatureAlgorithm.HS512); // Génère une clé sécurisée
}
