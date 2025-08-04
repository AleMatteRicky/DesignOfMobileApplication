package com.example.augmentedrealityglasses.ble

/**
 * Exception that represents Gatt command failures. For example, when calling
 * gatt.setCharacteristicNotification(...) the command fails immediately
 */
class InvalidGattOperationException(override val message: String? = "Invalid Gatt Operation") :
    Exception()