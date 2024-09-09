package com.lepu.pc700.net

import com.lepu.pc700.net.remote.Api
import com.lepu.pc700.net.remote.Repository
import com.lepu.pc700.net.remote.createWebService
import com.lepu.pc700.net.remote.getOkHttpClient
import com.lepu.pc700.net.util.Constant
import com.lepu.pc700.net.vm.GetPDFViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 *
 * java类作用描述
 * zrj 2021/6/26 10:44
 * 更新者 2021/6/26 10:44
 */

val viewModelModule = module {
    viewModel { GetPDFViewModel(get()) }
}


val remoteModule = module {
    single { getOkHttpClient() }
    single { createWebService<Api>(get(), Constant.AI_BASE_URL) }
}

val repositoryModule = module {
    single { Repository(get()) }
}

val appModule = listOf(viewModelModule, remoteModule, repositoryModule)


