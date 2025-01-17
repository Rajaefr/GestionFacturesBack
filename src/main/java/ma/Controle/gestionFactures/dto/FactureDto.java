package ma.Controle.gestionFactures.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class FactureDto {
    private Long id;
    private String description;
    private String categorie;
    private Double montant;
    private Double montantRestant;
    private LocalDate date;
    private String etat;
    private List<PaiementDto> paiements;

}
