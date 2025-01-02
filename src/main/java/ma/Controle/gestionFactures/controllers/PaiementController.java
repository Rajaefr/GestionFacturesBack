package ma.Controle.gestionFactures.controllers;

import jakarta.validation.Valid;
import ma.Controle.gestionFactures.entities.Facture;
import ma.Controle.gestionFactures.entities.Paiement;
import ma.Controle.gestionFactures.repositories.FactureRepository;
import ma.Controle.gestionFactures.repositories.PaiementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Controller
public class PaiementController {

    private static final Logger logger = LoggerFactory.getLogger(PaiementController.class);

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private FactureRepository factureRepository;

    @GetMapping("/list-paiement/{factureId}")
    public String listPaiements(@PathVariable Long factureId, Model model) {
        List<Paiement> paiements = paiementRepository.findByFactureId(factureId);
        model.addAttribute("paiements", paiements);
        model.addAttribute("factureId", factureId);
        return "list-paiements";
    }

    @GetMapping("/add-paiement/{factureId}")
    public String addPaiementForm(@PathVariable("factureId") Long factureId, Model model) {
        Paiement paiement = new Paiement();
        paiement.setDate(LocalDate.now()); // Use current date
        paiement.setFacture(factureRepository.findById(factureId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id: " + factureId)));
        model.addAttribute("paiement", paiement);
        return "add-paiement";
    }

    @Transactional
    @PostMapping("/add-paiement/{factureId}")
    public String addPaiement(@PathVariable("factureId") Long factureId,
                              @Valid @ModelAttribute("paiement") Paiement paiement,
                              BindingResult result, Model model) {

        logger.info("Adding payment for facture ID: {}", factureId);
        logger.info("Received payment date: {}", paiement.getDate());

        // Set the current date if the date is not provided
        if (paiement.getDate() == null) {
            logger.warn("Date is null. Setting current date.");
            paiement.setDate(LocalDate.now());
        }

        // Re-run validation manually after setting the date
        if (result.hasErrors()) {
            logger.error("Validation errors occurred: {}", result.getAllErrors());
            paiement.setFacture(factureRepository.findById(factureId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id: " + factureId)));
            model.addAttribute("paiement", paiement);
            return "add-paiement";
        }

        // Save the payment
        paiementRepository.save(paiement);
        logger.info("Payment saved successfully: {}", paiement);

        // Update facture's montantRestant and etat
        updateFactureStatus(factureId);

        return "redirect:/list-paiement/" + factureId;
    }



    @GetMapping("/update-paiement/{id}")
    public String showUpdateForm(@PathVariable("id") long id, Model model) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paiement Id: " + id));
        model.addAttribute("paiement", paiement);
        return "update-paiement";
    }

    @Transactional
    @PostMapping("/update-paiement/{id}")
    public String updatePaiement(@PathVariable("id") long id,
                                 @Valid @ModelAttribute("paiement") Paiement paiement,
                                 BindingResult result, Model model) {

        logger.info("Updating payment with ID: {}", id);

        if (result.hasErrors()) {
            logger.error("Validation errors occurred during update: {}", result.getAllErrors());
            model.addAttribute("paiement", paiement);
            return "update-paiement";
        }

        paiement.setId(id);
        paiementRepository.save(paiement);
        logger.info("Payment updated successfully: {}", paiement);

        // Update facture's montantRestant and etat
        updateFactureStatus(paiement.getFacture().getId());

        return "redirect:/list-paiement/" + paiement.getFacture().getId();
    }

    @Transactional
    @GetMapping("/delete-paiement/{id}")
    public String deletePaiement(@PathVariable("id") long id) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paiement Id: " + id));
        Long factureId = paiement.getFacture().getId();
        paiementRepository.delete(paiement);
        logger.info("Payment deleted successfully: {}", paiement);

        // Update facture's montantRestant and etat
        updateFactureStatus(factureId);

        return "redirect:/list-paiement/" + factureId;
    }

    // Utility method to update facture's montantRestant and etat
    private void updateFactureStatus(Long factureId) {
        List<Paiement> paiements = paiementRepository.findByFactureId(factureId);
        double montantPaye = paiements.stream().mapToDouble(Paiement::getMontant).sum();

        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id: " + factureId));

        double montantRestant = facture.getMontant() - montantPaye;
        facture.setMontantRestant(montantRestant);

        facture.setEtat(montantRestant == 0 ? "Complète" : "Incomplète");
        factureRepository.save(facture);
        logger.info("Facture status updated for facture ID: {} - Remaining: {}, Status: {}",
                factureId, montantRestant, facture.getEtat());
    }
}
