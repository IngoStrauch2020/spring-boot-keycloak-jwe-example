package demo.config;

import demo.keycloak.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@EnableWebSecurity
@RequiredArgsConstructor
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeRequests(c -> {
          c
              .mvcMatchers("/api/claims").hasAuthority("SCOPE_openid")
              .antMatchers("/oauth/**").anonymous()
              .antMatchers(HttpMethod.POST, "/oauth/decrypt").anonymous()
              .anyRequest().authenticated();
        }).oauth2ResourceServer().jwt(c -> {
          c.jwtAuthenticationConverter(jwtAuthenticationConverter);
        });
  }
}
