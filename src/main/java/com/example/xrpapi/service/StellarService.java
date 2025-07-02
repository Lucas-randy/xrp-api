package com.example.xrpapi.service;

import org.springframework.stereotype.Service;
import com.example.xrpapi.entity.Wallet;
import com.example.xrpapi.repository.WalletRepository;
import org.stellar.sdk.KeyPair;
import javax.crypto.SecretKey;
import okhttp3.Request;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import com.example.xrpapi.util.CryptoUtil;
import javax.crypto.KeyGenerator;

import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.Account;

import org.stellar.sdk.responses.SubmitTransactionResponse;

import org.stellar.sdk.Server;

import org.stellar.sdk.Network;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.PaymentOperation;

import org.stellar.sdk.requests.PaymentsRequestBuilder;
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
        KeyPair keyPair = createKeyPair();
        String publicKey = keyPair.getAccountId();
        String secretSeed = new String(keyPair.getSecretSeed());

        String encrypted = CryptoUtil.encrypt(secretSeed, encryptionKey);

        Wallet wallet = new Wallet(publicKey, encrypted);
        walletRepository.save(wallet);

        fundTestAccount(publicKey);

        return wallet;
    }

    public String getSecret(String publicKey) throws Exception {
        Wallet wallet = walletRepository.findById(publicKey)
                .orElseThrow(() -> new Exception("Portefeuille introuvable"));
        return CryptoUtil.decrypt(wallet.getEncryptedSecret(), encryptionKey);
    }

    public String sendXLM(String fromPublicKey, String toPublicKey, String amount) throws Exception {
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

    //Méthode pour intéroger l'historique des paiements en Stellar
    public String getPaymentHistory(String publicKey) throws Exception {
        Server server = new Server("https://horizon-testnet.stellar.org");

        PaymentsRequestBuilder paymentsRequest = server.payments().forAccount(publicKey).limit(10).order(org.stellar.sdk.requests.RequestBuilder.Order.DESC);

        StringBuilder sb = new StringBuilder();
        for (org.stellar.sdk.responses.operations.OperationResponse op : paymentsRequest.execute().getRecords()) {
            if (op instanceof PaymentOperationResponse) {
                PaymentOperationResponse payment = (PaymentOperationResponse) op;
                sb.append("ID: ").append(payment.getId())
                        .append(" | Type: ").append(payment.getType())
                        .append(" | From: ").append(payment.getFrom())
                        .append(" | To: ").append(payment.getTo())
                        .append(" | Amount: ").append(payment.getAmount())
                        .append(" | Asset: ").append(payment.getAsset().getType())
                        .append("\n");
            }
        }
        return sb.toString();
    }
}
