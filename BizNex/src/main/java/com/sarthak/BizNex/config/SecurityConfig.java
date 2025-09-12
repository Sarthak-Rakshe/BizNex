package com.sarthak.BizNex.config;

import com.sarthak.BizNex.security.JwtAuthenticationFilter;
import com.sarthak.BizNex.security.RestAccessDeniedHandler;
import com.sarthak.BizNex.security.RestAuthEntryPoint;
import com.sarthak.BizNex.security.ForcePasswordChangeFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central Spring Security configuration: stateless JWT auth, endpoint authorization rules,
 * CORS setup, and exception handling (401/403) wiring.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthEntryPoint restAuthEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final ForcePasswordChangeFilter forcePasswordChangeFilter;

    // CORS properties (comma separated in application.properties / env overrides)
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String corsAllowedOrigins;
    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String corsAllowedMethods;
    @Value("${app.cors.allowed-headers:Authorization,Content-Type}")
    private String corsAllowedHeaders;
    @Value("${app.cors.exposed-headers:Authorization}")
    private String corsExposedHeaders;
    @Value("${app.cors.allow-credentials:true}")
    private boolean corsAllowCredentials;
    @Value("${app.cors.max-age:3600}")
    private long corsMaxAge;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Only login & forgot-password are public
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/forgot-password", "/api/v1/auth/first-login/password").permitAll()
                        // OpenAPI / Swagger UI endpoints
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/billing/**", "/api/v1/bills/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(forcePasswordChangeFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    private List<String> csvToList(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = csvToList(corsAllowedOrigins);
        boolean wildcard = origins.stream().anyMatch(o -> o.contains("*"));
        if (wildcard) {
            // If wildcard present and credentials requested, disable credentials per CORS spec
            if (corsAllowCredentials) {
                log.warn("Wildcard origin used with credentials=true; forcing allowCredentials=false to satisfy CORS spec.");
            }
            cfg.setAllowCredentials(false); // will be false if wildcard
            cfg.setAllowedOriginPatterns(origins);
        } else {
            cfg.setAllowedOrigins(origins);
            cfg.setAllowCredentials(corsAllowCredentials);
        }
        cfg.setAllowedMethods(csvToList(corsAllowedMethods));
        cfg.setAllowedHeaders(csvToList(corsAllowedHeaders));
        cfg.setExposedHeaders(csvToList(corsExposedHeaders));
        cfg.setMaxAge(corsMaxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
