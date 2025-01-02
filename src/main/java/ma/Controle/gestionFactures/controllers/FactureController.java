package ma.Controle.gestionFactures.controllers;

import jakarta.validation.Valid;
import ma.Controle.gestionFactures.entities.Facture;
import ma.Controle.gestionFactures.repositories.FactureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class FactureController {


    @Autowired
    private FactureRepository factureRepository;

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
    public String showListFacture(Model model) {
        model.addAttribute("factures", factureRepository.findAll());

        return "index";
    }

    @GetMapping("/addfacture")
    public String showAddFacturePage(Model model) {
        model.addAttribute("facture", new Facture());  // Exemple d'objet attendu dans le template
        return "addfacture";
    }

    @PostMapping("/addfacture")
    public String addFacture(@Valid Facture facture, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "addfacture";
        }

        factureRepository.save(facture);
        model.addAttribute("factures", factureRepository.findAll());
        return "index";

    }


    @GetMapping("/update/{id}")
    public String showUpdateForm(@PathVariable("id") long id, Model model) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id:" + id));
        model.addAttribute("facture", facture);
        return "update-facture";
    }

    @PostMapping("/update/{id}")
    public String updateFacture(@PathVariable("id") long id, @Valid Facture facture, BindingResult result, Model model) {
        if (result.hasErrors()) {
            facture.setId(id);
            return "update-facture";
        }

        factureRepository.save(facture);
        model.addAttribute("factures", factureRepository.findAll());
        return "index";
    }

    @GetMapping("/delete/{id}")
    public String deleteFacture(@PathVariable("id") long id, Model model) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid facture Id:" + id));
        factureRepository.delete(facture);
        model.addAttribute("factures", factureRepository.findAll());
        return "index";
    }


}
