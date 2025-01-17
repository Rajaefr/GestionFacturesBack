package ma.Controle.gestionFactures.controllers;
import java.text.ParseException;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;

import jakarta.validation.Valid;
import ma.Controle.gestionFactures.dto.FactureDto;
import ma.Controle.gestionFactures.dto.PaiementDto;
import ma.Controle.gestionFactures.entities.Facture;
import ma.Controle.gestionFactures.entities.Paiement;
import ma.Controle.gestionFactures.entities.UserEntity;
import ma.Controle.gestionFactures.repositories.FactureRepository;
import ma.Controle.gestionFactures.repositories.PaiementRepository;
import ma.Controle.gestionFactures.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;



@Controller
public class FactureController {

    private static final Logger logger = LoggerFactory.getLogger(FactureController.class);

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaiementRepository paiementRepository;


    @CrossOrigin(origins = "http://localhost:3000")


    @GetMapping("/")
    public String accueil() {
        return "accueil";
    }

    @GetMapping("/search")
    public String searchFactures(@RequestParam("categorie") String categorie, Model model) {
        List<Facture> factures = factureRepository.findByCategorieContainingIgnoreCase(categorie);
        model.addAttribute("factures", factures);
        return "index";
    }

    @GetMapping("/api/factures")
    public ResponseEntity<List<FactureDto>> showListFacture(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {

        if (principal == null) {
            logger.error("No authenticated user found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // Unauthorized
        }

        String username = principal.getName();
        List<Facture> factures = factureRepository.findByUser_Username(username);

        // Convert to FactureDto with payments
        List<FactureDto> facturesDTO = toFactureDTOs(factures);

        // Pagination logic
        int start = page * size;
        int end = Math.min(start + size, facturesDTO.size());

        return start >= facturesDTO.size()
                ? ResponseEntity.ok(Collections.emptyList())
                : ResponseEntity.ok(facturesDTO.subList(start, end));
    }


    @PostMapping("/api/add-facture")
    public ResponseEntity<String> addFacture(
            @RequestBody @Valid FactureDto factureDto,
            BindingResult result,
            Principal principal) {

        if (principal == null) {
            logger.error("No authenticated user found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        if (result.hasErrors()) {
            logger.error("Validation errors: {}", result.getAllErrors());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Validation errors");
        }

        try {
            // Retrieve the user associated with the request
            String username = principal.getName();
            Optional<UserEntity> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                logger.error("User not found: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            UserEntity user = userOpt.get();

            // Create a new Facture entity from the DTO
            Facture facture = new Facture();
            facture.setDescription(factureDto.getDescription());
            facture.setCategorie(factureDto.getCategorie());
            facture.setMontant(factureDto.getMontant());
            facture.setDate(java.sql.Date.valueOf(factureDto.getDate()));
            facture.setEtat(factureDto.getEtat());
////        facture.setMontantRestant(factureDto.getMontant()); // Adjust this logic if needed
            facture.setUser(user);
            // Vérifiez l'état et ajustez le montant restant
            if ("Paid".equalsIgnoreCase(factureDto.getEtat())) {
                facture.setMontantRestant(0.0);
            } else if ("Unpaid".equalsIgnoreCase(factureDto.getEtat())) {
                facture.setMontantRestant(factureDto.getMontant()); // Sinon, montant restant = montant total
            }

            // Save the facture to the database
            factureRepository.save(facture);
            logger.info("Facture added successfully: {}", facture);

            return ResponseEntity.status(HttpStatus.CREATED).body("Facture added successfully");
        } catch (Exception e) {
            logger.error("Error while adding facture: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while adding facture");
        }
    }










    @PutMapping("/update/{id}")
    public ResponseEntity<Facture> updateFacture(@PathVariable("id") long id, @RequestBody Map<String, Object> updates, BindingResult result) {
        if (result.hasErrors()) {
            logger.error("Validation errors while updating facture: {}", result.getAllErrors());
            return ResponseEntity.badRequest().build();  // Return validation errors
        }

        // Retrieve the existing Facture
        Facture existingFacture = factureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Facture not found"));

        // Update the fields if present in the request
        if (updates.containsKey("date")) {
            String dateStr = (String) updates.get("date");
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = dateFormat.parse(dateStr);
                existingFacture.setDate(date);  // Set the updated date
            } catch (ParseException e) {
                logger.error("Error while converting date", e);
                return ResponseEntity.badRequest().build();  // In case of date format error
            }
        }

        if (updates.containsKey("categorie")) {
            existingFacture.setCategorie((String) updates.get("categorie"));
        }
        if (updates.containsKey("montant")) {
            existingFacture.setMontant((Double) updates.get("montant"));
        }
        if (updates.containsKey("montantRestant")) {
            Double montantRestant = (Double) updates.get("montantRestant");
            existingFacture.setMontantRestant(montantRestant);
        }
        if (updates.containsKey("etat")) {
            String etat = (String) updates.get("etat");
            existingFacture.setEtat(etat);  // Ensure consistency based on the state
        }

        // After all updates, ensure montantRestant and etat are recalculated
        existingFacture.updateMontantRestantAndEtat();  // Helper method to update these fields based on payments

        // Save the updated facture to the database
        factureRepository.save(existingFacture);
        logger.info("Facture updated: {}", existingFacture);

        return ResponseEntity.ok(existingFacture);  // Return the updated facture
    }









    @GetMapping("/addfacture")
    public String showAddFacturePage(Model model) {
        model.addAttribute("facture", new Facture());
        model.addAttribute("categories", List.of("Transportation", "Housing", "Healthcare", "Obligation")); // Adjust categories as needed
        return "addfacture";
    }


    @GetMapping("/update/{id}")
    public ResponseEntity<Facture> getFactureDetails(@PathVariable("id") long id) {
        logger.info("Received request to fetch Facture with ID: {}", id);

        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Facture with ID {} not found", id);
                    throw new IllegalArgumentException("Invalid Facture ID: " + id);
                });

        logger.info("Facture retrieved successfully: {}", facture);
        return ResponseEntity.ok(facture);  // Return the facture as JSON
    }







    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFacture(@PathVariable("id") long id) {
        // Retrieve the facture to be deleted
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id: " + id));

        // Perform the delete operation
        factureRepository.delete(facture);
        logger.info("Facture deleted: {}", facture);

        // Return a success response with a message
        return ResponseEntity.ok("Facture deleted successfully");
    }


    @GetMapping("/api/factures-echeance-proche")
    public ResponseEntity<List<FactureDto>> getFacturesEcheanceProche(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {

        if (principal == null) {
            logger.error("User is not authenticated");
            throw new IllegalStateException("User is not authenticated");
        }

        String username = principal.getName();
        LocalDate currentDate = LocalDate.now();
        LocalDate thresholdDate = currentDate.plusDays(7);

        // Retrieve invoices for the authenticated user
        List<Facture> userFactures = factureRepository.findByUser_Username(username);

        // Filter and map to DTOs
        List<FactureDto> facturesDTO = toFactureDTOs(
                userFactures.stream()
                        .filter(facture -> {
                            Date dateEcheance = facture.getDate();
                            if (dateEcheance == null) {
                                return false;
                            }
                            LocalDate dateEcheanceLocal = new java.sql.Date(dateEcheance.getTime()).toLocalDate();
                            return !dateEcheanceLocal.isBefore(currentDate) && dateEcheanceLocal.isBefore(thresholdDate);
                        })
                        .collect(Collectors.toList())
        );

        // Pagination logic
        int start = page * size;
        int end = Math.min(start + size, facturesDTO.size());
        return start >= facturesDTO.size()
                ? ResponseEntity.ok(Collections.emptyList())
                : ResponseEntity.ok(facturesDTO.subList(start, end));
    }

    public List<FactureDto> toFactureDTOs(List<Facture> factures) {
        return factures.stream().map(facture -> {
            FactureDto dto = new FactureDto();
            dto.setId(facture.getId());
            dto.setCategorie(facture.getCategorie());
            dto.setDescription(facture.getDescription());
            dto.setMontant(facture.getMontant());

            if (facture.getDate() != null) {
                dto.setDate(new java.sql.Date(facture.getDate().getTime()).toLocalDate());
            }

            dto.setMontantRestant(facture.getMontantRestant());
            dto.setEtat(facture.getEtat());

            dto.setPaiements(facture.getPaiements().stream()
                    .map(paiement -> {
                        PaiementDto paiementDTO = new PaiementDto();
                        paiementDTO.setId(paiement.getId());
                        paiementDTO.setDate(paiement.getDate());
                        paiementDTO.setMontant(paiement.getMontant());
                        return paiementDTO;
                    }).collect(Collectors.toList()));

            return dto;
        }).collect(Collectors.toList());
    }

}


