package com.example.xrpapi.controller;

import com.example.xrpapi.entity.Wallet;
import com.example.xrpapi.service.StellarService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stellar")
public class StellarController {

    private final StellarService stellarService;

    public StellarController(StellarService stellarService) {
        this.stellarService = stellarService;
    }

    //Route pour générer un wallet + créer une paire de clés + cryptage
    @GetMapping("/create")
    public String createAccount() throws Exception {
        // ➜ Crée une paire de clés + chiffre + stocke + active Friendbot
        Wallet wallet = stellarService.createAndStoreWallet();

        return "✅ New Stellar wallet created & funded!\n"
                + "Public Key: " + wallet.getPublicKey() + "\n"
                + "(Encrypted secret stored securely)";
    }

    //Route pour décrypter une adresse publique
    @GetMapping("/secret/{publicKey}")
    public String getDecrypted(@PathVariable String publicKey) throws Exception {
        String seed = stellarService.getSecret(publicKey);
        return "🔑 Decrypted Seed for " + publicKey + " : " + seed;
    }

    //Route pour connaître la balance
    @GetMapping("/balance/{publicKey}")
    public String checkBalance(@PathVariable String publicKey) throws Exception {
        return stellarService.checkBalance(publicKey);
    }

    //Route pour afficher l'historique
    @GetMapping("/history/{publicKey}")
    public String getPaymentHistory(@PathVariable String publicKey) throws Exception {
        return stellarService.getPaymentHistory(publicKey);
    }

    /**
     * ✅ Route POST : Envoi de XLM
     * Exemple : POST http://localhost:8080/stellar/send?fromPublicKey=...&toPublicKey=...&amount=...
     */
    @PostMapping("/send")
    public String sendXLM(
            @RequestParam String fromPublicKey,
            @RequestParam String toPublicKey,
            @RequestParam String amount) {

        try {
            System.out.println("💡 Reçu : from=" + fromPublicKey + " to=" + toPublicKey + " amount=" + amount);
            String result = stellarService.sendXLM(fromPublicKey, toPublicKey, amount);
            System.out.println("✅ Résultat du sendXLM : " + result);
            return result;

        } catch (Exception e) {
            e.printStackTrace(); // ✅ LOG l'exception dans le terminal
            return "❌ Erreur : " + e.getMessage();
        }
    }
}

