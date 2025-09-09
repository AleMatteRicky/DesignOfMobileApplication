package com.example.augmentedrealityglasses.ui.theme

import com.example.augmentedrealityglasses.R


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

    DOWNLOAD{
        override fun getID(): Int {
            return R.drawable.download
        }
    },

    DOWNLOAD_COMPLETED{
        override fun getID(): Int {
            return R.drawable.download_completed
        }
    },

    BACK_ARROW{
        override fun getID(): Int {
            return R.drawable.arrow_back
        }
    },

    BLUETOOTH_CONNECTED{
        override fun getID(): Int {
            return R.drawable.bluetooth_connected
        }
    },

    BLUETOOTH_DISABLED{
        override fun getID(): Int {
            return R.drawable.bluetooth_disabled
        }
    },

    ERROR{
        override fun getID(): Int {
            return R.drawable.error
        }
    },

    //<a href="https://www.flaticon.com/free-icons/safety-glasses" title="safety-glasses icons">Safety-glasses icons created by Luvdat - Flaticon</a>
    SMART_GLASSES{
        override fun getID(): Int {
            return R.drawable.smart_glasses_black
        }
    },

    REFRESH{
        override fun getID(): Int {
            return R.drawable.refresh
        }
    };

    abstract fun getID(): Int //todo add a parameter which determines the theme of the app
}