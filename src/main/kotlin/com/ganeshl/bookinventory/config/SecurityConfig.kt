package com.ganeshl.bookinventory.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enables @PreAuthorize, @PostAuthorize
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun userDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
        val adminUser = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("adminpass"))
            .roles("ADMIN", "USER")
            .build()

        val regularUser = User.builder()
            .username("user")
            .password(passwordEncoder.encode("userpass"))
            .roles("USER")
            .build()

        return InMemoryUserDetailsManager(adminUser, regularUser)
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // Disable CSRF for stateless APIs, consider enabling for web apps
            .authorizeHttpRequests { authz ->
                authz
                    // OpenAPI documentation and Actuator health endpoints
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    // Public read access for some GET requests
                    .requestMatchers(HttpMethod.GET, "/api/v1/books", "/api/v1/books/**", "/api/v1/books/search").permitAll()
                    // Write operations (POST, PUT, PATCH, DELETE) require ADMIN role (can also be done with @PreAuthorize)
                    .requestMatchers(HttpMethod.POST, "/api/v1/books/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/books/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/books/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/books/**").hasRole("ADMIN")
                    .anyRequest().authenticated() // All other requests need authentication
            }
            .httpBasic(Customizer.withDefaults()) // Use HTTP Basic authentication
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Stateless session management
            }
        return http.build()
    }
}