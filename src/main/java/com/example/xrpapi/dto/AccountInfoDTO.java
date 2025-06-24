package com.example.xrpapi.dto;

public class AccountInfoDTO {

    private String address;
    private String balance;
    private Long sequence;

    public AccountInfoDTO(String address, String balance, Long sequence) {
        this.address = address;
        this.balance = balance;
        this.sequence = sequence;
    }

    public String getAddress() {
        return address;
    }

    public String getBalance() {
        return balance;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }
}
