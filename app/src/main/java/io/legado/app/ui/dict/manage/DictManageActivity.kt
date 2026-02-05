package io.legado.app.ui.dict.manage

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityDictManageBinding
import io.legado.app.databinding.ItemDictManageBinding
import io.legado.app.model.TranslationLoader
import io.legado.app.utils.DictManager
import io.legado.app.utils.TranslateUtils
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
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

        refreshUI()
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
}
