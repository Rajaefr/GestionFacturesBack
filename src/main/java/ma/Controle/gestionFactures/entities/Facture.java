package ma.Controle.gestionFactures.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;

@Entity
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    @NotNull
    @Temporal(TemporalType.DATE)
    private Date date;

    private Double montant;

    private String categorie;

    @JsonManagedReference
    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<Paiement> paiements;


    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private Double montantRestant;

    private String etat;

    // Constructors, Getters, and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Double getMontant() {
        return montant;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
        updateMontantRestantAndEtat(); // Recalculate montantRestant and etat when montant changes
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public List<Paiement> getPaiements() {
        return paiements;
    }

    public void setPaiements(List<Paiement> paiements) {
        this.paiements = paiements;
        updateMontantRestantAndEtat(); // Ensure calculation happens here
    }

    public Double getMontantRestant() {
        return montantRestant;
    }

    public void setMontantRestant(Double montantRestant) {
        if (montantRestant < 0) {
            throw new IllegalArgumentException("Remaining amount cannot be negative. Montant restant: " + montantRestant);
        }
        this.montantRestant = montantRestant;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        if ("Paid".equalsIgnoreCase(etat)) {
            this.etat = "Paid";
            this.montantRestant = 0.0;
        } else if ("Unpaid".equalsIgnoreCase(etat)) {
            this.etat = "Unpaid";
            updateMontantRestantAndEtat(); // Ensure the state is consistent with the remaining amount
        } else {
            throw new IllegalArgumentException("Invalid status. Allowed values: 'Paid', 'Unpaid'.");
        }
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Helper to update montantRestant and etat based on payments
    public void updateMontantRestantAndEtat() {
        if (paiements != null && montant != null) {
            double totalPaiements = paiements.stream().mapToDouble(Paiement::getMontant).sum();
            this.montantRestant = montant - totalPaiements;
            this.etat = montantRestant == 0 ? "Paid" : "Unpaid";
        }
    }
}
