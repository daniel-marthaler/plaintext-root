/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security;

import ch.plaintext.boot.plugins.security.oidc.JdbcClientRegistrationRepository;
import ch.plaintext.boot.plugins.security.oidc.PlaintextOidcUserService;
import ch.plaintext.boot.plugins.security.service.MyRememberMeRepositoryRepository;
import ch.plaintext.boot.plugins.security.service.MyUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class PlaintextSecurityConfig {

    private final MyRememberMeRepositoryRepository tokenRepository;
    private final MyUserDetailsService userDetail;
    private final PlaintextAuthenticationSuccessHandler authenticationSuccessHandler;
    private final PlaintextSecurityProperties securityProperties;
    private final JdbcClientRegistrationRepository clientRegistrationRepository;
    private final PlaintextOidcUserService oidcUserService;

    // Framework-Defaults: CSRF ignorieren
    private static final List<String> DEFAULT_CSRF_IGNORE = List.of(
            "/autologin", "/discovery/login", "/nosec/**",
            "/api/preferences/**",
            "/**/*.xhtml", "/**/*.html"  // JSF uses ViewState for CSRF protection
    );

    // Framework-Defaults: Ohne Authentication erreichbar
    private static final List<String> DEFAULT_PERMIT_ALL = List.of(
            "/autologin", "/autologin/**",
            "/discovery/login",
            "/login.xhtml", "/login.html", "/jakarta.faces.resource/**",
            "/actuator/health",
            "/h2-console/**",
            "/nosec/**",
            "/oauth2/**", "/login/oauth2/**"
    );

    public PlaintextSecurityConfig(MyRememberMeRepositoryRepository tokenRepository,
                                   MyUserDetailsService detail,
                                   PlaintextAuthenticationSuccessHandler authenticationSuccessHandler,
                                   PlaintextSecurityProperties securityProperties,
                                   JdbcClientRegistrationRepository clientRegistrationRepository,
                                   PlaintextOidcUserService oidcUserService) {
        this.tokenRepository = tokenRepository;
        this.userDetail = detail;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.securityProperties = securityProperties;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oidcUserService = oidcUserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF-Ignore: Framework-Defaults + App-spezifische Pfade
        List<String> csrfIgnore = new ArrayList<>(DEFAULT_CSRF_IGNORE);
        csrfIgnore.addAll(securityProperties.getCsrfIgnorePatterns());
        String[] csrfIgnoreArray = csrfIgnore.toArray(new String[0]);

        // PermitAll: Framework-Defaults + App-spezifische Pfade
        List<String> permitAll = new ArrayList<>(DEFAULT_PERMIT_ALL);
        permitAll.addAll(securityProperties.getPermitAllPatterns());
        String[] permitAllArray = permitAll.toArray(new String[0]);

        http
                .securityContext(ctx -> ctx
                        .securityContextRepository(securityContextRepository())
                )
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(csrfIgnoreArray)
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://unpkg.com; " +
                                        "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://unpkg.com; " +
                                        "img-src 'self' data: https://*.tile.openstreetmap.org https://raw.githubusercontent.com; " +
                                        "font-src 'self' data:; " +
                                        "connect-src 'self'; " +
                                        "frame-ancestors 'self'; " +
                                        "base-uri 'self'; " +
                                        "form-action 'self' https://*.plaintext.ch")
                        )
                )
                .authorizeHttpRequests(authorize -> {
                    authorize
                            .requestMatchers(permitAllArray).permitAll()
                            .requestMatchers("/actuator/**").hasRole("ADMIN")
                            .requestMatchers("/api/branding/logo").authenticated()
                            .requestMatchers("/api/preferences/**").authenticated()
                            .anyRequest().authenticated();
                })
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/index.html", true)
                        .failureUrl("/login.html?error=true")
                        .successHandler(authenticationSuccessHandler)
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login.html")
                        .clientRegistrationRepository(clientRegistrationRepository)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService)
                        )
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/login.html?error=oidc")
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(org.springframework.http.HttpMethod.POST, "/logout"))
                        .logoutSuccessUrl("/login.html")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .clearAuthentication(true)
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeServices(rememberMeServices())
                        .tokenRepository(tokenRepository)
                        .tokenValiditySeconds(1209600) // 2 weeks
                        .key("mySecretKey")
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    RememberMeAuthenticationFilter rememberMeFilter(PersistentTokenBasedRememberMeServices service, AuthenticationManager auth) {
        return new RememberMeAuthenticationFilter(auth, service);
    }

    @Bean
    PersistentTokenBasedRememberMeServices rememberMeServices() {
        return new PersistentTokenBasedRememberMeServices("mySecretKey", userDetail, tokenRepository);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
