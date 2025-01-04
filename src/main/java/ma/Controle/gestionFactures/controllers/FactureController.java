package ma.Controle.gestionFactures.controllers;

import jakarta.validation.Valid;
import ma.Controle.gestionFactures.entities.Facture;
import ma.Controle.gestionFactures.entities.UserEntity;
import ma.Controle.gestionFactures.repositories.FactureRepository;
import ma.Controle.gestionFactures.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
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

    @GetMapping("/factures")
    public String showListFacture(Model model, Principal principal) {
        if (principal == null) {
            logger.error("No authenticated user found");
            throw new IllegalStateException("User is not authenticated");
        }

        String username = principal.getName();
        List<Facture> factures = factureRepository.findByUser_Username(username);
        model.addAttribute("factures", factures);
        return "index";
    }

    @GetMapping("/addfacture")
    public String showAddFacturePage(Model model) {
        model.addAttribute("facture", new Facture());
        return "addfacture";
    }

    @PostMapping("/addfacture")
    public String addFacture(@Valid @ModelAttribute Facture facture, BindingResult result, Principal principal, Model model) {
        logger.info("Received request to add facture: {}", facture);
        logger.info("Facture received: {}", facture);
        logger.info("Facture date: {}", facture.getDate());
        logger.info("Facture object sent to template: {}", model.getAttribute("facture"));


        if (result.hasErrors()) {
            logger.error("Facture validation errors: {}", result.getAllErrors());
            model.addAttribute("facture", facture);
            return "addfacture";
        }

        if (principal == null) {
            logger.error("User is not authenticated");
            throw new IllegalStateException("User is not authenticated");
        }

        String username = principal.getName();
        Optional<UserEntity> user = userRepository.findByUsername(username);
        if (!user.isPresent()) {
            logger.error("User not found: {}", username);
            throw new IllegalStateException("User not found");
        }

        facture.setUser(user.get());
        logger.info("Facture details before saving: {}", facture);

        factureRepository.save(facture);
        logger.info("Facture successfully saved");

        model.addAttribute("factures", factureRepository.findByUser_Username(username));
        return "redirect:/factures";
    }

    @GetMapping("/update/{id}")
    public String showUpdateForm(@PathVariable("id") long id, Model model) {
        logger.info("Received request to update Facture with ID: {}", id);

        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Facture with ID {} not found", id);
                    return new IllegalArgumentException("Invalid Facture ID: " + id);
                });

        logger.info("Facture retrieved successfully: {}", facture);
        model.addAttribute("facture", facture);
        return "update-facture";
    }


    @PostMapping("/update/{id}")
    public String updateFacture(@PathVariable("id") long id, @Valid Facture facture, BindingResult result, Model model) {
        if (result.hasErrors()) {
            facture.setId(id);
            logger.error("Validation errors while updating facture: {}", result.getAllErrors());
            return "update-facture";
        }

        factureRepository.save(facture);
        logger.info("Facture updated: {}", facture);

        model.addAttribute("factures", factureRepository.findByUser_Username(facture.getUser().getUsername()));
        return "redirect:/factures";
    }

    @GetMapping("/delete/{id}")
    public String deleteFacture(@PathVariable("id") long id, Model model) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id: " + id));

        factureRepository.delete(facture);
        logger.info("Facture deleted: {}", facture);

        model.addAttribute("factures", factureRepository.findByUser_Username(facture.getUser().getUsername()));
        return "redirect:/factures";
    }@GetMapping("/factures-echeance-proche")
    public String getFacturesEcheanceProche(Model model) {
        // Current date and threshold date (7 days ahead)
        LocalDate currentDate = LocalDate.now();
        LocalDate thresholdDate = currentDate.plusDays(7); // 7 days from now

        // Retrieve all invoices from the repository
        List<Facture> allFactures = factureRepository.findAll();

        // Filter the invoices based on the upcoming due date
        List<Facture> facturesEcheanceProche = allFactures.stream()
                .filter(facture -> {
                    Date dateEcheance = facture.getDate(); // Get the due date

                    // Skip if the due date is null
                    if (dateEcheance == null) {
                        return false;
                    }

                    // Convert java.sql.Date to java.util.Date
                    java.util.Date utilDate = new java.util.Date(dateEcheance.getTime());

                    // Convert java.util.Date to LocalDate
                    LocalDate dateEcheanceLocal = utilDate.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    // Check if the due date is within the range (from today to 7 days ahead)
                    return !dateEcheanceLocal.isBefore(currentDate) && dateEcheanceLocal.isBefore(thresholdDate);
                })
                .collect(Collectors.toList());

        model.addAttribute("facturesEcheanceProche", facturesEcheanceProche);

        return "factures-echeance-proche"; // Return the view
    }


}


