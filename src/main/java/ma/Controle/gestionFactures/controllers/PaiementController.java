package ma.Controle.gestionFactures.controllers;


import jakarta.validation.Valid;
import ma.Controle.gestionFactures.entities.Facture;
import ma.Controle.gestionFactures.entities.Paiement;
import ma.Controle.gestionFactures.entities.PaymentCategorySummary;
import ma.Controle.gestionFactures.repositories.FactureRepository;
import ma.Controle.gestionFactures.repositories.PaiementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.Principal;




@Controller
public class PaiementController {

    private static final Logger logger = LoggerFactory.getLogger(PaiementController.class);

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private FactureRepository factureRepository;

    @CrossOrigin(origins = "http://localhost:3000")

    @GetMapping("/list-paiement/{factureId}")
    public String listPaiements(@PathVariable Long factureId, Model model) {
        List<Paiement> paiements = paiementRepository.findByFactureId(factureId);
        model.addAttribute("paiements", paiements);
        model.addAttribute("factureId", factureId);
        return "list-paiements";
    }


    @GetMapping("/add-paiement/{facture_id}")
    public String addPaiementForm(@PathVariable("facture_id") Long factureId, Model model) {
        Paiement paiement = new Paiement();
        paiement.setDate(paiement.getDate()); // Valeur par défaut pour la date
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id: " + factureId));
        paiement.setFacture(facture);

        model.addAttribute("paiement", paiement);
        model.addAttribute("factureId", factureId); // Passer l'ID au formulaire
        return "add-paiement";
    }


    @Transactional
    @PostMapping("/add-paiement/{facture_id}")
    @ResponseBody // Returns a JSON response instead of rendering a view
    public ResponseEntity<String> addPaiementJson(
            @PathVariable("facture_id") Long factureId,
            @RequestBody @Valid Paiement paiement, // Handles JSON input
            BindingResult result) {

        logger.info("Adding payment for facture ID: {}", factureId);
        logger.info("Paiement object received: {}", paiement);

        // Check for validation errors
        if (result.hasErrors()) {
            logger.error("Validation errors occurred: {}", result.getAllErrors());
            return ResponseEntity.badRequest().body("Validation failed: " + result.getAllErrors());
        }

        // Retrieve the associated Facture
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id: " + factureId));
        paiement.setFacture(facture);

        // Save the Paiement
        paiementRepository.save(paiement);
        logger.info("Paiement saved successfully: {}", paiement);

        // Update the Facture status
        updateFactureStatus(factureId);

        // Return a success response
        return ResponseEntity.ok("Paiement added successfully.");
    }


    @GetMapping("/update-paiement/{id}")
    public String showUpdateForm(@PathVariable("id") long id, Model model) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paiement Id: " + id));
        model.addAttribute("paiement", paiement);
        return "update-paiement"; // Renders the HTML template
    }

    @Transactional
    @PatchMapping("/update-paiement/{id}")
    @ResponseBody
    public ResponseEntity<String> updatePaiementPartial(
            @PathVariable("id") long id,
            @RequestBody Paiement updatedPaiement) {

        logger.info("Partially updating payment with ID: {}", id);

        // Check if the paiement exists
        Paiement existingPaiement = paiementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paiement Id: " + id));

        // Update only the provided fields
        if (updatedPaiement.getDate() != null) {
            logger.info("Updating date: {}", updatedPaiement.getDate());
            existingPaiement.setDate(updatedPaiement.getDate());
        }
        if (updatedPaiement.getMontant() != null) {
            logger.info("Updating montant: {}", updatedPaiement.getMontant());
            existingPaiement.setMontant(updatedPaiement.getMontant());
        }

        // Save updated paiement
        paiementRepository.save(existingPaiement);
        logger.info("Paiement updated successfully: {}", existingPaiement);

        // Update facture status
        updateFactureStatus(existingPaiement.getFacture().getId());

        return ResponseEntity.ok("Paiement partially updated successfully.");
    }


    @DeleteMapping("/delete-paiement/{id}")
    public ResponseEntity<String> deletePaiement(@PathVariable("id") Long id) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paiement Id: " + id));
        Long factureId = paiement.getFacture().getId();
        paiementRepository.delete(paiement);
        updateFactureStatus(factureId);
        logger.info("Payment deleted successfully: {}", paiement);
        return ResponseEntity.ok("Paiement deleted successfully.");
    }


    private void updateFactureStatus(Long factureId) {
        List<Paiement> paiements = paiementRepository.findByFactureId(factureId);
        double montantPaye = paiements.stream().mapToDouble(Paiement::getMontant).sum();

        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id: " + factureId));

        double montantRestant = facture.getMontant() - montantPaye;
        facture.setMontantRestant(montantRestant);

        facture.setEtat(montantRestant == 0 ? "Paid" : "Unpaid");
        factureRepository.save(facture);
        logger.info("Facture status updated for facture ID: {} - Remaining: {}, Status: {}",
                factureId, montantRestant, facture.getEtat());
    }



    @GetMapping("/paiements-categories")
    public ResponseEntity<List<PaymentCategorySummary>> getPaiementsByCategoryForCurrentMonth(Principal principal) {
        if (principal == null) {
            logger.error("User is not authenticated");
            throw new IllegalStateException("User is not authenticated");
        }


        String username = principal.getName();
        logger.debug("Authenticated username: {}", username);

        LocalDate now = LocalDate.now();
        Month currentMonth = now.getMonth();
        LocalDate startDate= LocalDate.of(now.getYear(), currentMonth, 1);
        LocalDate endDate= LocalDate.of(now.getYear(), currentMonth, now.lengthOfMonth());
        logger.debug("Fetching paiements for user: {}, startDate: {}, endDate: {}", username, startDate, endDate);

        // Fetch payments for the current user and current month
        List<Paiement> paiements = paiementRepository.findByUserAndDateBetween(
                username,
                startDate,
                 endDate
        );

        // Group paiements by category and calculate the total and remaining amounts
        Map<String, PaymentCategorySummary> categorySummaryMap = new HashMap<>();

        for (Paiement paiement : paiements) {
            String category = paiement.getFacture().getCategorie();
            double amountPaid = paiement.getMontant();
            double totalAmountForCategory = paiement.getFacture().getMontant();

            categorySummaryMap.putIfAbsent(category, new PaymentCategorySummary(category));
            PaymentCategorySummary categorySummary = categorySummaryMap.get(category);

            categorySummary.addPaidAmount(amountPaid);

            if (categorySummary.getTotalAmount() == 0) {
                categorySummary.setTotalAmount(totalAmountForCategory);
            }

            double remainingAmount = categorySummary.getTotalAmount() - categorySummary.getPaidAmount();
            categorySummary.setRemainingAmount(remainingAmount);
        }

        List<PaymentCategorySummary> categorySummaries = new ArrayList<>(categorySummaryMap.values());

        // Return the data as JSON
        return ResponseEntity.ok(categorySummaries);
    }



}