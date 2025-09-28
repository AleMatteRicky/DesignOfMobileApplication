package com.example.augmentedrealityglasses.ble

fun checkOtherwiseExec(condition: Boolean, block: () -> Unit) {
    if (!condition)
        return block()
}