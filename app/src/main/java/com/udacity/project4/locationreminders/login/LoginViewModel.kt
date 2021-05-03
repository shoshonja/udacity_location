package com.udacity.project4.locationreminders.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class LoginViewModel: ViewModel() {

    enum class AuthenticationState{
        AUTHENTICATED, UNAUTHENTICATED
    }

    val authenticationState: LiveData<AuthenticationState> = FirebaseUserLiveData().map { firebaseUser ->
        if(firebaseUser != null){
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }
}