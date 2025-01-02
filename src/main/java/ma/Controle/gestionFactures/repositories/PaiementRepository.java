package ma.Controle.gestionFactures.repositories;

import ma.Controle.gestionFactures.entities.Paiement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaiementRepository extends CrudRepository<Paiement, Long> {
    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE p.facture.id = :factureId")
    Double sumMontantByFactureId(@Param("factureId") Long factureId);

    List<Paiement> findByFactureId(Long factureId);
}
