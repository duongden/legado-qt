package io.legado.app.help

import android.util.Base64
import cn.hutool.crypto.digest.DigestUtil
import cn.hutool.crypto.digest.HMac
import cn.hutool.crypto.symmetric.SymmetricCrypto
import io.legado.app.help.crypto.AsymmetricCrypto
import io.legado.app.help.crypto.Sign
import io.legado.app.help.crypto.SymmetricCryptoAndroid
import io.legado.app.utils.MD5Utils


/**
 * js加解密扩展类, 在js中通过java变量调用
 * 添加方法，请更新文档/legado/app/src/main/assets/help/JsHelp.md
 */
@Suppress("unused")
interface JsEncodeUtils {

    fun md5Encode(str: String): String {
        return MD5Utils.md5Encode(str)
    }

    fun md5Encode16(str: String): String {
        return MD5Utils.md5Encode16(str)
    }


    //******************Symmetric Encryption/Decryption************************//

    /**
     * Usage in JS
     * java.createSymmetricCrypto(transformation, key, iv).decrypt(data)
     * java.createSymmetricCrypto(transformation, key, iv).decryptStr(data)

     * java.createSymmetricCrypto(transformation, key, iv).encrypt(data)
     * java.createSymmetricCrypto(transformation, key, iv).encryptBase64(data)
     * java.createSymmetricCrypto(transformation, key, iv).encryptHex(data)
     */

    /* Call SymmetricCrypto use random key when key is null */
    fun createSymmetricCrypto(
        transformation: String,
        key: ByteArray?,
        iv: ByteArray?
    ): SymmetricCrypto {
        val symmetricCrypto = SymmetricCryptoAndroid(transformation, key)
        return if (iv != null && iv.isNotEmpty()) symmetricCrypto.setIv(iv) else symmetricCrypto
    }

    fun createSymmetricCrypto(
        transformation: String,
        key: ByteArray
    ): SymmetricCrypto {
        return createSymmetricCrypto(transformation, key, null)
    }

    fun createSymmetricCrypto(
        transformation: String,
        key: String
    ): SymmetricCrypto {
        return createSymmetricCrypto(transformation, key, null)
    }

    fun createSymmetricCrypto(
        transformation: String,
        key: String,
        iv: String?
    ): SymmetricCrypto {
        return createSymmetricCrypto(
            transformation, key.encodeToByteArray(), iv?.encodeToByteArray()
        )
    }
    //******************Asymmetric Encryption/Decryption************************//

    /* Use random key when keys are all null */
    fun createAsymmetricCrypto(
        transformation: String
    ): AsymmetricCrypto {
        return AsymmetricCrypto(transformation)
    }

    //******************Signature************************//
    fun createSign(
        algorithm: String
    ): Sign {
        return Sign(algorithm)
    }
    //******************Symmetric Encryption/Decryption Old************************//

