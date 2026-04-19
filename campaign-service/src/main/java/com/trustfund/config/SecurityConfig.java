package com.trustfund.config;

import com.trustfund.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui/index.html",
                                                                "/api-docs",
                                                                "/api-docs/**",
                                                                "/v3/api-docs",
                                                                "/v3/api-docs/**",
                                                                "/swagger-resources/**",
                                                                "/webjars/**")
                                                .permitAll()
                                                .requestMatchers("/actuator/**").permitAll()
                                                // Campaigns endpoints
                                                .requestMatchers(HttpMethod.GET, "/api/campaigns").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/campaigns/{id}").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/campaigns/category/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/campaign-categories/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/campaigns/fund-owner/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.PUT, "/api/campaigns/*/update-balance")
                                                .permitAll()
                                                .requestMatchers("/api/campaigns/**").authenticated()
                                                // Fundraising Goals endpoints
                                                .requestMatchers(HttpMethod.GET, "/api/fundraising-goals/**")
                                                .permitAll()
                                                .requestMatchers("/api/fundraising-goals/**").authenticated()
                                                // Campaign follow endpoints
                                                .requestMatchers(HttpMethod.GET, "/api/campaign-follows/**").permitAll()
                                                .requestMatchers("/api/campaign-follows/**").authenticated()
                                                // Expenditures endpoints
                                                .requestMatchers(HttpMethod.GET, "/api/expenditures/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/expenditures/{id}").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/expenditures/campaign/**")
                                                .permitAll()
                                                .requestMatchers("/api/expenditures/items/**").permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/internal-transactions/campaign/**")
                                                .permitAll()
                                                // Feed posts endpoints
                                                .requestMatchers(HttpMethod.GET, "/api/feed-posts/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/forum/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/flags/**").permitAll()
                                                // User post seen — allow GET (returns empty for anon), POST needs auth
                                                .requestMatchers(HttpMethod.GET, "/api/user-post-seen").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/user-post-seen").authenticated()
                                                .requestMatchers("/api/user-post-seen/**").authenticated()
                                                // Approval tasks
                                                .requestMatchers(HttpMethod.GET, "/api/admin/tasks/campaign/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/admin/tasks/type/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/admin/tasks/staff/**")
                                                .hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/api/admin/tasks/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .sessionManagement(sess -> sess
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(ex -> ex
                                                .accessDeniedHandler(accessDeniedHandler()))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        @Bean
        public AccessDeniedHandler accessDeniedHandler() {
                return (HttpServletRequest request, HttpServletResponse response,
                                org.springframework.security.access.AccessDeniedException accessDeniedException) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");

                        String jsonResponse = String.format(
                                        "{\"timestamp\":\"%s\", \"status\":403, \"error\":\"Forbidden\", \"message\":\"%s\", \"path\":\"%s\"}",
                                        LocalDateTime.now().toString(),
                                        accessDeniedException.getMessage().replace("\"", "\\\""),
                                        request.getRequestURI());

                        response.getWriter().write(jsonResponse);
                };
        }
}
