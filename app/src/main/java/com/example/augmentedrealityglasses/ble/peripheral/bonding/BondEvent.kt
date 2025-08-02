package com.example.augmentedrealityglasses.ble.peripheral.bonding

import android.bluetooth.BluetoothDevice

data class BondEvent(val bondState: BondState)

sealed class BondState {
    data object Unknown : BondState()

    data object None : BondState()

    data object Bonding : BondState()

    data object Bonded : BondState()

}

fun Int.toBondState(): BondState = when (this) {
    BluetoothDevice.BOND_BONDED -> BondState.Bonded
    BluetoothDevice.BOND_BONDING -> BondState.Bonding
    else -> BondState.None
}