    /////AES
    /**
     * AES decode to ByteArray
     * @param str Encrypted AES data
     * @param key Decryption key
     * @param transformation AES encryption mode
     * @param iv ECB mode offset vector
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decrypt(str)")
    )
    fun aesDecodeToByteArray(
        str: String, key: String, transformation: String, iv: String
    ): ByteArray? {
        return createSymmetricCrypto(transformation, key, iv).decrypt(str)
    }

    /**
     * AES decode to String
     * @param str Encrypted AES data
     * @param key Decryption key
     * @param transformation AES encryption mode
     * @param iv ECB mode offset vector
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decryptStr(str)")
    )
    fun aesDecodeToString(
        str: String, key: String, transformation: String, iv: String
    ): String? {
        return createSymmetricCrypto(transformation, key, iv).decryptStr(str)
    }

    /**
     * AES decode to String, params Base64 encrypted
     *
     * @param data Encrypted string
     * @param key Base64 Key
     * @param mode Mode
     * @param padding Padding
     * @param iv Base64 Salt
     * @return Decrypted string
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decryptStr(data)")
    )
    fun aesDecodeArgsBase64Str(
        data: String,
        key: String,
        mode: String,
        padding: String,
        iv: String
    ): String? {
        return createSymmetricCrypto(
            "AES/${mode}/${padding}",
            Base64.decode(key, Base64.NO_WRAP),
            Base64.decode(iv, Base64.NO_WRAP)
        ).decryptStr(data)
    }

    /**
     * Base64 AES decode into ByteArray
     * @param str Input data
     * @param key Key
     * @param transformation Mode
     * @param iv Offset
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decrypt(str)")
    )
    fun aesBase64DecodeToByteArray(
        str: String, key: String, transformation: String, iv: String
    ): ByteArray? {
        return createSymmetricCrypto(transformation, key, iv).decrypt(str)
    }

    /**
     * Base64 AES decode into String
     * @param str Input data
     * @param key Key
     * @param transformation Mode
     * @param iv Offset
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decryptStr(str)")
    )
    fun aesBase64DecodeToString(
        str: String, key: String, transformation: String, iv: String
    ): String? {
        return createSymmetricCrypto(transformation, key, iv).decryptStr(str)
    }

    /**
     * Encrypt aes to ByteArray
     * @param data Input data
     * @param key AES key
     * @param transformation Mode
     * @param iv ECB offset
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decrypt(data)")
    )
    fun aesEncodeToByteArray(
        data: String, key: String, transformation: String, iv: String
    ): ByteArray? {
        return createSymmetricCrypto(transformation, key, iv).encrypt(data)
    }

    /**
     * Encrypt aes to String
     * @param data Input data
     * @param key AES key
     * @param transformation Mode
     * @param iv ECB offset
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decryptStr(data)")
    )
    fun aesEncodeToString(
        data: String, key: String, transformation: String, iv: String
    ): String? {
        return createSymmetricCrypto(transformation, key, iv).decryptStr(data)
    }

    /**
     * AES Encrypt then Base64 ByteArray
     * @param data Input data
     * @param key AES key
     * @param transformation Mode
     * @param iv ECB offset
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).encryptBase64(data).toByteArray()")
    )
    fun aesEncodeToBase64ByteArray(
        data: String, key: String, transformation: String, iv: String
    ): ByteArray? {
        return createSymmetricCrypto(transformation, key, iv).encryptBase64(data).toByteArray()
    }

    /**
     * AES Encrypt then Base64 String
     * @param data Input data
     * @param key AES key
     * @param transformation Mode
     * @param iv ECB offset
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).encryptBase64(data)")
    )
    fun aesEncodeToBase64String(
        data: String, key: String, transformation: String, iv: String
    ): String? {
        return createSymmetricCrypto(transformation, key, iv).encryptBase64(data)
    }


    /**
     * AES encrypt and convert to Base64, params Base64 encrypted
     *
     * @param data Data to encrypt
     * @param key Base64 Key
     * @param mode Mode
     * @param padding Padding
     * @param iv Base64 Salt
     * @return Encrypted Base64
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).encryptBase64(data)")
    )
    fun aesEncodeArgsBase64Str(
        data: String,
        key: String,
        mode: String,
        padding: String,
        iv: String
    ): String? {
        return createSymmetricCrypto("AES/${mode}/${padding}", key, iv).encryptBase64(data)
    }

    /////DES
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decryptStr(data)")
    )
    fun desDecodeToString(
        data: String, key: String, transformation: String, iv: String
    ): String? {
        return createSymmetricCrypto(transformation, key, iv).decryptStr(data)
    }

    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decryptStr(data)")
    )
    fun desBase64DecodeToString(
        data: String, key: String, transformation: String, iv: String
    ): String? {
        return createSymmetricCrypto(transformation, key, iv).decryptStr(data)
    }

    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).encrypt(data)")
    )
    fun desEncodeToString(
        data: String, key: String, transformation: String, iv: String
    ): String? {
        return String(createSymmetricCrypto(transformation, key, iv).encrypt(data))
    }

    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).encryptBase64(data)")
    )
    fun desEncodeToBase64String(
        data: String, key: String, transformation: String, iv: String
    ): String? {
        return createSymmetricCrypto(transformation, key, iv).encryptBase64(data)
    }

    //////3DES
    /**
     * 3DES decrypt
     *
     * @param data Encrypted string
     * @param key Key
     * @param mode Mode
     * @param padding Padding
     * @param iv Salt
     * @return Decrypted string
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decryptStr(data)")
    )
    fun tripleDESDecodeStr(
        data: String,
        key: String,
        mode: String,
        padding: String,
        iv: String
    ): String? {
        return createSymmetricCrypto("DESede/${mode}/${padding}", key, iv).decryptStr(data)
    }

    /**
     * 3DES decrypt, params Base64 encrypted
     *
     * @param data Encrypted string
     * @param key Base64 Key
     * @param mode Mode
     * @param padding Padding
     * @param iv Base64 Salt
     * @return Decrypted string
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).decryptStr(data)")
    )
    fun tripleDESDecodeArgsBase64Str(
        data: String,
        key: String,
        mode: String,
        padding: String,
        iv: String
    ): String? {
        return createSymmetricCrypto(
            "DESede/${mode}/${padding}",
            Base64.decode(key, Base64.NO_WRAP),
            iv.encodeToByteArray()
        ).decryptStr(data)
    }


    /**
     * 3DES encrypt and convert to Base64
     *
     * @param data Encrypted string
     * @param key Key
     * @param mode Mode
     * @param padding Padding
     * @param iv Salt
     * @return Encrypted Base64
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).encryptBase64(data)")
    )
    fun tripleDESEncodeBase64Str(
        data: String,
        key: String,
        mode: String,
        padding: String,
        iv: String
    ): String? {
        return createSymmetricCrypto("DESede/${mode}/${padding}", key, iv)
            .encryptBase64(data)
    }

    /**
     * 3DES encrypt and convert to Base64, params Base64 encrypted
     *
     * @param data Encrypted string
     * @param key Base64 Key
     * @param mode Mode
     * @param padding Padding
     * @param iv Base64 Salt
     * @return Encrypted Base64
     */
    @Deprecated(
        "过于繁琐弃用",
        ReplaceWith("createSymmetricCrypto(transformation, key, iv).encryptBase64(data)")
    )
    fun tripleDESEncodeArgsBase64Str(
        data: String,
        key: String,
        mode: String,
        padding: String,
        iv: String
    ): String? {
        return createSymmetricCrypto(
            "DESede/${mode}/${padding}",
            Base64.decode(key, Base64.NO_WRAP),
            iv.encodeToByteArray()
        ).encryptBase64(data)
    }

//******************Message Digest/HMAC************************//

