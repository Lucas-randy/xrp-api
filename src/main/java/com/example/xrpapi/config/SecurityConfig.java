package com.example.xrpapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // OK pour test
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/stellar/create").permitAll()
                        .requestMatchers("/stellar/send").permitAll()  // ✅ autorise explicitement /send
                        .requestMatchers("/stellar/secret/**").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(httpBasic -> httpBasic.disable())   // ✅ Désactive httpBasic
                .formLogin(form -> form.disable());
        return http.build();
    }

}
