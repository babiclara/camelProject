package hr.algebra.camelle4.security;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @PostConstruct
    public void configureSecurityContextStrategy() {
        SecurityContextHolder.setStrategyName(
                SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        var viewer = User.withDefaultPasswordEncoder()
                .username("viewer").password("viewer123").roles("USER").build();
        var user = User.withDefaultPasswordEncoder()
                .username("user").password("user123").roles("USER").build();
        var admin = User.withDefaultPasswordEncoder()
                .username("admin").password("admin123").roles("ADMIN", "USER").build();
        return new InMemoryUserDetailsManager(viewer, user, admin);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().hasRole("USER")
                );
        return http.build();
    }
}