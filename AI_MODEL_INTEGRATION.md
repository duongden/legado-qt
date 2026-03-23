# AI Model Integration - Vietnamese to Chinese Translation

## Overview

This integration adds AI-powered Vietnamese to Chinese translation functionality to the Legado app search feature. When enabled, the app will automatically translate Vietnamese search queries into Chinese before performing the search.

## Features

- **Toggle AI Translation**: Users can enable/disable AI translation from the search menu
- **Automatic Translation**: When enabled, Vietnamese queries are automatically translated to Chinese
- **Fallback Support**: If the AI model fails to load or encounters an error, the original search query is used
- **Model Loading**: Models are loaded from app assets first, with fallback to internal storage

## Architecture

### Components

1. **AITranslationService**: Service class that manages ONNX model loading and inference
2. **VietnameseChineseTokenizer**: Handles tokenization and detokenization
3. **SearchActivity Integration**: Modified to use AI translation when enabled
4. **Menu Integration**: Added toggle option in search menu

### Model Files

The integration uses ONNX models located in:
- Primary: `app/src/main/assets/aimodel/`
- Fallback: `app/files/aimodel/onnx_output/`

Required model files:
- `encoder_model.onnx`
- `decoder_model.onnx`
- `source.spm` (Vietnamese tokenizer)
- `target.spm` (Chinese tokenizer)
- `vocab.json`
- `config.json`
- `tokenizer_config.json`

## Implementation Details

### Dependencies

Added to `app/build.gradle`:
```gradle
//ONNX Runtime for AI model inference
implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.16.3'
```

### Preference Key

Added to `PreferKey.kt`:
```kotlin
const val aiModelEnabled = "aiModelEnabled"
```

### Menu Integration

Added to `menu/book_search.xml`:
```xml
<item
    android:id="@+id/menu_ai_translation"
    android:checkable="true"
    android:title="@string/ai_translation"
    app:showAsAction="never" />
```

### String Resources

Added to `values/strings.xml`:
```xml
<string name="ai_translation">AI Translation (Vietnamese to Chinese)</string>
```

## Usage

1. **Enable AI Translation**:
   - Open the search screen
   - Tap the menu button (three dots)
   - Select "AI Translation (Vietnamese to Chinese)"
   - The option will be checked when enabled

2. **Search with Translation**:
   - With AI translation enabled, type a Vietnamese search query
   - The app will automatically translate it to Chinese and perform the search
   - Results will be based on the translated Chinese query

3. **Disable AI Translation**:
   - Uncheck the "AI Translation" option in the menu
   - Search will use the original query without translation

## Technical Notes

### Model Loading Process

1. Service initializes ONNX Runtime environment
2. Attempts to load models from assets folder
3. Falls back to internal storage if assets loading fails
4. Creates encoder and decoder inference sessions
5. Initializes tokenizer

### Translation Process

1. Input Vietnamese text is tokenized
2. Tokens are fed to encoder model
3. Encoder output is used by decoder model
4. Decoder generates Chinese tokens
5. Tokens are detokenized to produce Chinese text

### Error Handling

- Graceful fallback to original query if AI model fails
- Comprehensive error logging for debugging
- Model loading status tracking

## Limitations and Future Improvements

### Current Limitations

1. **Simplified Tokenizer**: Current implementation uses basic character-level tokenization
2. **Basic Decoder**: Decoder implementation is simplified and may not produce optimal results
3. **No Caching**: Translation results are not cached
4. **Model Size**: Large model files may impact app size and loading time

### Recommended Improvements

1. **Proper Tokenizer Implementation**:
   - Integrate SentencePiece tokenizer
   - Use actual vocabulary from model files
   - Implement proper special token handling

2. **Enhanced Decoder**:
   - Implement autoregressive decoding
   - Add beam search for better results
   - Handle start/end tokens properly

3. **Performance Optimization**:
   - Add result caching
   - Implement model quantization
   - Optimize tensor operations

4. **User Experience**:
   - Add loading indicators for translation
   - Show translation preview
   - Add translation history

## Troubleshooting

### Common Issues

1. **Model Loading Fails**:
   - Check if model files exist in assets folder
   - Verify ONNX Runtime dependency is properly added
   - Check app logs for detailed error messages

2. **Translation Quality Poor**:
   - Current tokenizer is basic - upgrade to proper implementation
   - Decoder may need proper autoregressive implementation
   - Model may require fine-tuning for better results

3. **App Size Issues**:
   - Consider using model quantization
   - Implement on-demand model downloading
   - Use model compression techniques

### Debug Logging

Enable debug logging to monitor AI model performance:
- Check AppLog for model loading status
- Monitor translation requests and results
- Track inference timing and errors

## Development Notes

When modifying the AI integration:

1. Always test with both enabled and disabled states
2. Verify fallback behavior works correctly
3. Monitor memory usage and performance
4. Test with various Vietnamese input texts
5. Ensure proper cleanup in service destruction

## File Structure

```
app/
├── src/main/
│   ├── java/io/legado/app/service/
│   │   └── AITranslationService.kt
│   ├── res/
│   │   ├── menu/book_search.xml
│   │   └── values/strings.xml
│   └── assets/aimodel/
│       ├── encoder_model.onnx
│       ├── decoder_model.onnx
│       ├── source.spm
│       ├── target.spm
│       ├── vocab.json
│       └── ...
└── build.gradle
```
