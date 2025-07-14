package com.example.xrpapi.service;

import org.springframework.stereotype.Service;
import com.example.xrpapi.entity.Wallet;
import com.example.xrpapi.repository.WalletRepository;
import org.stellar.sdk.*;


import javax.crypto.SecretKey;
import okhttp3.Request;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import com.example.xrpapi.util.CryptoUtil;
import javax.crypto.KeyGenerator;

import org.stellar.sdk.AssetTypeCreditAlphaNum4;
import org.stellar.sdk.ChangeTrustAsset;

import org.stellar.sdk.responses.AccountResponse;

import org.stellar.sdk.responses.SubmitTransactionResponse;

import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

@Service
public class StellarService {

    private final WalletRepository walletRepository;

    private SecretKey encryptionKey;

    public StellarService(WalletRepository walletRepository) throws Exception {
        this.walletRepository = walletRepository;
        this.encryptionKey = CryptoUtil.getFixedTestKey();
    }


    public KeyPair createKeyPair() {
        KeyPair keyPair = KeyPair.random();
        return keyPair;
    }

    // Créer un compte sur le testnet grâce à Friendbot
    public String fundTestAccount(String publicKey) throws Exception {
        OkHttpClient client = new OkHttpClient();
        String url = "https://friendbot.stellar.org/?addr=" + publicKey;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to fund account: " + response);
            }
            return response.body().string();
        }
    }

    public Wallet createAndStoreWallet() throws Exception {
        // Génère une nouvelle paire de clés
        KeyPair keyPair = KeyPair.random();

        // Convertit et VALIDE la seed avant stockage
        String secretSeed = validateStellarSeed(new String(keyPair.getSecretSeed()));

        // Crypte et sauvegarde
        String encrypted = CryptoUtil.encrypt(secretSeed, encryptionKey);
        Wallet wallet = new Wallet(keyPair.getAccountId(), encrypted);
        walletRepository.save(wallet);

        fundTestAccount(wallet.getPublicKey());
        return wallet;
    }

    private String validateStellarSeed(String seed) throws Exception {
        seed = seed.trim();
        try {
            KeyPair.fromSecretSeed(seed); // Valide le checksum
            System.out.println("✅ Seed validée avec succès");
            return seed;
        } catch (Exception e) {
            throw new Exception("Seed invalide générée : " + seed, e);
        }
    }

    public String getSecret(String publicKey) throws Exception {
        Wallet wallet = walletRepository.findById(publicKey)
                .orElseThrow(() -> new Exception("Portefeuille introuvable"));
        return CryptoUtil.decrypt(wallet.getEncryptedSecret(), encryptionKey);
    }

    //Générer une clé AES pour crypter la seed
    public SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // 256 bits pour une bonne sécurité
        return keyGen.generateKey();
    }

    // Méthode pour retourner le cryptage
    public String encryptSecretSeed(String secretSeed) throws Exception {
        return CryptoUtil.encrypt(secretSeed, encryptionKey);
    }

    //Méthode pour retourner le décryptage
    public String decryptSecretSeed(String encryptedSeed) throws Exception {
        return CryptoUtil.decrypt(encryptedSeed, encryptionKey);
    }

    /*Méthode pour vérifier le solde*/
    public String checkBalance(String publicKey) throws Exception {
        Server server = new Server("https://horizon-testnet.stellar.org");
        AccountResponse account = server.accounts().account(publicKey);

        StringBuilder sb = new StringBuilder();
        for (AccountResponse.Balance balance : account.getBalances()) {
            sb.append("Asset: ").append(balance.getAssetType())
                    .append(" | Balance: ").append(balance.getBalance())
                    .append("\n");
        }
        return sb.toString();
    }

    /*Méthode pour envoyer XLM*/
    public String sendXLM(String fromPublicKey, String toPublicKey, String amount) throws Exception {
        System.out.println("🔵 Début de sendXLM");
        try {
            System.out.println("🔵 Création des KeyPairs");
            KeyPair source = KeyPair.fromSecretSeed(getSecret(fromPublicKey));
            KeyPair destination = KeyPair.fromAccountId(toPublicKey);

            System.out.println("🔵 Connexion au serveur Stellar");
            Server server = new Server("https://horizon-testnet.stellar.org");

            System.out.println("🔵 Chargement du compte source: " + source.getAccountId());
            AccountResponse sourceAccountResponse = server.accounts().account(source.getAccountId());

            Account sourceAccount = new Account(source.getAccountId(), sourceAccountResponse.getSequenceNumber());
            System.out.println("🔵 Sequence number: " + sourceAccountResponse.getSequenceNumber());

            System.out.println("🔵 Construction de la transaction");
            Transaction transaction = new Transaction.Builder(sourceAccount, Network.TESTNET)
                    .addOperation(new PaymentOperation.Builder(destination.getAccountId(),
                            new AssetTypeNative(), amount).build())
                    .setTimeout(180)
                    .setBaseFee(Transaction.MIN_BASE_FEE)
                    .build();

            System.out.println("🔵 Signature de la transaction");
            transaction.sign(source);

            System.out.println("🔵 Envoi à Horizon");
            SubmitTransactionResponse response = server.submitTransaction(transaction);

            System.out.println("🔵 Réponse Horizon: " + response);
            return response.isSuccess() ? "✅ Tx Hash: " + response.getHash() :
                    "❌ Erreur: " + response.getExtras().getResultCodes();
        } catch (Exception e) {
            System.out.println("🔴 Exception dans sendXLM: " + e);
            throw e;
        }
    }

    //Méthode pour intéroger l'historique des paiements en Stellar
    public String getPaymentHistory(String publicKey) throws Exception {
        Server server = new Server("https://horizon-testnet.stellar.org");

        StringBuilder history = new StringBuilder();
        history.append("📜 Historique des paiements pour : ").append(publicKey).append("\n");

        // On récupère la liste des paiements
        for (OperationResponse operation : server.payments().forAccount(publicKey).execute().getRecords()) {
            if (operation instanceof PaymentOperationResponse) {
                PaymentOperationResponse payment = (PaymentOperationResponse) operation;
                String from = payment.getFrom();
                String to = payment.getTo();
                String asset = payment.getAsset().getType().equals("native") ? "XLM" : payment.getAsset().toString();
                String amount = payment.getAmount();
                history.append("De : ").append(from)
                        .append(" ➜ Vers : ").append(to)
                        .append(" | Montant : ").append(amount)
                        .append(" ").append(asset)
                        .append("\n");
            }
        }

        return history.toString();
    }

    /*Méthode de test pour diagnostiquer le problème*/
    public String testDecryption(String publicKey) {
        try {
            Wallet wallet = walletRepository.findById(publicKey)
                    .orElseThrow(() -> new Exception("Portefeuille introuvable"));

            System.out.println("=== DIAGNOSTIC DECRYPTION ===");
            System.out.println("Public Key: " + publicKey);
            System.out.println("Encrypted Secret: " + wallet.getEncryptedSecret());

            CryptoUtil.testDecryption(wallet.getEncryptedSecret());

            return "Test de déchiffrement terminé - voir les logs";

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors du test: " + e.getMessage();
        }
    }

    /*
    Méthode pour re-chiffrer une seed si nécessaire
     */
    public String rechiffreSeed(String publicKey, String newSeed) {
        try {
            // Valider la nouvelle seed
            KeyPair.fromSecretSeed(newSeed);

            // Chiffrer la nouvelle seed
            String encryptedSeed = CryptoUtil.encrypt(newSeed, encryptionKey);

            // Mettre à jour en base
            Wallet wallet = walletRepository.findById(publicKey)
                    .orElseThrow(() -> new Exception("Portefeuille introuvable"));

            wallet.setEncryptedSecret(encryptedSeed);
            walletRepository.save(wallet);

            return "✅ Seed re-chiffrée avec succès pour " + publicKey;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Erreur lors du re-chiffrement: " + e.getMessage();
        }
    }

    public String createTrustLineUSDC(String publicKey) throws Exception {
        System.out.println("➡️ Début createTrustLineUSDC pour : " + publicKey);

        try {
            // Utiliser votre méthode qui fonctionne
            String secret = getSecret(publicKey);
            System.out.println("🔑 Secret récupéré avec succès, longueur : " + secret.length());

            // Créer le KeyPair
            KeyPair source = KeyPair.fromSecretSeed(secret);
            System.out.println("✅ KeyPair créé : " + source.getAccountId());

            Server server = new Server("https://horizon-testnet.stellar.org");
            AccountResponse sourceAccount = server.accounts().account(source.getAccountId());
            System.out.println("📊 Compte chargé depuis Horizon");

            // ⚠️ TESTONS DIFFÉRENTES ADRESSES D'ISSUER USDC
            // Adresse 1 : Circle USDC sur testnet
            String[] usdcIssuers = {
                    "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5", // Circle testnet
                    "GBBH5D4BDGBCFCEEN7KMAVVQLXNO55Z2S6V2FPUM4GR34F5MRTSRAAIA", // Alternative 1
                    "GC7M46P53O23PYQ4U3YSTLBYZJNSYIPQSXVYYEZZ37GLD5FHHTAFNX7C"  // Alternative 2
            };

            Exception lastException = null;

            for (String issuer : usdcIssuers) {
                try {
                    System.out.println("🧪 Test avec issuer : " + issuer);

                    // Valider l'adresse de l'issuer
                    KeyPair.fromAccountId(issuer);
                    System.out.println("✅ Adresse issuer valide : " + issuer);

                    // Créer l'asset USDC
                    AssetTypeCreditAlphaNum4 usdc = new AssetTypeCreditAlphaNum4("USDC", issuer);
                    ChangeTrustAsset trustAsset = ChangeTrustAsset.create(usdc);
                    ChangeTrustOperation operation = new ChangeTrustOperation.Builder(trustAsset, "10000").build();

                    // Construire la transaction
                    Transaction transaction = new Transaction.Builder(sourceAccount, Network.TESTNET)
                            .addOperation(operation)
                            .setTimeout(180)
                            .setBaseFee(Transaction.MIN_BASE_FEE)
                            .build();

                    transaction.sign(source);
                    System.out.println("✍️ Transaction signée avec issuer : " + issuer);

                    // Soumettre la transaction
                    SubmitTransactionResponse response = server.submitTransaction(transaction);

                    if (response.isSuccess()) {
                        System.out.println("✅ Trustline ajoutée avec succès avec issuer : " + issuer);
                        return "✅ Trustline USDC ajoutée avec succès (issuer: " + issuer + ")";
                    } else {
                        System.out.println("❌ Échec avec issuer " + issuer + " : " + response.getExtras().getResultCodes());
                        lastException = new Exception("Horizon error: " + response.getExtras().getResultCodes());
                    }

                } catch (Exception e) {
                    System.out.println("❌ Erreur avec issuer " + issuer + " : " + e.getMessage());
                    lastException = e;
                    continue; // Essayer le suivant
                }
            }

            // Si aucun issuer n'a fonctionné
            throw new Exception("Aucun issuer USDC n'a fonctionné. Dernière erreur : " +
                    (lastException != null ? lastException.getMessage() : "Inconnue"));

        } catch (Exception e) {
            System.err.println("❌ Erreur dans createTrustLineUSDC : " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    //Méthode pour créer un Issuer
    public Wallet createIssuer() throws Exception {
        KeyPair issuerKeyPair = KeyPair.random();
        String publicKey = issuerKeyPair.getAccountId();
        String secretSeed = new String(issuerKeyPair.getSecretSeed());

        String encrypted = CryptoUtil.encrypt(secretSeed, encryptionKey);

        Wallet issuerWallet = new Wallet(publicKey, encrypted);
        walletRepository.save(issuerWallet);

        // Fund via Friendbot testnet :
        fundTestAccount(publicKey);

        System.out.println("✅ Issuer créé : " + publicKey);
        return issuerWallet;
    }

    //Issuer qui va dire : envoie de X USDC à XLM dans le même wallet
    public String issueUSDC(String issuerPublicKey, String toPublicKey, String amount) throws Exception {
        System.out.println("➡️ Début issueUSDC");

        String issuerSecret = getSecret(issuerPublicKey);
        KeyPair issuerKeyPair = KeyPair.fromSecretSeed(issuerSecret);

        KeyPair destination = KeyPair.fromAccountId(toPublicKey);

        Server server = new Server("https://horizon-testnet.stellar.org");
        AccountResponse issuerAccount = server.accounts().account(issuerKeyPair.getAccountId());

        // Créer l'Asset USDC
        AssetTypeCreditAlphaNum4 usdc = new AssetTypeCreditAlphaNum4("USDC", issuerKeyPair.getAccountId());

        // Créer PaymentOperation
        PaymentOperation payment = new PaymentOperation.Builder(
                destination.getAccountId(),
                usdc,
                amount
        ).build();

        Transaction transaction = new Transaction.Builder(issuerAccount, Network.TESTNET)
                .addOperation(payment)
                .setTimeout(180)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build();

        transaction.sign(issuerKeyPair);
        SubmitTransactionResponse response = server.submitTransaction(transaction);

        if (response.isSuccess()) {
            System.out.println("✅ USDC émis : TxHash = " + response.getHash());
            return "✅ USDC émis : " + amount + " USDC ➜ " + toPublicKey + " (Tx: " + response.getHash() + ")";
        } else {
            System.out.println("❌ Erreur issueUSDC : " + response.getExtras().getResultCodes());
            return "❌ Erreur issueUSDC : " + response.getExtras().getResultCodes();
        }
    }


    /*
    * Méthode pour swaper de XLM en USDC
    * */
    public String swapXLMtoUSDC(String fromPublicKey, String toPublicKey, String amountXLM, String amountUSDC) throws Exception {
        System.out.println("➡️ Début swapXLMtoUSDC pour : " + fromPublicKey);

        // Récupérer le secret chiffré => le déchiffrer
        String secret = getSecret(fromPublicKey);
        KeyPair source = KeyPair.fromSecretSeed(secret);

        Server server = new Server("https://horizon-testnet.stellar.org");
        AccountResponse sourceAccount = server.accounts().account(source.getAccountId());

        // Définir l'asset USDC
        Asset usdc = new AssetTypeCreditAlphaNum4(
                "USDC",
                "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        );

        // Construire l'opération PathPaymentStrictSend
        PathPaymentStrictSendOperation operation = new PathPaymentStrictSendOperation.Builder(
                new AssetTypeNative(), // Source: XLM
                amountXLM,
                toPublicKey,
                usdc,
                amountUSDC
        ).build();

        Transaction transaction = new Transaction.Builder(sourceAccount, Network.TESTNET)
                .addOperation(operation)
                .setTimeout(180)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build();

        transaction.sign(source);

        SubmitTransactionResponse response = server.submitTransaction(transaction);

        if (response.isSuccess()) {
            return "✅ SWAP XLM ➜ USDC OK (Tx Hash : " + response.getHash() + ")";
        } else {
            return "❌ SWAP failed : " + response.getExtras().getResultCodes();
        }
    }

    public String swapXLMtoUSDC(String fromPublicKey, String toPublicKey, String amount) throws Exception {
        System.out.println("➡️ Début swapXLMtoUSDC (version courte) pour : " + fromPublicKey);

        String secret = getSecret(fromPublicKey); // Tu récupères le secret depuis ton stockage interne
        return swapXLMtoUSDC(fromPublicKey, toPublicKey, amount, "0.000001");
    }


    public String createSellOfferUSDC(String publicKey, String amountXLM, String price) throws Exception {
        String secret = getSecret(publicKey);
        KeyPair source = KeyPair.fromSecretSeed(secret);

        Server server = new Server("https://horizon-testnet.stellar.org");
        AccountResponse sourceAccount = server.accounts().account(source.getAccountId());

        Asset selling = new AssetTypeCreditAlphaNum4("USDC", "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5");
        Asset buying = new AssetTypeNative(); // XLM

        ManageSellOfferOperation offerOperation = new ManageSellOfferOperation.Builder(
                selling, buying, amountXLM, price
        ).build();

        Transaction transaction = new Transaction.Builder(sourceAccount, Network.TESTNET)
                .addOperation(offerOperation)
                .setTimeout(180)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build();

        transaction.sign(source);
        SubmitTransactionResponse response = server.submitTransaction(transaction);

        if (response.isSuccess()) {
            System.out.println("✅ Offre placée !");
            return "✅ Offer created! Hash: " + response.getHash();
        } else {
            System.out.println("❌ Erreur create offer : " + response.getExtras().getResultCodes());
            return "❌ Failed: " + response.getExtras().getResultCodes();
        }
    }

}
