package com.example.augmentedrealityglasses.ble.peripheral

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import com.example.augmentedrealityglasses.ble.GattOperationMutex
import com.example.augmentedrealityglasses.ble.characteristic.Characteristic
import com.example.augmentedrealityglasses.ble.characteristic.checkBluetoothConnectPermission
import com.example.augmentedrealityglasses.ble.characteristic.readable.ReadableCharacteristicImpl
import com.example.augmentedrealityglasses.ble.characteristic.toCharacteristicProperties
import com.example.augmentedrealityglasses.ble.characteristic.writable.WritableCharacteristicImpl
import com.example.augmentedrealityglasses.ble.device.CHARACTERISTIC_UUID_RX
import com.example.augmentedrealityglasses.ble.device.SERVICE_UUID
import com.example.augmentedrealityglasses.ble.peripheral.bonding.BondState
import com.example.augmentedrealityglasses.ble.peripheral.bonding.BondingReceiver
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ServiceDiscoveredEvent
import com.example.augmentedrealityglasses.ble.service.Service
import com.example.augmentedrealityglasses.ble.service.ServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class PeripheralImpl(
    val device: BluetoothDevice,
    override val name: String,
    override val paired: Boolean,
    val context: Context,
    val bluetoothGattCallback: BluetoothGattCallback = BluetoothGattCallbackImpl(device),
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : Peripheral {
    private val TAG = PeripheralImpl::class.qualifiedName

    override val mac: String = device.address

    val bondingReceiver: BondingReceiver = BondingReceiver(mac)

    private val _connectionState: MutableStateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.Closed)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _bondingState: MutableStateFlow<BondState> = MutableStateFlow(BondState.Unknown)
    override val bondingState: StateFlow<BondState> = _bondingState

    override var services: List<Service> = emptyList()
        private set

    lateinit var gatt: BluetoothGatt

    private fun setup() {
        scope.launch {
            bondingReceiver.bondingEvent.collect {
                _bondingState.tryEmit(it.bondState)
            }
        }
        // monitor bond state changes
        val filter = IntentFilter(ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        context.registerReceiver(bondingReceiver, filter)

        scope.launch {
            bluetoothGattCallback.events
                .filterIsInstance<ConnectionEvent>()
                .collect {
                    _connectionState.tryEmit(it.state)
                }
        }
    }

    private fun cleanup() {
        checkBluetoothConnectPermission(context)
        gatt.close()
        scope.cancel()
        context.unregisterReceiver(bondingReceiver)
        _connectionState.tryEmit(ConnectionState.Closed)
    }

    override suspend fun close() {
        GattOperationMutex.withLock {
            cleanup()
        }
    }

    override suspend fun disconnect() {
        if (connectionState.value is ConnectionState.Disconnected)
            return

        checkBluetoothConnectPermission(context)
        GattOperationMutex.withLock {
            gatt.disconnect()
        }
    }

    override suspend fun connect() {
        // connection already issued, do nothing
        if (_connectionState.value !is ConnectionState.Closed)
            return

        checkBluetoothConnectPermission(context)

        setup()

        GattOperationMutex.withLock {
            /*
              hack to show the popup.
              Since in some devices, a popup is displayed if a discovery was issued in the last 60 seconds,
              we can pretend this happened by launching the discovery here and then connecting to the gatt
             */
            val bluetoothManager: BluetoothManager =
                checkNotNull(context.getSystemService(BluetoothManager::class.java))
            val adapter: BluetoothAdapter? = bluetoothManager.adapter
            adapter?.startDiscovery()
            delay(1.seconds)
            adapter?.cancelDiscovery()

            // always specify the preferred transport method
            gatt = device.connectGatt(context, false, bluetoothGattCallback, TRANSPORT_LE)
        }
    }

    override suspend fun discoverServices(): List<Service> {
        checkBluetoothConnectPermission(context)
        val resultOfDiscovery: List<Service>? = GattOperationMutex.withLock {
            var startingDiscovering = true
            bluetoothGattCallback.events
                .onSubscription {
                    startingDiscovering = gatt.discoverServices()
                }
                // the service started correctly and no disconnection in the meanwhile
                .takeWhile { !it.isDisconnectionEvent && startingDiscovering }
                // value refers to service discovered event
                .filterIsInstance<ServiceDiscoveredEvent>()
                // the discovery was a success and contains the requested service
                .takeWhile {
                    it.services?.find { service -> service.uuid == SERVICE_UUID } != null
                }
                // get the list of services or null in case any of the upstream operations failed
                .map {
                    it.services
                }
                .firstOrNull()
                // create a list of Service
                ?.map {
                    val writableCharacteristics = it.characteristics
                        .filter { characteristic ->
                            Log.d(TAG, "Properties: ${characteristic.properties}")
                            characteristic.properties.toCharacteristicProperties()
                                .contains(Characteristic.CharacteristicProperty.WRITE)
                        }
                        .map { bleCharacteristic ->
                            WritableCharacteristicImpl(
                                bleCharacteristic.uuid,
                                bluetoothGattCallback.events,
                                gatt,
                                bleCharacteristic,
                                scope,
                                context,
                            )
                        }

                    val readableCharacteristics = it.characteristics
                        .filter { characteristic ->
                            characteristic.properties.toCharacteristicProperties().contains(
                                Characteristic.CharacteristicProperty.READ
                            )
                        }
                        .map { bleCharacteristic ->
                            ReadableCharacteristicImpl(
                                bleCharacteristic.uuid,
                                bluetoothGattCallback.events,
                                gatt,
                                bleCharacteristic,
                                bleCharacteristic.descriptors.toSet(),
                                context,
                                emptySet()
                            )
                        }

                    ServiceImpl(
                        it,
                        writableCharacteristics,
                        readableCharacteristics
                    )
                }
        }

        // the discovery failed, nothing can be done: disconnect
        if (resultOfDiscovery == null) {
            disconnect()
            services = mutableListOf()
        } else {
            val characteristic = services.find { it.uuid == SERVICE_UUID }!!.readableCharacteristics.find { it.uuid ==  CHARACTERISTIC_UUID_RX}
            characteristic!!.setNotify(true)
            services = resultOfDiscovery
        }

        return services
    }

    override fun equals(other: Any?): Boolean {
        return other is Peripheral && mac == other.mac
    }

    override fun toString(): String {
        return "Peripheral: {mac = $mac, name = $name}"
    }

    override fun hashCode(): Int {
        var result = paired.hashCode()
        result = 31 * result + device.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + bluetoothGattCallback.hashCode()
        result = 31 * result + context.hashCode()
        result = 31 * result + scope.hashCode()
        result = 31 * result + (TAG?.hashCode() ?: 0)
        result = 31 * result + mac.hashCode()
        result = 31 * result + bondingReceiver.hashCode()
        result = 31 * result + _connectionState.hashCode()
        result = 31 * result + connectionState.hashCode()
        result = 31 * result + _bondingState.hashCode()
        result = 31 * result + bondingState.hashCode()
        result = 31 * result + services.hashCode()
        result = 31 * result + gatt.hashCode()
        return result
    }
}
