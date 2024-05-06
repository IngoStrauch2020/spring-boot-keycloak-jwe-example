package demo.config;

import demo.keycloak.KeycloakGrantedAuthoritiesConverter;
import demo.keycloak.KeycloakJwtAuthenticationConverter;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@RequiredArgsConstructor
class JwtSecurityConfig {

    @Bean
    KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter(Converter<Jwt, Collection<GrantedAuthority>> converter) {
        return new KeycloakJwtAuthenticationConverter(converter);
    }

    @Bean
    Converter<Jwt, Collection<GrantedAuthority>> keycloakGrantedAuthoritiesConverter(@Value("${keycloak.clientId}") String clientId,
                                                                                     GrantedAuthoritiesMapper mapper) {
        return new KeycloakGrantedAuthoritiesConverter(clientId, mapper);
    }

}
