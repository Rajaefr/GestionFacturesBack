package ma.Controle.gestionFactures.controllers;


import jakarta.validation.Valid;
import ma.Controle.gestionFactures.dto.PaiementDto;
import ma.Controle.gestionFactures.entities.Facture;
import ma.Controle.gestionFactures.entities.Paiement;
import ma.Controle.gestionFactures.entities.PaymentCategorySummary;
import ma.Controle.gestionFactures.repositories.FactureRepository;
import ma.Controle.gestionFactures.repositories.PaiementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import java.util.*;
import java.security.Principal;
import java.util.stream.Collectors;


@Controller
public class PaiementController {

    private static final Logger logger = LoggerFactory.getLogger(PaiementController.class);

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private FactureRepository factureRepository;

    @CrossOrigin(origins = "http://localhost:3000")

    @GetMapping("/list-paiements/{factureId}")
    @ResponseBody
    public ResponseEntity<List<PaiementDto>> listPaiements(@PathVariable Long factureId) {
        // Check if the facture exists
        Optional<Facture> factureOpt = factureRepository.findById(factureId);
        if (factureOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.emptyList()); // Return empty list if facture not found
        }

        // Retrieve the list of payments associated with the facture ID
        List<Paiement> paiements = paiementRepository.findByFactureId(factureId);

        // Map to DTOs to prevent exposing internal entity structure directly
        List<PaiementDto> paiementDtos = paiements.stream()
                .map(paiement -> {
                    PaiementDto dto = new PaiementDto();
                    dto.setId(paiement.getId());
                    dto.setDate(paiement.getDate());
                    dto.setMontant(paiement.getMontant());
                    return dto;
                })
                .collect(Collectors.toList());

        // Return the list of payments as JSON
        return ResponseEntity.ok(paiementDtos);
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
    @ResponseBody
    public ResponseEntity<String> addPaiementJson(
            @PathVariable("facture_id") Long factureId,
            @RequestBody @Valid Paiement paiement,
            BindingResult result) {

        logger.info("Adding payment for facture ID: {}", factureId);
        logger.info("Paiement object received: {}", paiement);

        // Check for validation errors
        if (result.hasErrors()) {
            logger.error("Validation errors occurred: {}", result.getAllErrors());
            return ResponseEntity.badRequest().body("Validation failed: " + result.getAllErrors());
        }

        // Retrieve the associated invoice
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture ID: " + factureId));

        // Validate if payment can be added
        if ("Paid".equalsIgnoreCase(facture.getEtat())) {
            logger.warn("Cannot add payment: Facture is already fully paid. Facture ID: {}", factureId);
            return ResponseEntity.badRequest().body("Cannot add payment: Facture is already fully paid.");
        }

        // Ensure there is a remaining amount on the invoice
        if (facture.getMontantRestant() <= 0) {
            logger.warn("Cannot add payment: No remaining amount for this facture. Facture ID: {}", factureId);
            return ResponseEntity.badRequest().body("Cannot add payment: No remaining amount for this facture.");
        }

        // Check if the payment amount exceeds the remaining amount
        if (paiement.getMontant() > facture.getMontantRestant()) {
            logger.warn("Cannot add payment: Payment exceeds remaining amount. Facture ID: {}", factureId);
            return ResponseEntity.badRequest().body("Payment amount exceeds the remaining balance.");
        }

        // Associate payment with the invoice
        paiement.setFacture(facture);

        // Save the payment
        paiementRepository.save(paiement);

        // Update the remaining amount after payment
        double montantRestant = facture.getMontantRestant() - paiement.getMontant();
        facture.setMontantRestant(montantRestant);

        // If the remaining amount is 0 or less, mark the invoice as paid
        if (montantRestant <= 0) {
            facture.setEtat("Paid");
        } else {
            facture.setEtat("Unpaid");
        }

        // Save the updated invoice
        factureRepository.save(facture);

        logger.info("Paiement saved successfully: {}", paiement);
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
        // Retrieve the facture
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture ID: " + factureId));

        // Retrieve payments and calculate total paid amount
        double montantPaye = paiementRepository.findByFactureId(factureId).stream()
                .mapToDouble(Paiement::getMontant)
                .sum();

        // Calculate remaining amount
        Double montantFacture = facture.getMontant();
        if (montantFacture == null || montantFacture <= 0) {
            throw new IllegalStateException("Facture montant must be greater than zero for ID: " + factureId);
        }

        double montantRestant = montantFacture - montantPaye;

        // Ensure montantRestant is not negative
        if (montantRestant < 0) {
            montantRestant = 0; // Set to 0 if it's negative
            logger.warn("Montant restant for facture ID {} was negative. Setting it to 0.", factureId);
        }

        // Update facture fields
        facture.setMontantRestant(montantRestant);

        // Set the correct 'etat' based on the montantRestant
        if (montantRestant <= 0) {
            facture.setEtat("Paid");
        } else {
            facture.setEtat("Unpaid");
        }

        // Save updated facture
        factureRepository.save(facture);

        // Log updated status
        logger.info("Facture status updated for facture ID: {} - Remaining: {}, Status: {}",
                factureId, montantRestant, facture.getEtat());
    }


    @GetMapping("/api/paiements-categories")
    public ResponseEntity<List<PaymentCategorySummary>> getPaiementsByCategoryForCurrentMonth(Principal principal) {
        if (principal == null) {
            logger.error("User is not authenticated");
            throw new IllegalStateException("User is not authenticated");
        }


        String username = principal.getName();
        logger.debug("Authenticated username: {}", username);

        LocalDate now = LocalDate.now();
        Month currentMonth = now.getMonth();
        LocalDate startDate = LocalDate.of(now.getYear(), currentMonth, 1);
        LocalDate endDate = LocalDate.of(now.getYear(), currentMonth, now.lengthOfMonth());
        logger.debug("Fetching paiements for user: {}, startDate: {}, endDate: {}", username, startDate, endDate);

        // Fetch payments for the current user and current month
        List<Paiement> paiements = paiementRepository.findByUserAndDateBetween(
                username,
                startDate,
                endDate
        );

        // Group paiements by category and calculate the total and remaining amounts
        Map<String, PaymentCategorySummary> categorySummaryMap = new HashMap<>();

        String category = null;
        PaymentCategorySummary categorySummary = null;
        for (Paiement paiement : paiements) {
            category = paiement.getFacture().getCategorie();
            double amountPaid = paiement.getMontant();
            double totalAmountForCategory = paiement.getFacture().getMontant();

            categorySummaryMap.putIfAbsent(category, new PaymentCategorySummary(category));
            categorySummary = categorySummaryMap.get(category);

            categorySummary.addPaidAmount(amountPaid);

            if (categorySummary.getTotalAmount() == 0) {
                categorySummary.setTotalAmount(totalAmountForCategory);
            }

            double remainingAmount = categorySummary.getTotalAmount() - categorySummary.getPaidAmount();
            categorySummary.setRemainingAmount(remainingAmount);
        }
        logger.debug("Category: {}, Paid Amount: {}, Total Amount: {}, Remaining Amount: {}",
                category, categorySummary.getPaidAmount(), categorySummary.getTotalAmount(), categorySummary.getRemainingAmount());

        List<PaymentCategorySummary> categorySummaries = new ArrayList<>(categorySummaryMap.values());

        // Return the data as JSON
        return ResponseEntity.ok(categorySummaries);
    }



}