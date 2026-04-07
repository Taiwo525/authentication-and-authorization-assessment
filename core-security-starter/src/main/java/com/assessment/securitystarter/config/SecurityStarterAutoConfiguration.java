package com.assessment.securitystarter.config;

import com.assessment.securitystarter.error.GlobalExceptionHandler;
import com.assessment.securitystarter.error.SecurityAccessDeniedHandler;
import com.assessment.securitystarter.security.SecurityAuthenticationEntryPoint;
import com.assessment.securitystarter.error.SecurityErrorWriter;
import com.assessment.securitystarter.security.JwtAuthenticationFilter;
import com.assessment.securitystarter.security.JwtTokenService;
import com.assessment.securitystarter.logging.AuthenticatedRequestLoggingFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityStarterProperties.class)
public class SecurityStarterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenService jwtTokenService(SecurityStarterProperties properties) {
        return new JwtTokenService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationFilter(jwtTokenService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticatedRequestLoggingFilter authenticatedRequestLoggingFilter() {
        return new AuthenticatedRequestLoggingFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityErrorWriter securityErrorWriter(ObjectMapper objectMapper) {
        return new SecurityErrorWriter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint(SecurityErrorWriter securityErrorWriter) {
        return new SecurityAuthenticationEntryPoint(securityErrorWriter);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityAccessDeniedHandler securityAccessDeniedHandler(SecurityErrorWriter securityErrorWriter) {
        return new SecurityAccessDeniedHandler(securityErrorWriter);
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticatedRequestLoggingFilter authenticatedRequestLoggingFilter,
            SecurityAuthenticationEntryPoint authenticationEntryPoint,
            SecurityAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/public/**", "/api/v1/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(authenticatedRequestLoggingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
