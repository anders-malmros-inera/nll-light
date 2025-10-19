package se.inera.nll.nlllight.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/login").permitAll()
                .requestMatchers("/prescriptions").permitAll()
                .requestMatchers("/patient/**").permitAll()
                .requestMatchers("/prescriber/**").permitAll()
                .requestMatchers("/pharmacist/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(oidcUserService())
                )
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    // OIDC logout handler that redirects to Keycloak's end_session_endpoint
    // This ensures the Keycloak session is also terminated, not just the local session
    // We manually construct the URL since we can't use issuer-uri discovery from inside the container
    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            String logoutUrl;
            
            // Extract ID token if user is authenticated via OIDC
            if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                String idToken = oidcUser.getIdToken().getTokenValue();
                
                // Construct Keycloak logout URL with id_token_hint
                // Browser needs to access Keycloak at localhost:8082 (not internal keycloak:8080)
                logoutUrl = UriComponentsBuilder
                    .fromUriString("http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/logout")
                    .queryParam("id_token_hint", idToken)
                    .queryParam("post_logout_redirect_uri", "http://localhost:8080/login?logout")
                    .build()
                    .toUriString();
            } else {
                // Fallback if not OIDC authenticated
                logoutUrl = "http://localhost:8080/login?logout";
            }
            
            try {
                response.sendRedirect(logoutUrl);
            } catch (IOException e) {
                // If redirect fails, fall back to local logout
                try {
                    response.sendRedirect("/login?logout");
                } catch (IOException ex) {
                    // Log error if needed
                }
            }
        };
    }

    // Custom OIDC User Service that extracts user info and roles from ID token
    // This avoids issuer mismatch errors when container calls Keycloak via internal network
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();
        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            
            // Extract roles from the ID token's realm_access.roles claim
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>(oidcUser.getAuthorities());
            
            // Get realm roles from ID token
            Object realmAccessObj = oidcUser.getIdToken().getClaim("realm_access");
            if (realmAccessObj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> realmAccess = (java.util.Map<String, Object>) realmAccessObj;
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) rolesObj;
                    for (String role : roles) {
                        // Add both with and without ROLE_ prefix for compatibility
                        mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        mappedAuthorities.add(new SimpleGrantedAuthority(role));
                    }
                }
            }
            
            // Create new OidcUser with mapped authorities
            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    // Custom JwtDecoder to fetch JWKs from internal Docker network
    // Issuer validation is disabled to allow Keycloak's dynamic issuer behavior
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.client.provider.keycloak.jwk-set-uri}") String jwkSetUri
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        // Don't validate issuer - Keycloak uses different URLs for browser vs container access
        // This prevents errors when JWT has issuer=localhost:8082 but we fetch JWKs from keycloak:8080
        decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefault());
        return decoder;
    }
}