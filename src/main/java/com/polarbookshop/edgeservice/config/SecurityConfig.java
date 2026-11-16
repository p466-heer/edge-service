package com.polarbookshop.edgeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveClientRegistrationRepository clientRegistrations) {

        // Protect browser requests with CSRF.
        // Bearer-token API requests do not use browser sessions.
        ServerWebExchangeMatcher csrfMatcher = exchange -> {
            HttpMethod method = exchange.getRequest().getMethod();

            if (method == HttpMethod.GET ||
                    method == HttpMethod.HEAD ||
                    method == HttpMethod.OPTIONS ||
                    method == HttpMethod.TRACE) {
                return ServerWebExchangeMatcher.MatchResult.notMatch();
            }

            String authorization = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authorization != null &&
                    authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return ServerWebExchangeMatcher.MatchResult.notMatch();
            }

            return ServerWebExchangeMatcher.MatchResult.match();
        };

        return http
                .csrf(csrf -> csrf
                        .requireCsrfProtectionMatcher(csrfMatcher))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/books/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/csrf").authenticated()
                        .anyExchange().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusServerEntryPoint(
                                        HttpStatus.UNAUTHORIZED)))
                .oauth2Login(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults()))
                .logout(logout -> logout
                        .logoutSuccessHandler(
                                oidcLogoutSuccessHandler(clientRegistrations)))
                .build();
    }

    @Bean
    ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }

    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler(
            ReactiveClientRegistrationRepository clientRegistrations) {

        var handler =
                new OidcClientInitiatedServerLogoutSuccessHandler(
                        clientRegistrations);

        handler.setPostLogoutRedirectUri("{baseUrl}");

        return handler;
    }
}