    /**
     * Generate digest, convert to hex string
     *
     * @param data Input data
     * @param algorithm Signature algorithm
     * @return Hex string
     */
    fun digestHex(
        data: String,
        algorithm: String,
    ): String {
        return DigestUtil.digester(algorithm).digestHex(data)
    }

    /**
     * Generate digest, convert to Base64 string
     *
     * @param data Input data
     * @param algorithm Signature algorithm
     * @return Base64 string
     */
    fun digestBase64Str(
        data: String,
        algorithm: String,
    ): String {
        return Base64.encodeToString(DigestUtil.digester(algorithm).digest(data), Base64.NO_WRAP)
    }

    /**
     * Generate HMAC, convert to hex string
     *
     * @param data Input data
     * @param algorithm Signature algorithm
     * @param key Secret key
     * @return Hex string
     */
    @Suppress("FunctionName")
    fun HMacHex(
        data: String,
        algorithm: String,
        key: String
    ): String {
        return HMac(algorithm, key.toByteArray()).digestHex(data)
    }

    /**
     * Generate HMAC, convert to Base64 string
     *
     * @param data Input data
     * @param algorithm Signature algorithm
     * @param key Secret key
     * @return Base64 string
     */
    @Suppress("FunctionName")
    fun HMacBase64(
        data: String,
        algorithm: String,
        key: String
    ): String {
        return Base64.encodeToString(
            HMac(algorithm, key.toByteArray()).digest(data),
            Base64.NO_WRAP
        )
    }


}