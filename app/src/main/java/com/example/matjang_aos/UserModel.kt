package com.example.matjang_aos

import androidx.lifecycle.ViewModel

enum class Type {
    Kakao, Guest
}

class UserModel{

    constructor(email: String?, type: Type){
        this.email = email
        this.type = type
    }
    val email: String?
    val type: Type



}