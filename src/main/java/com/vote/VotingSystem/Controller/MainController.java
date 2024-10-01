package com.vote.VotingSystem.Controller;

import com.vote.VotingSystem.Dto.UserDto;
import com.vote.VotingSystem.Model.Candidate;
import com.vote.VotingSystem.Model.Gender;
import com.vote.VotingSystem.Model.User;
import com.vote.VotingSystem.Repository.CandidateRepository;
import com.vote.VotingSystem.Service.CandidateService;
import com.vote.VotingSystem.Service.FlutterwaveService;
import com.vote.VotingSystem.Service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class MainController {

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    UserService userService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private FlutterwaveService flutterwaveService;

    @Autowired
    private CandidateService candidateService;
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    /*Afficher la page de vote avec les candidate creer*/
    @GetMapping("/vote")
    public String showVotePage(Model model) {
        List<Candidate> candidates = candidateService.getAllCandidates();
        int totalVotes = candidates.stream().mapToInt(Candidate::getVoteCount).sum();

        // Calculer le pourcentage pour chaque candidat
        candidates.forEach(candidate -> {
            double percentage = totalVotes > 0 ?
                    (double) candidate.getVoteCount() / totalVotes * 100 : 0;
            candidate.setVotePercentage(Math.round(percentage * 100.0) / 100.0); // Arrondir à 2 décimales
        });

        model.addAttribute("candidates", candidates);
        return "vote";
    }

    /*Affiche la page de l'admin*/
    @GetMapping("/admin-page")
    @PreAuthorize("hasRole('ADMIN')")
    public String showDashboard(Model model) {
        List<Candidate> candidates = candidateService.getAllCandidates();

        // Calculs existants
        model.addAttribute("candidates", candidates);
        model.addAttribute("candidateCount", candidates.size());
        int totalVotes = candidates.stream().mapToInt(Candidate::getVoteCount).sum();
        model.addAttribute("totalVotes", totalVotes);

        //gain total apres chaque vote
        long totalGain = candidateService.getTotalGain();
        model.addAttribute("totalGain", totalGain);

        //obtenir les candidat en tete par genre
        Candidate topMaleCandidate = candidateService.getTopCandidateByGender(Gender.HOMME);
        Candidate topFemaleCandidate = candidateService.getTopCandidateByGender(Gender.FEMME);

        model.addAttribute("topMaleCandidate", topMaleCandidate);
        model.addAttribute("topFemaleCandidate", topFemaleCandidate);

        // Nouveaux calculs pour les statistiques par genre
        List<Candidate> hommes = candidates.stream().filter(c -> c.getGender() == Gender.HOMME).collect(Collectors.toList());
        List<Candidate> femmes = candidates.stream().filter(c -> c.getGender() == Gender.FEMME).collect(Collectors.toList());

        model.addAttribute("hommeCount", hommes.size());
        model.addAttribute("femmeCount", femmes.size());

        int totalVotesHommes = hommes.stream().mapToInt(Candidate::getVoteCount).sum();
        int totalVotesFemmes = femmes.stream().mapToInt(Candidate::getVoteCount).sum();

        model.addAttribute("totalVotesHommes", totalVotesHommes);
        model.addAttribute("totalVotesFemmes", totalVotesFemmes);

        // Vous pouvez également ajouter ces listes si vous voulez les utiliser directement dans la vue
        model.addAttribute("candidatsHommes", hommes);
        model.addAttribute("candidatsFemmes", femmes);

        return "admin";
    }

    /*Liste des genres*/
    @ModelAttribute("genders")
    public List<Gender> getGenders() {
        return Arrays.asList(Gender.values());
    }

    /*Afficher le formulaire de creation de candidate*/
    @GetMapping("/candidates/add")
    public String showCandidateForm(Model model) {
        model.addAttribute("candidate", new Candidate());
        return "add-candidate";
    }

    /*Methode pour pour enregistrer un candidat creer avec son photo*/
    @PostMapping("/candidates")
    public String saveCandidate(@ModelAttribute Candidate candidate,
                                @RequestParam("file") MultipartFile file) throws IOException {
        if (!file.isEmpty()) {
            candidate.setPhoto(file.getBytes());
        }
        candidateService.saveCandidate(candidate);
        return "redirect:/admin-page";
    }

    /*Methode pour upload une image du pc*/
    @GetMapping("/candidate/image/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getCandidateImage(@PathVariable Long id) {
        Candidate candidate = candidateService.getCandidateById(id);
        byte[] imageContent = candidate.getPhoto();
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(imageContent, headers, HttpStatus.OK);
    }

    //Votecount methode
    @PostMapping("/vote/{id}")
    public ResponseEntity<?> vote(@PathVariable Long id, @RequestBody Map<String, String> paymentDetails) {
        try {
            String paymentLink = flutterwaveService.initiatePayment(
                    id.toString(),
                    100.0, // Montant fixe de 100 FCFA
                    paymentDetails.get("email"),
                    paymentDetails.get("name"),
                    paymentDetails.get("phoneNumber"),
                    "https://votre-site.com/payment-callback",
                    paymentDetails.get("paymentMethod")
            );
            if (paymentLink != null) {
                return ResponseEntity.ok(Map.of("paymentLink", paymentLink));
            } else {
                return ResponseEntity.badRequest().body("Erreur lors de l'initialisation du paiement");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'initialisation du paiement : " + e.getMessage());
        }
    }

    @GetMapping("/payment-callback")
    public String handlePaymentCallback(@RequestParam String transaction_id, @RequestParam String status) {
        try {
            if ("successful".equals(status) && flutterwaveService.verifyTransaction(transaction_id)) {

                String candidateId = extractCandidateIdFromTransaction(transaction_id);
                candidateService.incrementVoteCount(Long.parseLong(candidateId));
                return "redirect:/vote?success=true";
            } else {
                return "redirect:/vote?success=false";
            }
        } catch (Exception e) {
            return "redirect:/vote?success=false&error=" + e.getMessage();
        }
    }

    // Méthode pour extraire l'ID du candidat du tx_ref
    private String extractCandidateIdFromTransaction(String transactionId) {

        String[] parts = transactionId.split("_");
        if (parts.length >= 2) {
            return parts[1]; // Return the candidateId part
        }
        throw new IllegalArgumentException("Invalid transaction ID format");
    }

    /*//initier le paiement
    @PostMapping("/initiate-payment")
    @ResponseBody
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, String> paymentDetails) {
        try {
            String paymentLink = flutterwaveService.initializePayment(
                    paymentDetails.get("amount"),
                    paymentDetails.get("email"),
                    paymentDetails.get("phoneNumber"),
                    paymentDetails.get("name"),
                    paymentDetails.get("paymentMethod")
            );
            return ResponseEntity.ok(Map.of("paymentLink", paymentLink));
        } catch (RaveException e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'initialisation du paiement : " + e.getMessage());
        }
    }*/

    //Update form
    @GetMapping("/candidate/edit/{id}")
    public String showUpdateForm(@PathVariable("id") long id, Model model) {
        Candidate candidate = candidateService.getCandidateById(id);
        model.addAttribute("candidate", candidate);
        return "edit-candidate";
    }

    //update a candidate
    @PostMapping("/candidate/edit/{id}")
    public String updateCandidate(@PathVariable("id") long id,
                                  @ModelAttribute Candidate candidate,
                                  @RequestParam("file") MultipartFile file) throws IOException {
        Candidate existingCandidate = candidateService.getCandidateById(id);
        existingCandidate.setFullname(candidate.getFullname());
        existingCandidate.setVoteNumber(candidate.getVoteNumber());
        existingCandidate.setGender(candidate.getGender()); // Ajoutez cette ligne

        if (!file.isEmpty()) {
            existingCandidate.setPhoto(file.getBytes());
        }

        candidateService.saveCandidate(existingCandidate);
        return "redirect:/admin-page";
    }

    //Delete a candidate
    @GetMapping("/candidate/delete/{id}")
    public String deleteCandidate(@PathVariable("id") long id) {
        candidateService.deleteCandidate(id);
        return "redirect:/admin-page";
    }

    // handler method to handle user registration form request
    @GetMapping("/register")
    public String registerForm(Model model){
        UserDto user = new UserDto();
        model.addAttribute("user", user);
        return "register";
    }

    // handler method to handle user registration form submit request
    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") @Valid UserDto userDto,
                               BindingResult result,
                               Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register";
        }

        try {

            System.out.println("Username: " + userDto.getUsername());  // Log pour déboguer
            System.out.println("Password: " + userDto.getPassword());  // Log pour déboguer
            User createdUser = userService.registerNewUser(userDto);
            System.out.println("Created user: " + createdUser.getUsername());  // Log pour déboguer
            redirectAttributes.addFlashAttribute("registrationSuccess", "Inscription réussie. Veuillez vous connecter.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("registrationError", e.getMessage());
            return "register";
        }
    }

    // handler method to handle login request
    @GetMapping("/login")
    public String login(){
        return "login";
    }



}
