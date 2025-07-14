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

    /*Route pour afficher la balance
     * curl.exe "http://localhost:8080/stellar/balance/GA2SVBHMBZOVHHPFIRA4577CMKOYTL5OJWNEMDVEXXQ7KUTHVL7VTGE5"
     * */
    @GetMapping("/balance/{publicKey}")
    public String checkBalance(@PathVariable String publicKey) throws Exception {
        return stellarService.checkBalance(publicKey);
    }

    /*Route pour afficher l'historique de la transaction
     * curl.exe "http://localhost:8080/stellar/history/GA2SVBHMBZOVHHPFIRA4577CMKOYTL5OJWNEMDVEXXQ7KUTHVL7VTGE5"
     * */
    @GetMapping("/history/{publicKey}")
    public String getPaymentHistory(@PathVariable String publicKey) throws Exception {
        return stellarService.getPaymentHistory(publicKey);
    }

    /* ✅ Route POST : Envoi de XLM
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

    /* curl.exe -X POST "http://localhost:8080/stellar/swap?fromPublicKey=GC77VP2A7FIK2VUCOKRL7KU43PIBYQX4WRH2QOIKLXEFJSFNU7GYT3DL&
    toPublicKey=GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5&amount=20"
    ✅ SWAP XLM ➜ USDC OK (Tx Hash : c54e4e44d6e9c335c07a9dfaa9bf0da141b5230e6845e461cb5067fa4c8acbad)
    */
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

    /* ✅ Route POST : Trustline
     * Exemple : curl.exe -X POST "http://localhost:8080/stellar/trustline/usdc?publicKey=GC77VP2A7FIK2VUCOKRL7KU43PIBYQX4WRH2QOIKLXEFJSFNU7GYT3DL"
     * ✅ Trustline USDC ajoutée avec succès (issuer: GBT4MX7QU2DB7CFPCUQZ5GKCVO4EQ3U5IL3EXPGRNF2XZUFD4SBDHHDG)
     * Vérification sur https://stellar.expert/explorer/testnet/account/GC77VP2A7FIK2VUCOKRL7KU43PIBYQX4WRH2QOIKLXEFJSFNU7GYT3DL
     */
    @PostMapping("/trustline/usdc")
    public String addTrustlineUSDC(@RequestParam String publicKey) {
        try {
            return stellarService.createTrustLineUSDC(publicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Erreur : " + e.getMessage();
        }
    }

    /* curl.exe -X POST "http://localhost:8080/swap-path?fromPublicKey=GC77VP2A7FIK2VUCOKRL7KU43PIBYQX4WRH2QOIKLXEFJSFNU7GYT3DL&toPublicKey=GBT4MX7QU2DB7CFPCUQZ5GKCVO4EQ3U5IL3EXPGRNF2XZUFD4SBDHHDG&amountXLM=1&amountUSDC=0.000001"
    ✅ SWAP XLM ➜ USDC OK (Tx Hash : c54e4e44d6e9c335c07a9dfaa9bf0da141b5230e6845e461cb5067fa4c8acbad)
    */
    @PostMapping("/swap-path")
    public String swapXLMtoUSDC(
            @RequestParam String fromPublicKey,
            @RequestParam String toPublicKey,
            @RequestParam String amountXLM,
            @RequestParam String amountUSDC) throws Exception {

        return stellarService.swapXLMtoUSDC(fromPublicKey, toPublicKey, amountXLM, amountUSDC);
    }

    @PostMapping("/offer")
    public String createSellOfferUSDC(
            @RequestParam("publicKey") String publicKey,
            @RequestParam("amountXLM") String amountXLM,
            @RequestParam("price") String price) throws Exception {
        return stellarService.createSellOfferUSDC(publicKey, amountXLM, price);
    }

    @PostMapping("/issue")
    public String issueUSDC(@RequestParam("issuerPublicKey") String issuerPublicKey,
                            @RequestParam("toPublicKey") String toPublicKey,
                            @RequestParam("amount") String amount) throws Exception {
        return stellarService.issueUSDC(issuerPublicKey, toPublicKey, amount);
    }

}

