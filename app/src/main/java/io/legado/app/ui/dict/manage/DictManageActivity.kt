package io.legado.app.ui.dict.manage

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityDictManageBinding
import io.legado.app.databinding.ItemDictManageBinding
import io.legado.app.databinding.ItemAiModelDownloadBinding
import io.legado.app.model.TranslationLoader
import io.legado.app.utils.AiModelManager
import io.legado.app.utils.DictManager
import io.legado.app.utils.TranslateUtils
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.activity.result.contract.ActivityResultContracts

class DictManageActivity : BaseActivity<ActivityDictManageBinding>() {

    override val binding by lazy { ActivityDictManageBinding.inflate(layoutInflater) }
    
    // Helper to hold binding and type
    private data class DictItem(
        val binding: ItemDictManageBinding,
        val type: DictManager.DictType,
        val titleRes: Int
    )

    private lateinit var items: List<DictItem>
    private var currentImportType: DictManager.DictType? = null
    private var busyType: DictManager.DictType? = null

    // AI Model download state
    private var downloadJob: Job? = null
    private var isDownloading: Boolean = false

    private val importDictLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            currentImportType?.let { type ->
                lifecycleScope.launch {
                    setBusy(type, true)
                    val ok = withContext(Dispatchers.IO) {
                        DictManager.importDict(this@DictManageActivity, it, type)
                    }
                    if (ok) {
                        toastOnUi(R.string.import_success)
                        withContext(Dispatchers.IO) {
                            TranslationLoader.reloadType(type)
                            TranslationLoader.prebuildType(type)
                            TranslateUtils.clearCache()
                        }
                    } else {
                        toastOnUi(R.string.import_fail)
                    }
                    setBusy(type, false)
                    refreshUI()
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.title = getString(R.string.custom_dict_manage)
        
        items = listOf(
            DictItem(binding.itemNames, DictManager.DictType.NAMES, R.string.import_names),
            DictItem(binding.itemVietphrase, DictManager.DictType.VIETPHRASE, R.string.import_vietphrase),
            DictItem(binding.itemPhienam, DictManager.DictType.PHIENAM, R.string.import_phienam)
        )

        items.forEach { item ->
            item.binding.tvTitle.text = getString(item.titleRes)
            item.binding.btnImport.setOnClickListener {
                currentImportType = item.type
                importDictLauncher.launch("text/plain")
            }
            item.binding.btnReset.setOnClickListener {
                lifecycleScope.launch {
                    setBusy(item.type, true)
                    val ok = withContext(Dispatchers.IO) {
                        DictManager.deleteCustomDict(item.type)
                    }
                    if (ok) {
                        toastOnUi(R.string.delete_success)
                        withContext(Dispatchers.IO) {
                            TranslationLoader.reloadType(item.type)
                            TranslationLoader.prebuildType(item.type)
                            TranslateUtils.clearCache()
                        }
                    }
                    setBusy(item.type, false)
                    refreshUI()
                }
            }
        }

        setupAiModelSection()
        refreshUI()
    }

    private fun setupAiModelSection() {
        val aiBinding = binding.itemAiModel

        aiBinding.btnAiDownload.setOnClickListener {
            if (isDownloading) {
                // Cancel download
                downloadJob?.cancel()
                downloadJob = null
                isDownloading = false
                refreshAiModelUI()
                toastOnUi(R.string.ai_model_download_cancelled)
            } else {
                startDownload(aiBinding)
            }
        }

        aiBinding.btnAiDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(R.string.ai_model_delete_confirm)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            AiModelManager.deleteModels(this@DictManageActivity)
                        }
                        toastOnUi(R.string.ai_model_delete_success)
                        refreshAiModelUI()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        refreshAiModelUI()
    }

