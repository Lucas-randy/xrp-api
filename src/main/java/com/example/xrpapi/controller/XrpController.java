package com.example.xrpapi.controller;

import com.example.xrpapi.dto.AccountInfoDTO;
import com.example.xrpapi.service.XrpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/xrp")
public class XrpController {

    @Autowired
    private XrpService xrpService;

    /*
    * http://localhost:8080/api/xrp/account/rKKanvN1F68KRmGQD26dH7UzrWoZruRXyP
    * */
    @GetMapping("/account/{address}")
    public ResponseEntity<?> getAccountInfo(@PathVariable String address) {
        try {
            AccountInfoDTO result = xrpService.getAccountInfo(address);
            if (result == null) {
                return ResponseEntity.status(404).body("Compte introuvable ou erreur côté serveur XRP");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur : " + e.getMessage());
        }
    }
}