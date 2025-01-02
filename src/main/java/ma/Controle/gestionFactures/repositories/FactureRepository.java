package ma.Controle.gestionFactures.repositories;

import ma.Controle.gestionFactures.entities.Facture;
import org.springframework.boot.logging.java.JavaLoggingSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactureRepository extends CrudRepository<Facture, Long> {
    List<Facture> findByCategorieContainingIgnoreCase(String categorie);

    List<Facture> findByUser_Username(String username);


}
