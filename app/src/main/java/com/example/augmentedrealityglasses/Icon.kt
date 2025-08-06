package com.example.augmentedrealityglasses


//this enum can be useful in order to have multiple variant of the icons in a simple way
enum class Icon {
    MICROPHONE {
        override fun getID(): Int {
            return R.drawable.mic
        }
    };

    abstract fun getID(): Int //todo add a parameter which determines the theme of the app
}