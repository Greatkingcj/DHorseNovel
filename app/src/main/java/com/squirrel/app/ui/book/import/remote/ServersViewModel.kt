package com.squirrel.app.ui.book.import.remote

import android.app.Application
import com.squirrel.base.base.BaseViewModel
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.Server

class ServersViewModel(application: Application): BaseViewModel(application) {


    fun delete(server: Server) {
        execute {
            appDb.serverDao.delete(server)
        }
    }

}