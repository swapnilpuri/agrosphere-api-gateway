package com.agrosphere.gateway;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.web.cors.*;
@Configuration
class SecurityConfiguration {
    @Bean SecurityFilterChain security(HttpSecurity http, Environment env) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(cors(env)))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}).authenticationEntryPoint((req,res,ex) -> {
                res.setHeader("WWW-Authenticate", "Bearer"); GatewayProblem.write(res,401,"Authentication required");
            }))
            .addFilterAfter(new SubjectRateLimit(env.getProperty("GATEWAY_RATE_LIMIT", Integer.class, 120)), BearerTokenAuthenticationFilter.class)
            .build();
    }
    private CorsConfigurationSource cors(Environment env) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(env.getProperty("CORS_ALLOWED_ORIGINS", "http://localhost:4200").split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type","X-Correlation-ID","traceparent","tracestate"));
        config.setExposedHeaders(List.of("X-Correlation-ID","Location","Retry-After")); config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**", config); return source;
    }
}
