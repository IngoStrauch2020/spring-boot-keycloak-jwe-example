package demo.oauth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
class JwksEndpoint {

    private final RSAKey jweEncKey;

    @GetMapping("/oauth/jwks")
    Object getJwks() {
        log.info("Returning jwks: " + jweEncKey.getKeyID());

        return new JWKSet(jweEncKey).toJSONObject();
    }
}
