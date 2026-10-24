package com.agrosphere.gateway;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
@Configuration
class JwtValidationConfiguration {
    @Bean JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${OIDC_JWK_SET_URI:}") String jwks, @Value("${OIDC_AUDIENCE:agrosphere-api}") String audience) {
        if (audience.isBlank()) throw new IllegalArgumentException("OIDC audience is required");
        var decoder = (jwks.isBlank() ? NimbusJwtDecoder.withIssuerLocation(issuer) : NimbusJwtDecoder.withJwkSetUri(jwks))
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),
            jwt -> jwt.getAudience().contains(audience) ? OAuth2TokenValidatorResult.success() :
                OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null))));
        return decoder;
    }
}
