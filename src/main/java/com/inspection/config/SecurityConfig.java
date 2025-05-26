package com.inspection.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.inspection.security.JwtAuthenticationFilter;
import com.inspection.security.JwtTokenProvider;
import com.inspection.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    /* 인증 관리자 빈 등록 */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /* 인증 필터 빈 등록 */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, userService);
    }

    /* 인증 필터 빈 등록 */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/signup",
                    "/api/auth/check-username",
                    "/api/companies/**",
                    "/api/users/**",
                    "/api/inspections/**",
                    "/api/notices/**",
                    "/api/inquiries/**",
                    "/uploads/**",
                    "/api/stt/**",
                    "/api/fire-inspections/**",
                    "/api/fire-safety-inspections/**",
                    "/api/contracts/**",
                    "/api/facilities/**",
                    "/api/facility-images/**",
                    "/api/pdf/**",
                    "/api/contract-pdf/**",
                    "/api/contract-templates/**",
                    "/api/email/**",
                    "/api/sms/**",
                    "/api/codes/**",
                    "/api/signature/**",
                    "/api/participants/**",
                    "/api/brands/**",
                    "/api/contract-event-logs/**",
                    "/service-request-images/**",
                    "/api/nice/certification/**"
                ).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**").hasRole("MANAGER")
                .requestMatchers("/api/user/**").hasRole("USER")
                .requestMatchers("/api/companies/**").authenticated()
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/inspections/**").authenticated()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users/check/**").permitAll()
                .requestMatchers("/api/inspections/{id}").permitAll()
                .requestMatchers("/api/fire-safety-inspections/{id}").permitAll()
                .requestMatchers("/fire-safety-inspection/**").permitAll()
                .requestMatchers("/inspection/**").permitAll()
                .requestMatchers("/api/chat/**").permitAll()
                .requestMatchers("/api/chat/qa/**").authenticated()
                .requestMatchers(
                    "/api/guest-inquiries",           // 문의 등록
                    "/api/guest-inquiries/check",     // 문의 확인
                    "/api/guest-inquiries/all"        // 공개 목록
                ).permitAll()
                .requestMatchers(
                    "/api/guest-inquiries/*/answer",  // 답변 등록
                    "/api/guest-inquiries/admin",     // 관리자 조회
                    "/api/guest-inquiries/admin/*"    // 관리자 상세 조회
                ).hasAnyRole("ADMIN", "MANAGER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Unauthorized");
                })
            );

        return http.build();
    }

    /* CORS 설정 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3001"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:uploads/")
                        .setCachePeriod(3600)
                        .resourceChain(true);
            }
        };
    }
} 