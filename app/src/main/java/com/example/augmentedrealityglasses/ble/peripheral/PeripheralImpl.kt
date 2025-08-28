package com.example.augmentedrealityglasses.ble.peripheral

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.example.augmentedrealityglasses.ble.characteristic.Characteristic
import com.example.augmentedrealityglasses.ble.characteristic.checkBluetoothConnectPermission
import com.example.augmentedrealityglasses.ble.characteristic.readable.ReadableCharacteristicImpl
import com.example.augmentedrealityglasses.ble.characteristic.toCharacteristicProperties
import com.example.augmentedrealityglasses.ble.characteristic.writable.WritableCharacteristicImpl
import com.example.augmentedrealityglasses.ble.peripheral.bonding.BondState
import com.example.augmentedrealityglasses.ble.peripheral.bonding.BondingReceiver
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.MtuEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ServiceDiscoveredEvent
import com.example.augmentedrealityglasses.ble.service.Service
import com.example.augmentedrealityglasses.ble.service.ServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class PeripheralImpl(
    val device: BluetoothDevice,
    val context: Context,
    val bluetoothGattCallback: BluetoothGattCallback = BluetoothGattCallbackImpl(device),
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : Peripheral {

    companion object {
        private val TAG = PeripheralImpl::class.simpleName
    }

    override val mac: String = device.address

    override val name: String?

    private val _areServicesAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val areServicesAvailable: StateFlow<Boolean> = _areServicesAvailable.asStateFlow()

    val bondingReceiver: BondingReceiver = BondingReceiver(mac)

    private var _mtu: Int = 23

    private val _connectionState: MutableStateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.Initial)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _bondingState: MutableStateFlow<BondState> = MutableStateFlow(BondState.Unknown)
    override val bondingState: StateFlow<BondState> = _bondingState.asStateFlow()

    private var services: List<Service> = emptyList()
    private val isDiscoveringServices: AtomicBoolean = AtomicBoolean(false)

    var gatt: BluetoothGatt? = null

    private var consumerBondingState: Job? = null
    private var consumerConnectionState: Job? = null

    init {
        checkBluetoothConnectPermission(context)
        name = device.name ?: ""

        setup()
    }

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
        gatt?.close()
        scope.cancel()
        context.unregisterReceiver(bondingReceiver)
        _connectionState.tryEmit(ConnectionState.Closed)
    }

    override fun close() {
        cleanup()
    }

    override fun disconnect() {
        if (connectionState.value is ConnectionState.Disconnected) {
            Log.d(TAG, "Received disconnection, despite the peripheral being already disconnected")
            return
        }

        checkBluetoothConnectPermission(context)
        gatt?.disconnect()
    }

    override fun connect() {
        val connectionAlreadyInProgress = _connectionState.value !is ConnectionState.Initial

        if (connectionAlreadyInProgress) {
            Log.d(
                TAG,
                "Received connection request despite a connection is still in progress: do nothing"
            )
            return
        }

        checkBluetoothConnectPermission(context)

        /*
          hack to show the popup.
          Since in some devices (not Samsung), a popup is displayed if a discovery was issued in the last 60 seconds,
          we can pretend this happened by launching the discovery here and then connecting to the gatt
         */
        if (!Build.MANUFACTURER.equals("samsung")) {
            val bluetoothManager: BluetoothManager =
                checkNotNull(context.getSystemService(BluetoothManager::class.java))
            val adapter: BluetoothAdapter? = bluetoothManager.adapter
            adapter?.startDiscovery()
            runBlocking {
                delay(500.milliseconds)
            }
            adapter?.cancelDiscovery()
        }

        // always specify the preferred transport method
        gatt = device.connectGatt(context, false, bluetoothGattCallback, TRANSPORT_LE)
    }

    private fun serviceSanityCheck(serviceUUID: UUID): Service {
        if (!_areServicesAvailable.value) {
            throw IllegalStateException("Service must be discovered first")
        }

        val service = services.find { it.uuid == serviceUUID }

        if (service == null) {
            throw IllegalStateException("Service UUID: $serviceUUID is unknown")
        }

        return service
    }

    override suspend fun send(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        value: ByteArray
    ) {
        val service = serviceSanityCheck(serviceUUID)
        service.writeCharacteristic(characteristicUUID, value)
    }

    override suspend fun subscribe(
        serviceUUID: UUID,
        characteristicUUID: UUID
    ): Flow<String> {
        val service = serviceSanityCheck(serviceUUID)
        return service.subscribeCharacteristic(characteristicUUID).map { it.decodeToString() }
    }

    override suspend fun discoverServices() {
        // discovery can only happen after the device has been connected.
        if (_connectionState.value !is ConnectionState.Connected) {
            return
        }

        /* someone is already trying to discover the service, to not invalidate its result return. It must be atomic get and set, otherwise
         both threads might think that discovery is possible causing data races */
        if (isDiscoveringServices.getAndSet(true)) {
            return
        }

        // we are sure no other thread is in here, so we can discover services

        /*
            The check for services' availability must happen here and not before otherwise a thread might trigger a discovery despite
            the previous one already discovered the services
         */
        if (_areServicesAvailable.value) {
            isDiscoveringServices.set(false)
            return
        }

        checkBluetoothConnectPermission(context)

        var startingDiscovering = true
        // the services need to be discovered again, so set the flag to false and wait for the process to complete
        _areServicesAvailable.tryEmit(false)
        val resultOfDiscovery: List<Service>? =
            bluetoothGattCallback.events
                .onSubscription {
                    startingDiscovering = gatt!!.discoverServices()
                }
                // the service started correctly and no disconnection in the meanwhile
                .takeWhile { !it.isDisconnectionEvent && startingDiscovering }
                // value refers to service discovered event
                .filterIsInstance<ServiceDiscoveredEvent>()
                // get the list of services or null in case any of the upstream operations failed
                .map {
                    it.services
                }
                .firstOrNull()
                // create a list of Service
                ?.map {
                    val writableCharacteristics = it.characteristics
                        .filter { characteristic ->
                            Characteristic.isWritable(characteristic.properties.toCharacteristicProperties())
                        }
                        .map { bleCharacteristic ->
                            WritableCharacteristicImpl(
                                bleCharacteristic.uuid,
                                _mtu,
                                bluetoothGattCallback.events,
                                gatt!!,
                                bleCharacteristic,
                                context,
                                scope
                            )
                        }

                    val readableCharacteristics = it.characteristics
                        .filter { characteristic ->
                            // I have not negated !isWritable because in general a characteristic can be both readable and writable
                            Characteristic.isReadable(characteristic.properties.toCharacteristicProperties())
                        }
                        .map { bleCharacteristic ->
                            ReadableCharacteristicImpl(
                                bleCharacteristic.uuid,
                                _mtu,
                                bluetoothGattCallback.events,
                                gatt!!,
                                bleCharacteristic,
                                bleCharacteristic.descriptors.toSet(),
                                context,
                            )
                        }

                    ServiceImpl(
                        it,
                        writableCharacteristics,
                        readableCharacteristics
                    )
                }

        // the discovery failed, nothing can be done: disconnect
        val wasTheDiscoveryASuccess = resultOfDiscovery != null
        if (!wasTheDiscoveryASuccess) {
            disconnect()
            services = mutableListOf()
        } else {
            services = resultOfDiscovery!!
        }

        _areServicesAvailable.emit(wasTheDiscoveryASuccess)
        isDiscoveringServices.set(false)
    }

    override suspend fun requestMtu(mtu: Int) {
        checkBluetoothConnectPermission(context)
        val mtuEvent = bluetoothGattCallback.events
            .onSubscription {
                gatt!!.requestMtu(mtu)
            }.takeWhile { !it.isDisconnectionEvent }
            .filterIsInstance<MtuEvent>()
            .firstOrNull()
        if (mtuEvent != null && mtuEvent.isValid) {
            _mtu = mtuEvent.mtu
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is Peripheral && mac == other.mac
    }

    override fun toString(): String {
        return "Peripheral: {mac = $mac, name = $name}"
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + context.hashCode()
        result = 31 * result + bluetoothGattCallback.hashCode()
        result = 31 * result + scope.hashCode()
        result = 31 * result + (TAG?.hashCode() ?: 0)
        result = 31 * result + mac.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + bondingReceiver.hashCode()
        result = 31 * result + _connectionState.hashCode()
        result = 31 * result + connectionState.hashCode()
        result = 31 * result + _bondingState.hashCode()
        result = 31 * result + bondingState.hashCode()
        result = 31 * result + services.hashCode()
        result = 31 * result + gatt.hashCode()
        result = 31 * result + (consumerBondingState?.hashCode() ?: 0)
        result = 31 * result + (consumerConnectionState?.hashCode() ?: 0)
        return result
    }
}
