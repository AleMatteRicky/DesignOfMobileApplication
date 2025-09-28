package com.example.augmentedrealityglasses.ble.characteristic.writable.dispatcher

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.augmentedrealityglasses.ble.GattOperationMutex
import com.example.augmentedrealityglasses.ble.characteristic.writable.message.Message
import com.example.augmentedrealityglasses.ble.checkOtherwiseExec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Deque
import java.util.LinkedList

class MsgDispatcherImpl(
    val context: Context,
    val limitRetry: UInt = 15U
) : MsgDispatcher {
    private val TAG = MsgDispatcherImpl::class.qualifiedName
    val lock = Mutex()
    val msgQueue: Deque<Message> = LinkedList()
    val job: Job

    init {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                lock.withLock {
                    val m = msgQueue.poll()
                    if (m != null && m.state == Message.State.ToSend) {
                        checkBluetoothConnectPermission()
                        val isFired =
                            GattOperationMutex.withLock {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    m.gatt.writeCharacteristic(
                                        m.characteristic,
                                        m.data,
                                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                    )
                                else {
                                    m.characteristic.writeType =
                                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                    m.characteristic.value = m.data
                                    m.gatt.writeCharacteristic(
                                        m.characteristic
                                    )
                                }
                            }

                        val newMessage: Message
                        if (isFired == BluetoothStatusCodes.SUCCESS) {
                            newMessage = m.copy(state = Message.State.Pending)
                        } else {
                            newMessage = m.copy(state = Message.State.ToSend, nTries = m.nTries + 1U)
                        }

                        if (m.nTries >= limitRetry) {
                            Log.d(TAG, "Message $m is continuously failing")
                        } else {
                            msgQueue.addFirst(newMessage)
                        }
                    }
                }
            }
            delay(200)
        }
    }

    override suspend fun add(msg: Message) {
        lock.withLock {
            msgQueue.add(msg)
        }
    }

    override suspend fun close() {
        Log.d(TAG, "closing the handler")
        lock.withLock {
            msgQueue.clear()
            job.cancel()
        }
    }

    override suspend fun getFirst(): Message? {
        return lock.withLock {
            msgQueue.first
        }
    }

    override suspend fun removeFirst() {
        return lock.withLock {
            msgQueue.poll()
        }
    }

    private fun checkBluetoothConnectPermission() {
        checkOtherwiseExec(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Bluetooth_Connect permission is missing")
        }
    }
}