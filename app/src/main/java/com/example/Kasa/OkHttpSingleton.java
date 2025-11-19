package com.example.Kasa;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;

public final class OkHttpSingleton {

    /**
     * Certificate pinning configuration.
     * SHA-256 hashes of the public keys from the certificates.
     * 
     * WARNING: The pins below are PLACEHOLDERS and will cause API calls to fail!
     * You MUST replace them with actual certificate pins before building for production.
     * 
     * To get the pin for a domain, use:
     * openssl s_client -servername <domain> -connect <domain>:443 < /dev/null | \
     *   openssl x509 -pubkey -noout | \
     *   openssl pkey -pubin -outform der | \
     *   openssl dgst -sha256 -binary | \
     *   openssl enc -base64
     * 
     * Or use online tools like: https://www.ssllabs.com/ssltest/
     * 
     * IMPORTANT: 
     * - Update these pins when certificates are rotated
     * - Keep backup pins for certificate rotation periods
     * - For development/testing, you can temporarily remove certificatePinner from the OkHttpClient builder
     */
    private static final CertificatePinner CERTIFICATE_PINNER = new CertificatePinner.Builder()
            // OpenAI API - REPLACE WITH ACTUAL PIN
            .add("api.openai.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            // Ghana NLP API - REPLACE WITH ACTUAL PIN
            .add("translation-api.ghananlp.org", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build();

    /** single shared OkHttp instance for the whole app with certificate pinning */
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .certificatePinner(CERTIFICATE_PINNER)
            .build();

    private OkHttpSingleton(){}   // no instances

    /** Get the shared client (e.g. OkHttpSingleton.client()) */
    public static OkHttpClient client() { return CLIENT; }

    /** Cancel every in‑flight call (used by your Cancel button) */
    public static void cancelAll() { CLIENT.dispatcher().cancelAll(); }
}
