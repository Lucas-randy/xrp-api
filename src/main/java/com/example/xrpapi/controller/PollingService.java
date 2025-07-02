package com.example.xrpapi.controller;

import com.example.xrpapi.service.StellarService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PollingService {

    private final StellarService stellarService;

    public PollingService(StellarService stellarService) {
        this.stellarService = stellarService;
    }

    @Scheduled(fixedRate = 30000) // toutes les 30 sec
    public void pollBalance() throws Exception {
        String publicKey = "GBT4MX7QU2DB7CFPCUQZ5GKCVO4EQ3U5IL3EXPGRNF2XZUFD4SBDHHDG";
        String balance = stellarService.checkBalance(publicKey);
        System.out.println("Solde actuel pour " + publicKey + " :\n" + balance);
    }
}