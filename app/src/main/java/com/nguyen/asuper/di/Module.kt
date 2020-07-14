package com.nguyen.asuper.di

import com.google.firebase.auth.FirebaseAuth
import com.nguyen.asuper.repository.AuthRepository
import com.nguyen.asuper.repository.MainRepository
import com.nguyen.asuper.viewmodels.AuthViewModel
import com.nguyen.asuper.viewmodels.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module



val viewModelModule = module {
    viewModel {
        AuthViewModel(get())
    }
    viewModel {
        MainViewModel(get())
    }

}

val repositoryModule = module {
    single {
        AuthRepository()
    }

    single {
        MainRepository()
    }
}
