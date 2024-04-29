package demo.oauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
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
    Object getJwks() throws JOSEException {
        log.info("Returning jwks: " + jweEncKey.getKeyID());

        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) jweEncKey.toPublicKey())
            .privateKey((RSAPrivateKey) jweEncKey.toPrivateKey())
            .keyID(jweEncKey.getKeyID())
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE) // keys from keytool have 'use: "enc"'
            .build();

        return new JWKSet(jwk).toJSONObject();
    }
}
