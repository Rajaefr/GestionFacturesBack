package ma.Controle.gestionFactures.controllers;

import jakarta.validation.Valid;
import ma.Controle.gestionFactures.entities.Facture;
import ma.Controle.gestionFactures.entities.Paiement;
import ma.Controle.gestionFactures.repositories.FactureRepository;
import ma.Controle.gestionFactures.repositories.PaiementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class PaiementController {
    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private FactureRepository factureRepository;


    @GetMapping("/list-paiement/{factureId}")
    public String listPaiements(@PathVariable Long factureId, Model model) {
        List<Paiement> paiements = paiementRepository.findByFactureId(factureId);
        model.addAttribute("paiements", paiements);
        model.addAttribute("factureId", factureId); // Ajout de factureId au modèle
        return "list-paiements";
    }


    @GetMapping("/add-paiement/{factureId}")
    public String addPaiementForm(@PathVariable("factureId") Long factureId, Model model) {
        Paiement paiement = new Paiement();
        paiement.setFacture(factureRepository.findById(factureId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id:" + factureId)));
        model.addAttribute("paiement", paiement);
        return "add-paiement";

    }


    // Ajoute un nouveau paiement
    @PostMapping("/add-paiement/{factureId}")
    public String addPaiement(@PathVariable("factureId") Long factureId,
                              @Valid @ModelAttribute("paiement") Paiement paiement,
                              BindingResult result, Model model) {
        if (result.hasErrors()) {
            paiement.setFacture(factureRepository.findById(factureId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id:" + factureId))); // Récupère la facture à associer
            model.addAttribute("paiement", paiement);
            return "add-paiement";
        }

        paiementRepository.save(paiement);

        List<Paiement> paiements = paiementRepository.findByFactureId(factureId);
        double montantPaye = 0.0;


        for (Paiement p : paiements) {
            montantPaye += p.getMontant();
        }


        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id:" + factureId));


        double montantRestant = facture.getMontant() - montantPaye;
        facture.setMontantRestant(montantRestant);

        if (montantRestant == 0) {
            facture.setEtat("Complète");
        } else {
            facture.setEtat("Incomplète");
        }

        factureRepository.save(facture);

        model.addAttribute("paiements", paiements);
        return "list-paiements";
    }


    @GetMapping("/update-paiement/{id}")
    public String showUpdateForm(@PathVariable("id") long id, Model model) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paiement Id:" + id));
        model.addAttribute("paiement", paiement);
        return "update-paiement";
    }


    @PostMapping("/update-paiement/{id}")
    public String updatePaiement(@PathVariable("id") long id,
                                 @Valid @ModelAttribute("paiement") Paiement paiement,
                                 BindingResult result, Model model) {
        if (result.hasErrors()) {
            paiement.setId(id);
            return "update-paiement";
        }

        Paiement paiement1 = paiementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paiement Id:" + id));


        paiement.setFacture(paiement1.getFacture());


        paiementRepository.save(paiement);


        model.addAttribute("paiements", paiementRepository.findByFactureId(paiement1.getFacture().getId()));
        return "list-paiements";
    }


    @GetMapping("/delete-paiement/{id}")
    public String deletePaiement(@PathVariable("id") long id) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paiement Id:" + id));
        paiementRepository.delete(paiement);
        return "list-paiements";
    }
}
