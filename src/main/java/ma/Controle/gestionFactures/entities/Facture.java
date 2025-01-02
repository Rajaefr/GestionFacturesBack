package ma.Controle.gestionFactures.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
public class Facture {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Temporal(TemporalType.DATE)
    private Date date;

    private Double montant;


    private String categorie;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL)
    private List<Paiement> paiements;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    private double montantRestant;

    private String etat ;

    public Facture() {
    }

    public String getCategorie() {

        return categorie;
    }

    public void setCategorie(String categorie) {

        this.categorie = categorie;
    }

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
    }

    public List<Paiement> getPaiements() {
        return paiements;
    }

    public void setPaiements(List<Paiement> paiements) {
        this.paiements = paiements;
    }



    public double getMontantRestant() {
        if (paiements == null) {
            return montant;
        }
        double totalPaiements = paiements.stream().mapToDouble(Paiement::getMontant).sum();
        return montant - totalPaiements;
    }

    public void setMontantRestant(double montantRestant) {
        this.montantRestant = montantRestant;
    }
    public String getEtat() {
        double montantRestant = getMontantRestant();
        if (montantRestant == 0) {
            return "Complète";
        } else {
            return "Incomplète";
        }
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }





}