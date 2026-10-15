package app.template.patches.telegram.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.cert.seedCert
import app.template.patches.shared.firebase.spoofFirebaseCertHashPatch
import app.template.patches.shared.installer.spoofInstallSourcePatch
import app.template.patches.shared.signature.spoofSignatureVerificationPatch

// ════════════════════════════════════════════════════════════════════════════════
// Telegram signing certificate — DER-encoded X.509 (NOT PKCS7)
//
// seedCert() requires the raw DER X.509 certificate extracted from inside the
// PKCS7 .RSA file. Using the full PKCS7 bytes causes Firebase 403 + SSL failures
// because the cert hash doesn't match what getCertificateSHA256Fingerprint() expects.
//
// org.telegram.messenger.web — VK / Nikolay Kudashov key (539 bytes, DER)
// ════════════════════════════════════════════════════════════════════════════════

private const val TELEGRAM_WEB_CERT =
    "MIICFzCCAYCgAwIBAgIEUh+dSTANBgkqhkiG9w0BAQUFADBQMRkwFwYDVQQHExBT" +
    "YWludC1QZXRlcnNidXJnMQswCQYDVQQKEwJWSzELMAkGA1UECxMCVksxGTAXBgNV" +
    "BAMTEE5pa29sYXkgS3VkYXNob3YwHhcNMTMwODI5MTkxMzEzWhcNMzgwODIzMTkx" +
    "MzEzWjBQMRkwFwYDVQQHExBTYWludC1QZXRlcnNidXJnMQswCQYDVQQKEwJWSzEL" +
    "MAkGA1UECxMCVksxGTAXBgNVBAMTEE5pa29sYXkgS3VkYXNob3YwgZ8wDQYJKoZI" +
    "hvcNAQEBBQADgY0AMIGJAoGBAN9emToN7Aq1tVff/3fgsiJxhsvxPR/R7Y6d61ZQ" +
    "xf1EZ7tRv6WFIo0IS9JwRfdBW3xOOPCL42Jjmi7rmwx0naRg8nBfan4UrKdqvjNg" +
    "rwC3GcxfP/TU2gWVgyfpSLNnnmQXrXuqh3m51ol5m6NFg5oEn9RDYkmQVKCAOgF4" +
    "x3N5AgMBAAEwDQYJKoZIhvcNAQEFBQADgYEA3aWM3ZAVnEMezEoVkC6vsHpQ4Bup" +
    "1PjmVewUsGvY6HcSOXEKKJkQOeAuNSdi61JK8HYCu9+0edNxhlilNNQR36swEiyN" +
    "Cl79FlpiBmnYCiIaBKx9aLOBEVDHac+X0ydL6bnyfExYd+q7z4mQQJ5ZQ9+N61Cf" +
    "qD1o6rx098WXZ0M="

internal val telegramCertSeedPatch = rawResourcePatch(default = false) {
    execute {
        seedCert(TELEGRAM_WEB_CERT)
    }
}

val telegramBasePatch = bytecodePatch(default = false) {
    dependsOn(
        telegramCertSeedPatch,
        spoofSignatureVerificationPatch,
        spoofFirebaseCertHashPatch,
        spoofInstallSourcePatch,
    )
}

@JvmSynthetic
internal fun telegramSpoofDependency() = telegramBasePatch
