package ma.Controle.gestionFactures.repositories;

import ma.Controle.gestionFactures.entities.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Roles,Integer> {
    Optional<Roles> findByName(String name);
}
