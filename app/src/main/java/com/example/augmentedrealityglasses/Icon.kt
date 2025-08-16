package com.example.augmentedrealityglasses


//this enum can be useful in order to have multiple variant of the icons in a simple way
enum class Icon {
    MICROPHONE {
        override fun getID(): Int {
            return R.drawable.mic
        }
    },

    STOP{
        override fun getID(): Int {
            return R.drawable.stop
        }
    },

    RIGHT_ARROW{
        override fun getID(): Int {
            return R.drawable.right_arrow
        }
    },

    BACK_ARROW{
        override fun getID(): Int {
            return R.drawable.arrow_back
        }
    };

    abstract fun getID(): Int //todo add a parameter which determines the theme of the app
}