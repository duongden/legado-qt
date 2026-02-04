package io.legado.app.ui.qrcode

import com.google.zxing.Result
import com.king.camera.scan.AnalyzeResult
import com.king.camera.scan.CameraScan
import com.king.zxing.BarcodeCameraScanFragment
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.analyze.MultiFormatAnalyzer

class QrCodeFragment : BarcodeCameraScanFragment() {

    override fun initCameraScan(cameraScan: CameraScan<Result>) {
        super.initCameraScan(cameraScan)
        //Init decode config
        val decodeConfig = DecodeConfig()
        //Higher efficiency if only QR code needed, default DecodeFormatManager.DEFAULT_HINTS if not set
        decodeConfig.hints = DecodeFormatManager.QR_CODE_HINTS
        //Set full area recognition, default false
        decodeConfig.isFullAreaScan = true
        //Set recognition area ratio, default 0.8, crop rectangle based on ratio in preview for scan
        decodeConfig.areaRectRatio = 0.8f

        //Set analyzer before preview start, only recognize QR code
        cameraScan.setAnalyzer(MultiFormatAnalyzer(decodeConfig))
    }

    override fun onScanResultCallback(result: AnalyzeResult<Result>) {
        cameraScan.setAnalyzeImage(false)
        (activity as? QrCodeActivity)?.onScanResultCallback(result.result)
    }

}