package com.example.xrpapi.service;

import org.springframework.stereotype.Service;
import com.example.xrpapi.entity.Wallet;
import com.example.xrpapi.repository.WalletRepository;
import org.stellar.sdk.*;

import org.stellar.sdk.responses.*;
import org.stellar.sdk.requests.*;
import org.stellar.sdk.xdr.MemoType;


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
import org.stellar.sdk.AssetTypeCreditAlphaNum;

import org.stellar.sdk.requests.PaymentsRequestBuilder;
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

    /*public Wallet createAndStoreWallet() throws Exception {
        KeyPair keyPair = createKeyPair();
        String publicKey = keyPair.getAccountId();
        String secretSeed = new String(keyPair.getSecretSeed());

        String encrypted = CryptoUtil.encrypt(secretSeed, encryptionKey);

        Wallet wallet = new Wallet(publicKey, encrypted);
        walletRepository.save(wallet);

        fundTestAccount(publicKey);

        return wallet;
    }*/
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

   /* public String sendXLM(String fromPublicKey, String toPublicKey, String amount) throws Exception {
        KeyPair source = KeyPair.fromSecretSeed(getSecret(fromPublicKey)); // Déchiffre d'abord
        KeyPair destination = KeyPair.fromAccountId(toPublicKey);

        Server server = new Server("https://horizon-testnet.stellar.org");

        // Charger compte source
        AccountResponse sourceAccountResponse = server.accounts().account(source.getAccountId());

        // Adapter : créer un Account pour le TransactionBuilder
        Account sourceAccount = new Account(source.getAccountId(), sourceAccountResponse.getSequenceNumber());

        Transaction transaction = new Transaction.Builder(sourceAccount, Network.TESTNET)
                .addOperation(new PaymentOperation.Builder(destination.getAccountId(), new AssetTypeNative(), amount).build())
                .setTimeout(180)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build();

        transaction.sign(source); // Signature locale

        // Envoyer à Horizon
        SubmitTransactionResponse response = server.submitTransaction(transaction);
        System.out.println("🔔 Horizon Response : isSuccess = " + response.isSuccess());
        System.out.println("🔔 Horizon Hash : " + response.getHash());
        System.out.println("🔔 Horizon Extras : " + response.getExtras());

        return response.isSuccess() ? "✅ Tx Hash: " + response.getHash() : response.getExtras().getResultCodes().toString();
    }*/

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

    //Méthode pour vérifier le solde
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

    //Méthode pour convertir XLM en USDC
    /*public String swapXLMtoUSDC(String fromPublicKey, String toPublicKey, String amount) throws Exception {
        System.out.println("🔄 Début swap XLM ➜ USDC");

        // Déchiffre la clé secrète de l'envoyeur
        KeyPair source = KeyPair.fromSecretSeed(getSecret(fromPublicKey));
        KeyPair destination = KeyPair.fromAccountId(toPublicKey);

        Server server = new Server("https://horizon-testnet.stellar.org");

        // Vérifie que le compte destination existe (optionnel)
        server.accounts().account(destination.getAccountId());

        // Charge le compte source
        AccountResponse sourceAccount = server.accounts().account(source.getAccountId());
        Account sourceAccountTx = new Account(source.getAccountId(), sourceAccount.getSequenceNumber());

        // Définit l'asset de destination (USDC sur testnet : Circle USDC)
        Asset usdc = new AssetTypeCreditAlphaNum4(
                "USDC",
                "GA5ZSEU5GAC3UQJ5BJYJGBQYY3ZXKXNBHFOKRS67VJYNNZYDW7ZACBZK"  // Ex : Circle USDC Testnet issuer
        );

        // Crée la transaction PathPaymentStrictSend
        Transaction transaction = new Transaction.Builder(sourceAccountTx, Network.TESTNET)
                .addOperation(new PathPaymentStrictSendOperation.Builder(
                        new AssetTypeNative(), // Envoie XLM
                        amount,
                        destination.getAccountId(),
                        usdc,
                        "0.01" // minAmount : peut être ajusté
                ).build())
                .setTimeout(180)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build();

        // Signe localement
        transaction.sign(source);

        // Envoie à Horizon
        SubmitTransactionResponse response = server.submitTransaction(transaction);

        System.out.println("🔄 Swap Submit Response: isSuccess = " + response.isSuccess());
        System.out.println("🔄 Hash : " + response.getHash());

        return response.isSuccess()
                ? "✅ Swap OK, Tx Hash: " + response.getHash()
                : "❌ Swap KO : " + response.getExtras().getResultCodes();
    }*/

    /*public String createTrustLineUSDC(String publicKey) throws Exception {
        System.out.println("➡️ Début createTrustLineUSDC");

        // Déchiffrer le seed
        String secret = getSecret(publicKey);


        System.out.println("🔑 Decrypted seed : " + secret);

        // Validation explicite
        try {
            KeyPair.fromSecretSeed(secret); // Lance une exception si la seed est invalide
        } catch (Exception e) {
            throw new Exception("Seed corrompue : " + secret + " (longueur : " + secret.length() + ")", e);
        }

        KeyPair source = KeyPair.fromSecretSeed(secret);

        Server server = new Server("https://horizon-testnet.stellar.org");

        // Charger le compte
        AccountResponse sourceAccount = server.accounts().account(source.getAccountId());

        // Définir l'asset USDC (exemple Circle)
        AssetTypeCreditAlphaNum4 usdc = new AssetTypeCreditAlphaNum4(
                "USDC",
                "GA5ZSEU5GAC3UQJ5BJYJGBQYY3ZXKXNBHFOKRS67VJYNNZYDW7ZACBZK"
        );

        ChangeTrustAsset trustAsset = ChangeTrustAsset.create(usdc);

        ChangeTrustOperation operation = new ChangeTrustOperation.Builder(trustAsset, "10000").build();

        Transaction transaction = new Transaction.Builder(sourceAccount, Network.TESTNET)
                .addOperation(operation)
                .setTimeout(180)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build();

        transaction.sign(source);

        SubmitTransactionResponse response = server.submitTransaction(transaction);

        if (response.isSuccess()) {
            System.out.println("✅ Trustline ajoutée !");
            return "✅ Trustline USDC ajoutée";
        } else {
            System.out.println("❌ Horizon response : " + response.getExtras().getResultCodes());
            return "❌ Horizon response : " + response.getExtras().getResultCodes();
        }
    }*/

    // Méthode de test pour diagnostiquer le problème
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

    // Méthode pour re-chiffrer une seed si nécessaire
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



    // Méthode alternative avec un asset simple pour tester
    public String createSimpleTrustLine(String publicKey) throws Exception {
        System.out.println("➡️ Test avec un asset simple");

        try {
            String secret = getSecret(publicKey);
            KeyPair source = KeyPair.fromSecretSeed(secret);
            Server server = new Server("https://horizon-testnet.stellar.org");
            AccountResponse sourceAccount = server.accounts().account(source.getAccountId());

            // Créer un asset de test simple avec une adresse d'issuer valide
            String testIssuer = "GCKFBEIYTKP5RDBQMU2TQCQHD6TNQTP2JXGMH5UQNXZP6GKXQBGBTB4Q";
            AssetTypeCreditAlphaNum4 testAsset = new AssetTypeCreditAlphaNum4("TEST", testIssuer);

            ChangeTrustAsset trustAsset = ChangeTrustAsset.create(testAsset);
            ChangeTrustOperation operation = new ChangeTrustOperation.Builder(trustAsset, "1000").build();

            Transaction transaction = new Transaction.Builder(sourceAccount, Network.TESTNET)
                    .addOperation(operation)
                    .setTimeout(180)
                    .setBaseFee(Transaction.MIN_BASE_FEE)
                    .build();

            transaction.sign(source);
            SubmitTransactionResponse response = server.submitTransaction(transaction);

            if (response.isSuccess()) {
                return "✅ Trustline TEST ajoutée avec succès";
            } else {
                return "❌ Erreur : " + response.getExtras().getResultCodes();
            }

        } catch (Exception e) {
            throw new Exception("Erreur trustline simple : " + e.getMessage(), e);
        }
    }

    // Méthode bonus pour créer une trustline avec un asset personnalisé
    /*public String createCustomTrustLine(String publicKey, String assetCode, String issuerAddress, String limit) throws Exception {
        System.out.println("➡️ Début createCustomTrustLine pour : " + publicKey);

        try {
            String secret = getSecret(publicKey);
            validateStellarSeed(secret);

            // Valider l'adresse de l'issuer
            try {
                KeyPair.fromAccountId(issuerAddress);
                System.out.println("✅ Adresse issuer validée : " + issuerAddress);
            } catch (Exception e) {
                throw new Exception("Adresse issuer invalide : " + issuerAddress, e);
            }

            KeyPair source = KeyPair.fromSecretSeed(secret);
            Server server = new Server("https://horizon-testnet.stellar.org");

            AccountResponse sourceAccount = server.accounts().account(source.getAccountId());

            // Créer l'asset personnalisé
            Asset asset;
            if (assetCode.length() <= 4) {
                asset = new AssetTypeCreditAlphaNum4(assetCode, issuerAddress);
            } else {
                asset = new AssetTypeCreditAlphaNum12(assetCode, issuerAddress);
            }

            ChangeTrustAsset trustAsset = ChangeTrustAsset.create(asset);
            ChangeTrustOperation operation = new ChangeTrustOperation.Builder(trustAsset, limit).build();

            Transaction transaction = new Transaction.Builder(sourceAccount, Network.TESTNET)
                    .addOperation(operation)
                    .setTimeout(180)
                    .setBaseFee(Transaction.MIN_BASE_FEE)
                    .build();

            transaction.sign(source);
            SubmitTransactionResponse response = server.submitTransaction(transaction);

            if (response.isSuccess()) {
                return "✅ Trustline " + assetCode + " ajoutée avec succès";
            } else {
                return "❌ Erreur : " + response.getExtras().getResultCodes().toString();
            }

        } catch (Exception e) {
            throw new Exception("Erreur lors de la création de la trustline personnalisée : " + e.getMessage(), e);
        }
    }*/

    /*
    * Méthode pour swaper de XLM en USDC
    * */
    public String swapXLMtoUSDC(String fromPublicKey, String toPublicKey, String amount) throws Exception {
        System.out.println("➡️ Début swapXLMtoUSDC pour : " + fromPublicKey);

        String secret = getSecret(fromPublicKey);
        KeyPair source = KeyPair.fromSecretSeed(secret);

        Server server = new Server("https://horizon-testnet.stellar.org");
        AccountResponse sourceAccount = server.accounts().account(source.getAccountId());

        System.out.println("➡️ AssetCode: 'USDC', length: " + "USDC".trim().length());
        System.out.println("➡️ Issuer: " + "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5");


        Asset usdc = new AssetTypeCreditAlphaNum4(
                "USDC",
                "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        );
        System.out.println("➡️ Asset created OK: " + usdc);

        PathPaymentStrictSendOperation operation = new PathPaymentStrictSendOperation.Builder(
                new AssetTypeNative(), // XLM à envoyer
                amount,
                toPublicKey,           // destinataire correct
                usdc,
                "0.000001"
        ).build();

        Transaction transaction = new Transaction.Builder(sourceAccount, Network.TESTNET)
                .addOperation(operation)
                .setTimeout(180)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build();

        transaction.sign(source);

        SubmitTransactionResponse response = server.submitTransaction(transaction);

        if (response.isSuccess()) {
            System.out.println("✅ SWAP réussi !");
            return "✅ SWAP XLM ➜ USDC OK (Tx Hash : " + response.getHash() + ")";
        } else {
            System.out.println("❌ SWAP échoué : " + response.getExtras().getResultCodes());
            return "❌ SWAP failed : " + response.getExtras().getResultCodes();
        }
    }





}
