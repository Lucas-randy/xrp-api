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

    /*Route pour générer un wallet + créer une paire de clés + cryptage
    * curl.exe http://localhost:8080/stellar/create

     * */
    @GetMapping("/create")
    public String createAccount() throws Exception {
        // ➜ Crée une paire de clés + chiffre + stocke + active Friendbot
        Wallet wallet = stellarService.createAndStoreWallet();

        return "✅ New Stellar wallet created & funded!\n"
                + "Public Key: " + wallet.getPublicKey() + "\n"
                + "(Encrypted secret stored securely)";
    }

    /*Route pour décrypter une adresse publique
    * curl.exe "http://localhost:8080/stellar/secret/GA2SVBHMBZOVHHPFIRA4577CMKOYTL5OJWNEMDVEXXQ7KUTHVL7VTGE5"
     * */
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
     * Exemple : curl.exe -X POST "http://localhost:8080/stellar/send?fromPublicKey=GBT4MX7QU2DB7CFPCUQZ5GKCVO4EQ3U5IL3EXPGRNF2XZUFD4SBDHHDG&
     * toPublicKey=GA2SVBHMBZOVHHPFIRA4577CMKOYTL5OJWNEMDVEXXQ7KUTHVL7VTGE5&amount=50"
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

    @PostMapping("/swap")
    public String swapXLMToUSDC(
            @RequestParam String fromPublicKey,
            @RequestParam String toPublicKey,
            @RequestParam String amount) {

        try {
            System.out.println("🔄 Reçu swap : from=" + fromPublicKey + " to=" + toPublicKey + " amount=" + amount);
            String result = stellarService.swapXLMtoUSDC(fromPublicKey, toPublicKey, amount);
            System.out.println("✅ Résultat du swap : " + result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Erreur swap : " + e.getMessage();
        }
    }

    @PostMapping("/trustline/usdc")
    public String addTrustlineUSDC(@RequestParam String publicKey) {
        try {
            return stellarService.createTrustLineUSDC(publicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Erreur : " + e.getMessage();
        }
    }



}