    private fun startDownload(aiBinding: ItemAiModelDownloadBinding) {
        isDownloading = true
        refreshAiModelUI()

        downloadJob = lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                AiModelManager.downloadModels(
                    context = this@DictManageActivity,
                    onFileStart = { fileName, index ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            val statusText = getString(
                                R.string.ai_model_status_downloading,
                                index + 1,
                                AiModelManager.MODEL_FILES.size,
                                fileName
                            )
                            aiBinding.tvAiStatus.text = statusText
                        }
                    },
                    onProgress = { fileIndex, totalFiles, filePercent, overallPercent ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            // Update progress bar
                            aiBinding.layoutAiProgress.visibility = View.VISIBLE
                            aiBinding.progressAiDownload.progress = overallPercent

                            val fileName = AiModelManager.MODEL_FILES.getOrElse(fileIndex) { "" }
                            val detail = getString(
                                R.string.ai_model_status_downloading,
                                fileIndex + 1,
                                totalFiles,
                                fileName
                            ) + " (${getString(R.string.ai_model_downloading_percent, filePercent)})"
                            aiBinding.tvAiProgressDetail.text = detail
                        }
                    },
                    shouldCancel = { !isDownloading }
                )
            }

            isDownloading = false

            if (success) {
                toastOnUi(R.string.ai_model_download_success)
            } else if (downloadJob?.isCancelled == false) {
                // Not cancelled, so it's an error
                toastOnUi(getString(R.string.ai_model_download_error, "Network error"))
            }

            downloadJob = null
            refreshAiModelUI()
        }
    }

    private fun refreshAiModelUI() {
        val aiBinding = binding.itemAiModel
        val isReady = AiModelManager.isModelReady(this)

        // Progress layout
        aiBinding.layoutAiProgress.visibility = if (isDownloading) View.VISIBLE else View.GONE
        aiBinding.progressAiSpinner.visibility = if (isDownloading && aiBinding.progressAiDownload.progress == 0) View.VISIBLE else View.GONE

        // Download button: changes to "Cancel" icon/action when downloading
        aiBinding.btnAiDownload.visibility = View.VISIBLE
        if (isDownloading) {
            aiBinding.btnAiDownload.setImageResource(R.drawable.ic_baseline_close)
            aiBinding.btnAiDownload.contentDescription = getString(R.string.close)
        } else {
            aiBinding.btnAiDownload.setImageResource(R.drawable.ic_download_line)
            aiBinding.btnAiDownload.contentDescription = getString(R.string.download)
            // Hide spinner and progress when not downloading
            aiBinding.progressAiSpinner.visibility = View.GONE
        }

        // Delete button
        aiBinding.btnAiDelete.visibility = if (isReady && !isDownloading) View.VISIBLE else View.GONE

        // Status text
        if (!isDownloading) {
            if (isReady) {
                aiBinding.tvAiStatus.text = getString(R.string.ai_model_status_ready)
                aiBinding.tvAiStatus.setTextColor(android.graphics.Color.parseColor("#43A047"))
            } else {
                aiBinding.tvAiStatus.text = getString(R.string.ai_model_status_not_downloaded)
                aiBinding.tvAiStatus.setTextColor(android.graphics.Color.GRAY)
            }
        }
    }

    private fun refreshUI() {
        items.forEach { item ->
            val hasCustom = DictManager.hasCustomDict(item.type)
            val isBusy = busyType == item.type

            item.binding.progressImport.visibility = if (isBusy) View.VISIBLE else View.GONE
            item.binding.btnImport.visibility = if (isBusy) View.GONE else View.VISIBLE
            item.binding.btnReset.isEnabled = !isBusy
            item.binding.btnImport.isEnabled = !isBusy

            if (hasCustom) {
                item.binding.tvStatus.text = "Đang dùng: Từ điển tùy chỉnh"
                item.binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#43A047"))
                item.binding.btnReset.visibility = if (isBusy) View.GONE else View.VISIBLE
            } else {
                item.binding.tvStatus.text = "Đang dùng: Mặc định (tích hợp sẵn)"
                item.binding.tvStatus.setTextColor(android.graphics.Color.GRAY)
                item.binding.btnReset.visibility = View.GONE
            }
        }
    }

    private fun setBusy(type: DictManager.DictType, busy: Boolean) {
        busyType = if (busy) type else null
        refreshUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel any active download when activity is destroyed
        downloadJob?.cancel()
    }
}
