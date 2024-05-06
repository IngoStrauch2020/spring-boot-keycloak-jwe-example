PoC: client_credentials flow using client-jwt with JWKS URL between Keycloak and Spring Boot application 
----
This is a simple PoC to illustrate the client_credentials flow using a "jwt-bearer" client_assertion_type
instead of a client_secret.
The keycloak client in this example uses a JWKS URL pointing to a Spring Boot application
that exposes the public key in JSON Web Key Set format.
The app then uses the private key to create a JWT that is sent as client_assertion to the
Keycloak token endpoint.

# Keycloak

This following starts a local Keycloak instance accessible via http://localhost:8081/

## Run Keycloak via docker compose

This starts a server with user "admin" and password "admin" as admin.
In addition, it automatically imports a realm called "jwedemo" and a client named "jweclient".

```
docker compose up -d
```

The client credentials of the "jweclient" in the "jwedemo" realm are already configured.
In the client "keys" tab you can see that the JWKS URL is http://localhost:8080/oauth/jwks, this is the endpoint where
Keycloak obtains the RSA public key from the Spring Boot service to verify the client_assertion JWZ
during the token request.
And in the "credentials" tab you can see that the "Client Authenticator" is set to "Signed JWT"
instead of "Client ID and Secret".

The keycloak docker container is started with a couple of log levels set to DEBUG or TRACE
in order to see what's going on behind the scenes like this:

```
docker logs -f keycloak_with_jwks_example
```

## Add user to jwedemo Realm

Add a user with username "tester" and password "test". Assign role "user" of the "jweclient" to "tester".

# Spring Boot Service

The following starts a Spring Boot Service available on http://localhost:8080 which exposes
several endpoints:
- /oauth/jwks - exposes the public RSA key used for client_assertion validation by keycloak
- /oauth/client_assertion - generates a JWT that can be used as client_assertion
- /oauth/token - requests an access token from keycloak using the client_credentials flow and returns the full response from keycloak
- /api/claims - exposes a protected endpoint that can be accessed with an access token and returns the contained claims
- /api/hello - similar to /api/claims but returns a greeting using the username or subject from the access token
- /api/hello - similar to /api/claims but returns the email from the access token

## Prepare Spring Boot Service

### Generate Keystore
A public/private key pair is part of this repository.
It was created like this:
```
keytool -genkey \
        -alias jweclient-enc-v1 \
        -keystore src/main/resources/keystore.jks \
        -storepass geheim \
        -dname "CN=Thomas Darimont, OU=R&D, O=tdlabs, L=Saarbrücken, ST=SL, C=DE"  \
        -keyalg RSA \
        -keysize 2048 \
        -validity 9999
```

### Running the Spring Boot App
Just run the App class with the main method.

# Demo

## Retrieve Tokens
For demo purposes we obtain access tokens via Resource Owner Password Credentials (ROPC) Grant - "Direct Access Grants" in Keycloak speech.

Call http://localhost:8080/oauth/client_assertion and use the response as value for `KC_JWT` in the snippet below.

