package com.example.xrpapi.service;

import org.stellar.sdk.*;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.IOException;

public class StellarSwap {

    public static void main(String[] args) throws Exception {
        // Horizon server testnet
        Server server = new Server("https://horizon-testnet.stellar.org");

        // Clés : Explicite !
        String fromSecret = "SXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"; // Seed XLM payer
        String toPublicKey = "GXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"; // Receveur USDC

        KeyPair fromKeyPair = KeyPair.fromSecretSeed(fromSecret);

        // Charger dernier sequence number
        AccountResponse fromAccount = server.accounts().account(fromKeyPair.getAccountId());

        // Définir les Assets :
        Asset sendAsset = new AssetTypeNative(); // XLM
        Asset usdc = new AssetTypeCreditAlphaNum4(
                "USDC",
                "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        );


// Construire la transaction
        Transaction transaction = new Transaction.Builder(fromAccount, Network.TESTNET)
                .addOperation(
                        new PathPaymentStrictReceiveOperation.Builder(
                                sendAsset, // asset source (XLM)
                                "10",      // max amount de XLM à envoyer (plafond)
                                toPublicKey,
                                usdc, // ✅ Ici tu utilises bien ta variable usdc déclarée juste avant
                                "5"        // montant USDC exact à recevoir
                        ).build()
                )
                .setTimeout(Transaction.Builder.TIMEOUT_INFINITE)
                .setBaseFee(100)
                .build();


        // Signer
        transaction.sign(fromKeyPair);

        // Soumettre
        SubmitTransactionResponse response = server.submitTransaction(transaction);
        System.out.println("✅ PathPayment Tx Hash : " + response.getHash());
    }
}

