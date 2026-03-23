package io.legado.app.service

import android.content.Context
import android.content.Intent
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import io.legado.app.base.BaseService
import io.legado.app.constant.AppLog
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.putPrefBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import splitties.init.appCtx
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.exp

/**
 * AI Translation Service for Vietnamese to Chinese translation
 * Uses ONNX Runtime with MarianMT model and SentencePiece tokenizer
 */
class AITranslationService : BaseService() {

    private var ortEnv: OrtEnvironment? = null
    private var encoderSession: OrtSession? = null
    private var decoderSession: OrtSession? = null
    private var tokenizer: SentencePieceTokenizer? = null
    
    @Volatile
    private var isModelLoaded = false
    
    private val inferenceMutex = Mutex()
    
    // Cache for translated queries
    private val translationCache = object : LinkedHashMap<String, String>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 100
        }
    }

    companion object {
        private const val AI_MODEL_ENABLED = "ai_model_enabled"
        private const val MAX_LENGTH = 128
        private const val EOS_TOKEN_ID = 0
        private const val PAD_TOKEN_ID = 39753
        private const val DECODER_START_TOKEN_ID = 39753
        
        @Volatile
        private var instance: AITranslationService? = null
        
        fun isAIEnabled(context: Context): Boolean {
            return context.getPrefBoolean(AI_MODEL_ENABLED, false)
        }
        
        fun setAIEnabled(context: Context, enabled: Boolean) {
            context.putPrefBoolean(AI_MODEL_ENABLED, enabled)
        }
        
        fun getInstance(): AITranslationService? = instance
        
        fun start(context: Context) {
            val intent = Intent(context, AITranslationService::class.java)
            context.startService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, AITranslationService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppLog.put("AITranslationService created")
        loadModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupModel()
        instance = null
        AppLog.put("AITranslationService destroyed")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun loadModel() {
        execute {
            try {
                AppLog.put("Loading AI translation model...")
                
                // Initialize ONNX Runtime
                AppLog.put("Initializing ONNX Runtime...")
                ortEnv = OrtEnvironment.getEnvironment()
                AppLog.put("ONNX Runtime initialized")
                
                // Load tokenizer first (smaller files)
                AppLog.put("Loading tokenizer...")
                tokenizer = SentencePieceTokenizer(this@AITranslationService)
                AppLog.put("Tokenizer ready: ${tokenizer?.isReady()}")
                
                // Load ONNX models from assets (copy to cache first for large files)
                AppLog.put("Loading ONNX models...")
                val modelLoaded = loadModelsFromCache()
                AppLog.put("Models loaded: $modelLoaded")
                
                if (modelLoaded && tokenizer?.isReady() == true) {
                    isModelLoaded = true
                    AppLog.put("AI translation model loaded successfully")
                } else {
                    AppLog.put("Failed to load AI model or tokenizer")
                    isModelLoaded = false
                }
                
            } catch (e: Exception) {
                AppLog.put("Failed to load AI translation model: ${e.localizedMessage}", e)
                e.printStackTrace()
                isModelLoaded = false
            } catch (e: OutOfMemoryError) {
                AppLog.put("OutOfMemoryError loading AI model", Exception(e))
                isModelLoaded = false
            }
        }
    }
    
    /**
     * Copy model from assets to cache and load from file path
     * This is more memory efficient for large models
     */
    private fun loadModelsFromCache(): Boolean {
        return try {
            val cacheDir = java.io.File(cacheDir, "aimodel")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            // Copy and load encoder model
            AppLog.put("Copying encoder model to cache...")
            val encoderFile = copyAssetToCache("aimodel/encoder_model.onnx", "encoder_model.onnx", cacheDir)
            AppLog.put("Loading encoder from: ${encoderFile.absolutePath}")
            encoderSession = ortEnv?.createSession(encoderFile.absolutePath)
            AppLog.put("Encoder model loaded")
            
            // Copy and load decoder model (use standard decoder, not with_past)
            AppLog.put("Copying decoder model to cache...")
            val decoderFile = copyAssetToCache("aimodel/decoder_model.onnx", "decoder_model.onnx", cacheDir)
            AppLog.put("Loading decoder from: ${decoderFile.absolutePath}")
            decoderSession = ortEnv?.createSession(decoderFile.absolutePath)
            AppLog.put("Decoder model loaded")
            
            true
        } catch (e: Exception) {
            AppLog.put("Failed to load models: ${e.localizedMessage}", e)
            e.printStackTrace()
            false
        }
    }
    
    private fun copyAssetToCache(assetPath: String, fileName: String, cacheDir: java.io.File): java.io.File {
        val outFile = java.io.File(cacheDir, fileName)
        if (!outFile.exists()) {
            AppLog.put("Copying $assetPath to ${outFile.absolutePath}...")
            assets.open(assetPath).use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var total = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        total += read
                    }
                    AppLog.put("Copied $total bytes to ${outFile.name}")
                }
            }
        } else {
            AppLog.put("Using cached ${outFile.name}")
        }
        return outFile
    }

    private fun cleanupModel() {
        try {
            encoderSession?.close()
            decoderSession?.close()
            ortEnv?.close()
            tokenizer?.close()
            encoderSession = null
            decoderSession = null
            ortEnv = null
            tokenizer = null
            isModelLoaded = false
            translationCache.clear()
        } catch (e: Exception) {
            AppLog.put("Error cleaning up AI model: ${e.localizedMessage}", e)
        }
    }

    /**
     * Translate Vietnamese text to Chinese
     * Uses greedy decoding for simplicity and speed
     */
    suspend fun translateVietnameseToChinese(vietnameseText: String): String {
        // Check cache first
        translationCache[vietnameseText]?.let { cached ->
            AppLog.put("Cache hit for: '$vietnameseText' -> '$cached'")
            return cached
        }
        
        return withContext(Dispatchers.Default) {
            inferenceMutex.withLock {
                translateInternal(vietnameseText)
            }
        }
    }
    
    private suspend fun translateInternal(vietnameseText: String): String {
        if (!isModelLoaded || tokenizer == null || encoderSession == null || decoderSession == null) {
            AppLog.put("AI model not ready, returning original text")
            return vietnameseText
        }

        try {
            val startTime = System.currentTimeMillis()
            
            // Step 1: Tokenize input
            val inputIds = tokenizer!!.encode(vietnameseText)
            if (inputIds.isEmpty()) {
                AppLog.put("Empty input after tokenization")
                return vietnameseText
            }
            
            // Step 2: Run encoder
            val encoderHiddenStates = runEncoder(inputIds)
            if (encoderHiddenStates == null) {
                AppLog.put("Encoder failed")
                return vietnameseText
            }
            
            // Step 3: Run decoder with greedy decoding
            // Limit output to number of words in input
            val wordCount = vietnameseText.trim().split("\\s+".toRegex()).size
            val maxOutputTokens = wordCount.coerceIn(2, MAX_LENGTH)
            val outputIds = runGreedyDecode(encoderHiddenStates, inputIds.size, maxOutputTokens)
            
            // Step 4: Decode output tokens to text
            var translatedText = tokenizer!!.decode(outputIds)
            
            // Limit output characters to number of input words
            if (translatedText.length > wordCount) {
                translatedText = translatedText.take(wordCount)
            }
            
            val elapsed = System.currentTimeMillis() - startTime
            AppLog.put("Translation: '$vietnameseText' -> '$translatedText' (${elapsed}ms)")
            
            // Cache the result
            if (translatedText.isNotBlank() && translatedText != vietnameseText) {
                translationCache[vietnameseText] = translatedText
            }
            
            return translatedText.ifBlank { vietnameseText }
            
        } catch (e: Exception) {
            AppLog.put("AI translation failed: ${e.localizedMessage}", e)
            return vietnameseText
        }
    }
    
    /**
     * Run encoder to get hidden states
     */
    private fun runEncoder(inputIds: IntArray): FloatArray? {
        try {
            val env = ortEnv ?: return null
            val session = encoderSession ?: return null
            
            // Create input tensors
            val batchSize = 1L
            val seqLength = inputIds.size.toLong()
            
            // input_ids tensor
            val inputIdsLong = inputIds.map { it.toLong() }.toLongArray()
            val inputIdsTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(inputIdsLong),
                longArrayOf(batchSize, seqLength)
            )
            
            // attention_mask tensor (all 1s)
            val attentionMask = LongArray(inputIds.size) { 1L }
            val attentionMaskTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(attentionMask),
                longArrayOf(batchSize, seqLength)
            )
            
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )
            
            // Run encoder
            val outputs = session.run(inputs)
            
            // Get encoder hidden states (last_hidden_state)
            val outputTensor = outputs.get(0) as? OnnxTensor
            val hiddenStates = outputTensor?.floatBuffer?.let { buffer ->
                FloatArray(buffer.remaining()).also { buffer.get(it) }
            }
            
            // Cleanup
            inputIdsTensor.close()
            attentionMaskTensor.close()
            outputs.close()
            
            return hiddenStates
            
        } catch (e: Exception) {
            AppLog.put("Encoder inference failed: ${e.localizedMessage}", e)
            return null
        }
    }
    
    /**
     * Run greedy decoding to generate output tokens
     */
    private fun runGreedyDecode(encoderHiddenStates: FloatArray, encoderSeqLength: Int, maxOutputTokens: Int): IntArray {
        try {
            val env = ortEnv ?: return intArrayOf()
            val session = decoderSession ?: return intArrayOf()
            
            val outputTokens = mutableListOf<Int>()
            var currentToken = DECODER_START_TOKEN_ID
            val effectiveMaxLength = maxOutputTokens.coerceAtMost(MAX_LENGTH)
            
            // Get model hidden size from encoder output
            val hiddenSize = encoderHiddenStates.size / encoderSeqLength
            
            for (step in 0 until effectiveMaxLength) {
                // Prepare decoder inputs
                val decoderInputIds = (listOf(DECODER_START_TOKEN_ID) + outputTokens).map { it.toLong() }.toLongArray()
                val decoderSeqLength = decoderInputIds.size.toLong()
                
                val decoderInputIdsTensor = OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(decoderInputIds),
                    longArrayOf(1L, decoderSeqLength)
                )
                
                // Encoder hidden states tensor
                val encoderHiddenStatesTensor = OnnxTensor.createTensor(
                    env,
                    FloatBuffer.wrap(encoderHiddenStates),
                    longArrayOf(1L, encoderSeqLength.toLong(), hiddenSize.toLong())
                )
                
                // Encoder attention mask
                val encoderAttentionMask = LongArray(encoderSeqLength) { 1L }
                val encoderAttentionMaskTensor = OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(encoderAttentionMask),
                    longArrayOf(1L, encoderSeqLength.toLong())
                )
                
                val inputs = mapOf(
                    "input_ids" to decoderInputIdsTensor,
                    "encoder_hidden_states" to encoderHiddenStatesTensor,
                    "encoder_attention_mask" to encoderAttentionMaskTensor
                )
                
                // Run decoder
                val outputs = session.run(inputs)
                
                // Get logits and find next token (greedy: argmax)
                val logitsTensor = outputs.get(0) as? OnnxTensor
                val logits = logitsTensor?.floatBuffer?.let { buffer ->
                    FloatArray(buffer.remaining()).also { buffer.get(it) }
                }
                
                // Cleanup tensors
                decoderInputIdsTensor.close()
                encoderHiddenStatesTensor.close()
                encoderAttentionMaskTensor.close()
                outputs.close()
                
                if (logits == null) {
                    AppLog.put("Failed to get decoder logits at step $step")
                    break
                }
                
                // Logits shape: [batch=1, seq_len, vocab_size]
                // Get logits for the last token position
                val vocabSize = 39754 // From config
                val seqLen = decoderInputIds.size
                val totalSize = logits.size
                
                // Verify shape matches expectation
                if (totalSize != seqLen * vocabSize) {
                    AppLog.put("Unexpected logits size: $totalSize, expected: ${seqLen * vocabSize}")
                }
                
                // Get last token's logits: starts at (seqLen-1)*vocabSize
                val startIdx = (seqLen - 1) * vocabSize
                val lastTokenLogits = FloatArray(vocabSize)
                for (i in 0 until vocabSize) {
                    lastTokenLogits[i] = logits[startIdx + i]
                }
                
                // Apply bad_words_ids filter (don't generate PAD token)
                lastTokenLogits[PAD_TOKEN_ID] = Float.NEGATIVE_INFINITY
                
                // Apply repetition penalty to already generated tokens
                val repetitionPenalty = 1.2f
                for (token in outputTokens) {
                    if (lastTokenLogits[token] > 0) {
                        lastTokenLogits[token] /= repetitionPenalty
                    } else {
                        lastTokenLogits[token] *= repetitionPenalty
                    }
                }
                
                // Greedy: find argmax
                val nextToken = lastTokenLogits.indices.maxByOrNull { lastTokenLogits[it] } ?: EOS_TOKEN_ID
                
                // Check for EOS
                if (nextToken == EOS_TOKEN_ID) {
                    AppLog.put("EOS reached at step $step")
                    break
                }
                
                // Early stopping: detect repetition (same token 3+ times in a row)
                if (outputTokens.size >= 2 && 
                    outputTokens.takeLast(2).all { it == nextToken }) {
                    AppLog.put("Repetition detected at step $step, stopping")
                    break
                }
                
                outputTokens.add(nextToken)
                currentToken = nextToken
            }
            
            AppLog.put("Generated ${outputTokens.size} tokens: ${outputTokens.take(10)}...")
            return outputTokens.toIntArray()
            
        } catch (e: Exception) {
            AppLog.put("Decoder inference failed: ${e.localizedMessage}", e)
            return intArrayOf()
        }
    }

    fun isModelReady(): Boolean {
        return isModelLoaded && tokenizer?.isReady() == true && encoderSession != null && decoderSession != null
    }
}
