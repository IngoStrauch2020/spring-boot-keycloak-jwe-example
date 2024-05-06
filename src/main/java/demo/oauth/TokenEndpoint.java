package demo.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import demo.oauth.model.ClientAssertionPayload;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequiredArgsConstructor
class TokenEndpoint {

  private final RSAKey jweEncKey;

  private final ObjectMapper objectMapper;

  @Value("${keycloak.clientId}")
  String clientId;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  String issuerUri;

  /**
   * Generate a JWT that can be used to obtain an access token for the jweclient
   *
   * @return The generated JWT
   */
  @GetMapping("/oauth/client_assertion")
  Object getClientAssertion() throws JOSEException, JsonProcessingException {

    JWSObject clientAssertion = buildClientAssertion();

    return clientAssertion.serialize();
  }

  /**
   * Requests a new access token from keycloak
   *
   * @return The full response from keycloak
   */
  @GetMapping("/oauth/token")
  Object getAccessToken() throws JOSEException, JsonProcessingException {

    JWSObject clientAssertion = buildClientAssertion();

    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> formData= new LinkedMultiValueMap<>();
    formData.add("client_id", clientId);
    formData.add("grant_type","client_credentials");
    formData.add("client_assertion_type","urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer");
    formData.add("client_assertion", clientAssertion.serialize());
    formData.add("scope","profile openid");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

    String access_token_url = issuerUri + "/protocol/openid-connect/token";

    return restTemplate.exchange(access_token_url, HttpMethod.POST, request, String.class);
  }

  private JWSObject buildClientAssertion() throws JOSEException, JsonProcessingException {
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .keyID(jweEncKey.getKeyID())
        .type(JOSEObjectType.JWT)
        .build();

    JWSSigner signer = new RSASSASigner(jweEncKey);

    ClientAssertionPayload clientAssertionPayload = ClientAssertionPayload.builder()
        .exp(Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond())
        .jti(UUID.randomUUID())
        .iss(clientId)
        .sub(clientId)
        .aud(issuerUri)
        .build();

    Payload payload = new Payload(objectMapper.writeValueAsString(clientAssertionPayload));

    JWSObject jwsObject = new JWSObject(header, payload);
    jwsObject.sign(signer);
    return jwsObject;
  }
}
