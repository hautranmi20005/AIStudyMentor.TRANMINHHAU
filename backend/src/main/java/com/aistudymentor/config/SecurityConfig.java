package com.aistudymentor.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SecurityConfig {

    private static final String GUARDIAN_CONSENT_VERIFY_PATH = "/api/v1/guardian-consent/verify";
    private static final List<String> GUARDIAN_CONSENT_VIEWER_ORIGINS = List.of(
            "http://localhost:8025",
            "http://127.0.0.1:8025",
            "null");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;
    private final GuardianConsentEnforcementFilter guardianConsentEnforcementFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Value("${app.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Bean
    @Order(1)
    @Profile({"prod", "dev"})
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("MONITORING"))
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Profile({"prod", "dev"})
    public UserDetailsService monitoringUserDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${management.security.username}") String username,
            @Value("${management.security.password}") String password) {
        if (username.isBlank() || password.length() < 24) {
            throw new IllegalStateException("Actuator credentials must be set and the password must be at least 24 characters.");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("MONITORING")
                .build());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/mfa/verify",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/guardian-consent/verify").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/guardian-consent/verify").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiRateLimitFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(guardianConsentEnforcementFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        allowedOrigins.forEach(this::validateCorsOrigin);
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        CorsConfiguration consentConfiguration = new CorsConfiguration();
        consentConfiguration.setAllowedOrigins(GUARDIAN_CONSENT_VIEWER_ORIGINS);
        consentConfiguration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        consentConfiguration.setAllowedHeaders(List.of("Content-Type", "Accept", "Origin"));
        consentConfiguration.setAllowCredentials(false);
        consentConfiguration.setMaxAge(Duration.ofHours(1));
        return request -> {
            if (isSameOrigin(request)) {
                return null;
            }
            if (GUARDIAN_CONSENT_VERIFY_PATH.equals(requestPath(request))) {
                return consentConfiguration;
            }
            return source.getCorsConfiguration(request);
        };
    }

    private String requestPath(jakarta.servlet.http.HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return request.getRequestURI().substring(contextPath.length());
    }

    private boolean isSameOrigin(jakarta.servlet.http.HttpServletRequest request) {
        String originHeader = request.getHeader("Origin");
        if (originHeader == null || originHeader.isBlank()) {
            return false;
        }
        try {
            URI origin = URI.create(originHeader);
            String scheme = request.getScheme();
            int originPort = effectivePort(origin.getScheme(), origin.getPort());
            int requestPort = effectivePort(scheme, request.getServerPort());
            return origin.getRawUserInfo() == null
                    && origin.getHost() != null
                    && scheme.equalsIgnoreCase(origin.getScheme())
                    && request.getServerName().equalsIgnoreCase(origin.getHost())
                    && requestPort == originPort;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private int effectivePort(String scheme, int port) {
        if (port >= 0) {
            return port;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return -1;
    }

    private void validateCorsOrigin(String origin) {
        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must contain valid origins only.");
        }
        if ("*".equals(origin)
                || !List.of("http", "https").contains(uri.getScheme())
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must contain exact http(s) origins without paths or wildcards.");
        }
    }

    @Bean
    public FilterRegistrationBean<ApiRateLimitFilter> apiRateLimitFilterRegistration(ApiRateLimitFilter filter) {
        FilterRegistrationBean<ApiRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
