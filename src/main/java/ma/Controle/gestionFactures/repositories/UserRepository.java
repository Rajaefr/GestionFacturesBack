package ma.Controle.gestionFactures.repositories;


import ma.Controle.gestionFactures.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);  // Add method to find by email
    Boolean existsByUsername(String username);

    Optional<UserEntity> existsByEmail(String email);


}
