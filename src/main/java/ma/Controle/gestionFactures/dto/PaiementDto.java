package ma.Controle.gestionFactures.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
public class PaiementDto {
    private Long id;
    private LocalDate date;
    private Double montant;
}


