package ma.Controle.gestionFactures.entities;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;


@Table(name = "paiement")
@Entity
public class Paiement {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La date de paiement est obligatoire")
    private LocalDate date;

    @NotNull(message = "Le montant est obligatoire")
    private Double montant;


    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id")
    private Facture facture;


    public Paiement() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Double getMontant() {
        return montant;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public Facture getFacture() {
        return facture;
    }

    public void setFacture(Facture facture) {
        this.facture = facture;
    }


    public @NotNull(message = "La date de paiement est obligatoire") LocalDate getDate() {
        return date;
    }

    public void setDate(@NotNull(message = "La date de paiement est obligatoire") LocalDate date) {
        this.date = date;
    }
}