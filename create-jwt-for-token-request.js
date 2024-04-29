const fs = require('fs')
const jks = require('jks-js')
const { v4: uuidv4 } = require('uuid')
const crypto = require('crypto')

const kid = 'jweclient-enc-v1'
const client_id = "jweclient"

const keystore = jks.toPem(
  fs.readFileSync('src/main/resources/keystore.jks'),
  'geheim'
)

const {cert, key} = keystore[kid]
const pubKey = crypto.createPublicKey(cert).export({type:'spki', format:'pem'})

console.log("Private Key", key)
console.log("Public Key", pubKey)

const header = {
  "alg": "RS256",
  "typ": "JWT",
  "kid": kid
}

const header_string = JSON.stringify(header)
console.log("Header", header_string)

const payload = {
  "exp": Date.now() + 3600 * 1000,
  "jti": uuidv4(),
  "iss": client_id,
  "aud": "http://localhost:8081/realms/jwedemo",
  "sub": client_id
}

const playload_string = JSON.stringify(payload)
console.log("Payload", playload_string)

const header_base64 = Buffer.from(header_string).toString("base64url")
console.log("Header (base64)", header_base64)

const payload_base64 = Buffer.from(playload_string).toString("base64url")
console.log("Payload (base64)", payload_base64)

const sign = crypto.createSign('RSA-SHA256')
sign.update(header_base64 + "." + payload_base64)
const sig_base64 = sign.sign(key, 'base64url')

console.log("Signature (base64)", sig_base64)

const jwt = header_base64 + "." + payload_base64 + "." + sig_base64

console.log("=== JWT ===", jwt)
