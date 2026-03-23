package io.legado.app.help.http

import android.annotation.SuppressLint
import android.net.http.X509TrustManagerExtensions
import io.legado.app.utils.printOnDebug


import java.io.IOException
import java.io.InputStream
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

@Suppress("unused")
object SSLHelper {

    /**
     * To solve client not trusting server certificate,
     * most online solutions disable check,
     * which is a huge security hole
     */
    val unsafeTrustManager: X509TrustManager =
        @SuppressLint("CustomX509TrustManager")
        object : X509TrustManager {
            @SuppressLint("TrustAllX509TrustManager")
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                //do nothing, accept any client cert
            }

            @SuppressLint("TrustAllX509TrustManager")
            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                //do nothing, accept any client cert
            }

            fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, host: String): List<X509Certificate> {
                return chain.toList()
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }

    val unsafeTrustManagerExtensions by lazy {
        X509TrustManagerExtensions(unsafeTrustManager)
    }

    val unsafeSSLSocketFactory: SSLSocketFactory by lazy {
        try {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf(unsafeTrustManager), SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * Base interface for hostname verification. If URL hostname and server ID hostname mismatch during handshake,
     * mechanism calls this interface to allow connection. Policy can be cert-based or other scheme.
     * Callbacks used when default hostname verification fails. Return true if hostname acceptable.
     */
    val unsafeHostnameVerifier: HostnameVerifier = HostnameVerifier { _, _ -> true }

    class SSLParams {
        lateinit var sSLSocketFactory: SSLSocketFactory
        lateinit var trustManager: X509TrustManager
    }

    /**
     * https one-way authentication
     * Can configure trusted server certificate policy, otherwise defaults to CA verification. If not CA trusted, verification fails.
     */
    fun getSslSocketFactory(trustManager: X509TrustManager): SSLParams? {
        return getSslSocketFactoryBase(trustManager, null, null)
    }

    /**
     * https one-way authentication
     * Verify server certificate using certificate containing server public key
     */
    fun getSslSocketFactory(vararg certificates: InputStream): SSLParams? {
        return getSslSocketFactoryBase(null, null, null, *certificates)
    }

    /**
     * https mutual authentication
     * bksFile and password -> Client uses bks cert to verify server cert
     * certificates -> Verify server cert using cert containing server public key
     */
    fun getSslSocketFactory(
        bksFile: InputStream,
        password: String,
        vararg certificates: InputStream
    ): SSLParams? {
        return getSslSocketFactoryBase(null, bksFile, password, *certificates)
    }

    /**
     * https mutual authentication
     * bksFile and password -> Client uses bks cert to verify server cert
     * X509TrustManager -> If custom verification needed, implement it, otherwise pass null
     */
    fun getSslSocketFactory(
        bksFile: InputStream,
        password: String,
        trustManager: X509TrustManager
    ): SSLParams? {
        return getSslSocketFactoryBase(trustManager, bksFile, password)
    }

    private fun getSslSocketFactoryBase(
        trustManager: X509TrustManager?,
        bksFile: InputStream?,
        password: String?,
        vararg certificates: InputStream
    ): SSLParams? {
        val sslParams = SSLParams()
        try {
            val keyManagers = prepareKeyManager(bksFile, password)
            val trustManagers = prepareTrustManager(*certificates)
            val manager: X509TrustManager = trustManager ?: chooseTrustManager(trustManagers)
            // Create TLS SSLContext object, that uses our TrustManager
            val sslContext = SSLContext.getInstance("TLS")
            // Init SSLContext with obtained trustManagers, so sslContext trusts certs in keyStore
            // First param is auth key manager for auth verification (e.g. self-signed). Second is trusted cert manager for server cert verification.
            sslContext.init(keyManagers, arrayOf<TrustManager>(manager), null)
            // Get SSLSocketFactory via sslContext
            sslParams.sSLSocketFactory = sslContext.socketFactory
            sslParams.trustManager = manager
            return sslParams
        } catch (e: NoSuchAlgorithmException) {
            e.printOnDebug()
        } catch (e: KeyManagementException) {
            e.printOnDebug()
        }
        return null
    }

    private fun prepareKeyManager(bksFile: InputStream?, password: String?): Array<KeyManager>? {
        try {
            if (bksFile == null || password == null) return null
            val clientKeyStore = KeyStore.getInstance("BKS")
            clientKeyStore.load(bksFile, password.toCharArray())
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(clientKeyStore, password.toCharArray())
            return kmf.keyManagers
        } catch (e: Exception) {
            e.printOnDebug()
        }
        return null
    }

    private fun prepareTrustManager(vararg certificates: InputStream): Array<TrustManager> {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        // Create default KeyStore, storing certs we trust
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)
        for ((index, certStream) in certificates.withIndex()) {
            val certificateAlias = index.toString()
            // Certificate factory generates cert from file stream
            val cert = certificateFactory.generateCertificate(certStream)
            // Put cert into keyStore as trusted cert
            keyStore.setCertificateEntry(certificateAlias, cert)
            try {
                certStream.close()
            } catch (e: IOException) {
                e.printOnDebug()
            }
        }
        //Create default TrustManagerFactory
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        //Init TrustManagerFactory with previous keyStore, so tmf trusts certs in keyStore
        tmf.init(keyStore)
        //Get TrustManager array via tmf, TrustManager will trust keyStore certs
        return tmf.trustManagers
    }

    private fun chooseTrustManager(trustManagers: Array<TrustManager>): X509TrustManager {
        for (trustManager in trustManagers) {
            if (trustManager is X509TrustManager) {
                return trustManager
            }
        }
        throw NullPointerException()
    }
}
