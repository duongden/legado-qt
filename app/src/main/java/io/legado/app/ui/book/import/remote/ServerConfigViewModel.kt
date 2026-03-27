package io.legado.app.ui.book.import.remote

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Server
import io.legado.app.utils.toastOnUi

class ServerConfigViewModel(application: Application): BaseViewModel(application) {

    var mServer: Server? = null

    fun init(id: Long?, onSuccess: () -> Unit) {
        //mServer not null maybe screen rotation recreated UI, no update needed
        if (mServer != null) return
        execute {
            mServer = if (id != null) {
                appDb.serverDao.get(id)
            } else {
                Server()
            }
        }.onSuccess {
            onSuccess.invoke()
        }
    }

    fun save(server: Server, onSuccess: () -> Unit) {
        execute {
            mServer?.let {
                appDb.serverDao.delete(it)
            }
            mServer = server
            appDb.serverDao.insert(server)
        }.onSuccess {
            onSuccess.invoke()
        }.onError {
            context.toastOnUi("Lỗi khi lưu\n${it.localizedMessage}")
        }
    }

}