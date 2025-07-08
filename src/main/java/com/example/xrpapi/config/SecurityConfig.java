package com.example.xrpapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // OK pour test
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/stellar/create").permitAll()
                        .requestMatchers("/stellar/**").permitAll()  // ✅ autorise explicitement /send
                        .requestMatchers("/stellar/secret/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/stellar/trustline/usdc").permitAll()
                        .anyRequest().permitAll()
                )
                .httpBasic(httpBasic -> httpBasic.disable())   // ✅ Désactive httpBasic
                .formLogin(form -> form.disable());
        return http.build();
    }

}
