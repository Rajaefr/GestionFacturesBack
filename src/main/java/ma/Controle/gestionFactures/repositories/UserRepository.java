package ma.Controle.gestionFactures.repositories;


import ma.Controle.gestionFactures.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);  // Add method to find by email
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);  // Add method to check if email exists
}
