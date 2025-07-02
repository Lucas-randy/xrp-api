package com.example.xrpapi.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    private String publicKey;
    private String encryptedSecret;

    public Wallet() {
    }

    public Wallet(String publicKey, String encryptedSecret) {
        this.publicKey = publicKey;
        this.encryptedSecret = encryptedSecret;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getEncryptedSecret() {
        return encryptedSecret;
    }

    public void setEncryptedSecret(String encryptedSecret) {
        this.encryptedSecret = encryptedSecret;
    }
}