```
KC_USERNAME=tester
KC_PASSWORD=test
KC_CLIENT_ID=jweclient
KC_ISSUER=http://localhost:8081/realms/jwedemo
KC_JWT=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Imp3ZWNsaWVudC1lbmMtdjEifQ.eyJleHAiOjE3MDE0NDE1ODQ5MDcsImp0aSI6IjYxOWQ0MGJmLWU1NWYtNGU1MC1hZGQ2LTdkMTg1NzE2NWU2OCIsImlzcyI6Imp3ZWNsaWVudCIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9yZWFsbXMvandlZGVtbyIsInN1YiI6Imp3ZWNsaWVudCJ9.JiQgt1Lr6HpYgdL6k-OR-y2yBd5vQBYhHncYrjT50fsD7oiHvmigA9rc3LFxLIMleTQv5H3iYAvf6HLE3GmNFhrIlc6AJmC1gEXAiepXUAaLzHBbDXweemfeW1WMuxU8UBaiHhULMVP8wDTle7jvYdUyPv1T4EvX89r-ge0jut2i443ftMZt2cBBr0CwYiJzFZfeI5lUwRwWqPTKuQGXVciXbUumN7iKr7zXhcKfYjKKWkNcOEB0Lps8A4C8m7uLazO6Wmrc_Jb4rO5LoKOJrT4XPT5AkraVrukpDLn1OkXeNwlUL2776B8yjwl1i0TKjHEHPQ2b9am5wmcoldbfDw

KC_RESPONSE=$( \
curl \
  -d "client_id=$KC_CLIENT_ID" \
  -d "username=$KC_USERNAME" \
  -d "password=$KC_PASSWORD" \
  -d "grant_type=password" \
  -d "client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer" \
  -d "client_assertion=$KC_JWT" \
  -d "scope=profile openid" \
  "$KC_ISSUER/protocol/openid-connect/token" \
)
echo $KC_RESPONSE | jq -C .

KC_ID_TOKEN=$(echo $KC_RESPONSE | jq -r .id_token)
KC_ACCESS_TOKEN=$(echo $KC_RESPONSE | jq -r .access_token)

```

Another example uses the Client Credentials Grant - "Service account roles" in Keycloak speech (this is for machine-to-machine communication).

```
KC_CLIENT_ID=jweclient
KC_ISSUER=http://localhost:8081/realms/jwedemo
KC_JWT=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Imp3ZWNsaWVudC1lbmMtdjEifQ.eyJleHAiOjE3MDE0NDE1ODQ5MDcsImp0aSI6IjYxOWQ0MGJmLWU1NWYtNGU1MC1hZGQ2LTdkMTg1NzE2NWU2OCIsImlzcyI6Imp3ZWNsaWVudCIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9yZWFsbXMvandlZGVtbyIsInN1YiI6Imp3ZWNsaWVudCJ9.JiQgt1Lr6HpYgdL6k-OR-y2yBd5vQBYhHncYrjT50fsD7oiHvmigA9rc3LFxLIMleTQv5H3iYAvf6HLE3GmNFhrIlc6AJmC1gEXAiepXUAaLzHBbDXweemfeW1WMuxU8UBaiHhULMVP8wDTle7jvYdUyPv1T4EvX89r-ge0jut2i443ftMZt2cBBr0CwYiJzFZfeI5lUwRwWqPTKuQGXVciXbUumN7iKr7zXhcKfYjKKWkNcOEB0Lps8A4C8m7uLazO6Wmrc_Jb4rO5LoKOJrT4XPT5AkraVrukpDLn1OkXeNwlUL2776B8yjwl1i0TKjHEHPQ2b9am5wmcoldbfDw

KC_RESPONSE=$( \
curl \
  -d "client_id=$KC_CLIENT_ID" \
  -d "grant_type=client_credentials" \
  -d "client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer" \
  -d "client_assertion=$KC_JWT" \
  -d "scope=profile openid" \
  "$KC_ISSUER/protocol/openid-connect/token" \
)
echo $KC_RESPONSE | jq -C .

KC_ID_TOKEN=$(echo $KC_RESPONSE | jq -r .id_token)
KC_ACCESS_TOKEN=$(echo $KC_RESPONSE | jq -r .access_token)
```

## Use encrypted Access-Token to access the /api/claims Endpoint
```
curl -v \
     -H "Authorization: Bearer $KC_ACCESS_TOKEN" \
     http://localhost:8080/api/claims | jq -C .
```

# More information

## client_assertion in detail

This repo contains a script that 
- reads the keystore
- prints the public and private key in PEM format
- prints the header and payload of the client_assertion in JSON and Base64 encoded
- prints the Base64 encoded signature
- and finally prints the JWT (like the /oauth/client_assertion endpoint)

Run it like this:
```
npm|yarn|pnpm install
npm|yarn|pnpm start
```

It's not strictly necessary to use for the demo, just for illustration purposes if you
want to have a more low-level overview.
