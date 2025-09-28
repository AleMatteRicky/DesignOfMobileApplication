package com.example.augmentedrealityglasses.ble.peripheral.gattevent

data class MtuEvent(val mtu: Int, val isValid : Boolean) : GattEvent(){
}