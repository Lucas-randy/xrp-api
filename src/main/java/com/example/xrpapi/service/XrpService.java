package com.example.xrpapi.service;

import okhttp3.HttpUrl;
import com.example.xrpapi.dto.AccountInfoDTO;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.common.LedgerSpecifier;
import org.xrpl.xrpl4j.model.transactions.Address;

@Service
public class XrpService {

    private final XrplClient client;

    public XrpService() {
        this.client = new XrplClient(HttpUrl.get("https://s.altnet.rippletest.net:51234/"));
    }

    public AccountInfoDTO getAccountInfo(String address) {
        try {
            Address classicAddress = Address.of(address);

            AccountInfoRequestParams params = AccountInfoRequestParams.builder()
                    .ledgerSpecifier(LedgerSpecifier.VALIDATED)
                    .account(classicAddress)
                    .build();

            AccountInfoResult result = client.accountInfo(params);

            return new AccountInfoDTO(
                    result.accountData().account().value(),
                    result.accountData().balance().value().toString(),
                    result.accountData().sequence().longValue()
            );

        } catch (JsonRpcClientErrorException e) {
            e.printStackTrace();
            return null;
        }
    }
}
