package io.legado.app.service

import android.content.Context
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxTensorLike
import io.legado.app.base.BaseService
import io.legado.app.constant.AppLog
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.putPrefBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ByteBuffer.allocate
import java.nio.LongBuffer

class AITranslationService : BaseService() {

    private var ortEnv: OrtEnvironment? = null
    private var encoderSession: OrtSession? = null
    private var decoderSession: OrtSession? = null
    private var tokenizer: VietnameseChineseTokenizer? = null
    private var isModelLoaded = false

    companion object {
        private const val AI_MODEL_ENABLED = "ai_model_enabled"
        
        fun isAIEnabled(context: Context): Boolean {
            return context.getPrefBoolean(AI_MODEL_ENABLED, false)
        }
        
        fun setAIEnabled(context: Context, enabled: Boolean) {
            context.putPrefBoolean(AI_MODEL_ENABLED, enabled)
        }
        
        @Volatile
        private var instance: AITranslationService? = null
        
        fun getInstance(): AITranslationService? {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        loadModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupModel()
        instance = null
    }

    private fun loadModel() {
        execute {
            try {
                // Initialize ONNX Runtime
                ortEnv = OrtEnvironment.getEnvironment()
                
                // Load tokenizer
                tokenizer = VietnameseChineseTokenizer()
                
                // Try to load from assets first, then fallback to internal storage
                val modelLoadedFromAssets = loadModelFromAssets()
                
                if (!modelLoadedFromAssets) {
                    // Fallback to internal storage
                    val encoderModelPath = "${filesDir.absolutePath}/aimodel/onnx_output/encoder_model.onnx"
                    val decoderModelPath = "${filesDir.absolutePath}/aimodel/onnx_output/decoder_model.onnx"
                    
                    // Check if model files exist
                    if (!java.io.File(encoderModelPath).exists() || !java.io.File(decoderModelPath).exists()) {
                        AppLog.put("AI model files not found at $encoderModelPath or $decoderModelPath")
                        return@execute
                    }
                    
                    // Create ONNX sessions
                    val encoderBytes = java.io.File(encoderModelPath).readBytes()
                    val decoderBytes = java.io.File(decoderModelPath).readBytes()
                    
                    encoderSession = ortEnv?.createSession(encoderBytes)
                    decoderSession = ortEnv?.createSession(decoderBytes)
                }
                
                isModelLoaded = true
                AppLog.put("AI translation model loaded successfully")
                
            } catch (e: Exception) {
                AppLog.put("Failed to load AI translation model: ${e.localizedMessage}", e)
                isModelLoaded = false
            }
        }
    }
    
    private fun loadModelFromAssets(): Boolean {
        return try {
            // Load models from assets
            val encoderInputStream = assets.open("aimodel/encoder_model.onnx")
            val decoderInputStream = assets.open("aimodel/decoder_model.onnx")
            
            val encoderBytes = encoderInputStream.readBytes()
            val decoderBytes = decoderInputStream.readBytes()
            
            encoderInputStream.close()
            decoderInputStream.close()
            
            encoderSession = ortEnv?.createSession(encoderBytes)
            decoderSession = ortEnv?.createSession(decoderBytes)
            
            true
        } catch (e: Exception) {
            AppLog.put("Failed to load models from assets: ${e.localizedMessage}")
            false
        }
    }

    private fun cleanupModel() {
        try {
            encoderSession?.close()
            decoderSession?.close()
            ortEnv?.close()
            encoderSession = null
            decoderSession = null
            ortEnv = null
            tokenizer = null
            isModelLoaded = false
        } catch (e: Exception) {
            AppLog.put("Error cleaning up AI model: ${e.localizedMessage}", e)
        }
    }

    suspend fun translateVietnameseToChinese(vietnameseText: String): String {
        return withContext(Dispatchers.IO) {
            if (!isModelLoaded || tokenizer == null || encoderSession == null || decoderSession == null) {
                AppLog.put("AI model not loaded, returning original text")
                return@withContext vietnameseText
            }

            try {
                // Tokenize input
                val inputTokens = tokenizer!!.encode(vietnameseText)
                
                // Prepare input tensor for encoder using proper ONNX Runtime API
                val inputShape = longArrayOf(1, inputTokens.size.toLong())
                val inputBuffer = ByteBuffer.allocate(inputTokens.size * 8).order(ByteOrder.nativeOrder())
                inputTokens.forEach { inputBuffer.putLong(it.toLong()) }
                inputBuffer.flip()
                
                val inputTensor = OnnxTensor.createTensor(ortEnv, inputBuffer, inputShape)
                val encoderInputs = mapOf<String, OnnxTensor>("input_ids" to inputTensor)
                
                // Run encoder
                val encoderOutputs = encoderSession?.run(encoderInputs)
                val encoderOutput = encoderOutputs?.get(0) // Get first output tensor
                
                // Run decoder (simplified - you'll need to implement proper autoregressive decoding)
                val outputTokens = runDecoder(encoderOutput, inputTokens)
                
                // Clean up tensor
                inputTensor.close()
                
                // Decode output
                val translatedText = tokenizer!!.decode(outputTokens)
                
                AppLog.put("AI translation: '$vietnameseText' -> '$translatedText'")
                translatedText
                
            } catch (e: Exception) {
                AppLog.put("AI translation failed: ${e.localizedMessage}", e)
                vietnameseText // Return original text on failure
            }
        }
    }

    private suspend fun runDecoder(encoderOutput: Any?, inputTokens: IntArray): IntArray {
        // Simplified decoder implementation - you'll need to implement proper autoregressive decoding
        // This is a placeholder that returns the input tokens
        // In a real implementation, you would:
        // 1. Start with a start token
        // 2. Feed encoder output and previous tokens to decoder
        // 3. Generate next token
        // 4. Repeat until end token or max length
        
        try {
            // Create decoder input (simplified)
            val decoderInputShape = longArrayOf(1, inputTokens.size.toLong())
            val decoderBuffer = ByteBuffer.allocate(inputTokens.size * 8).order(ByteOrder.nativeOrder())
            inputTokens.forEach { decoderBuffer.putLong(it.toLong()) }
            decoderBuffer.flip()
            
            val decoderInputTensor = OnnxTensor.createTensor(ortEnv, decoderBuffer, decoderInputShape)
            val decoderInputs = mapOf<String, OnnxTensor>("input_ids" to decoderInputTensor)
            
            // Run decoder
            val decoderOutputs = decoderSession?.run(decoderInputs)
            val decoderOutput = decoderOutputs?.get(0) // Get first output tensor
            
            // Clean up tensor
            decoderInputTensor.close()
            
            // Convert output to tokens (simplified)
            // In reality, you'd need to apply softmax and argmax to get the next token
            return inputTokens // Placeholder
            
        } catch (e: Exception) {
            AppLog.put("Decoder inference failed: ${e.localizedMessage}", e)
            return inputTokens
        }
    }

    fun isModelReady(): Boolean {
        return isModelLoaded && tokenizer != null && encoderSession != null && decoderSession != null
    }
}

/**
 * Simplified tokenizer for Vietnamese to Chinese translation
 * This is a placeholder implementation. You'll need to implement proper tokenization
 * based on your model's tokenizer files (source.spm, target.spm, vocab.json)
 */
class VietnameseChineseTokenizer {
    
    fun encode(text: String): IntArray {
        // Simplified tokenization - replace with actual implementation
        // using sentencepiece or your tokenizer
        // For now, convert each character to its Unicode code point
        return text.map { it.code }.toIntArray()
    }
    
    fun decode(tokens: IntArray): String {
        // Simplified detokenization - replace with actual implementation
        // For now, convert Unicode code points back to characters
        return tokens.map { 
            if (it > 0 && it < Character.MAX_CODE_POINT) {
                it.toChar()
            } else {
                '?'
            }
        }.joinToString("")
    }
}
