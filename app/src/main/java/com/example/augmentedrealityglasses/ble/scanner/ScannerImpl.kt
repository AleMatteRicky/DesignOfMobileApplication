package com.example.augmentedrealityglasses.ble.scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.augmentedrealityglasses.ble.checkOtherwiseExec
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

class ScannerImpl(
    private val adapter: BluetoothAdapter,
    private val context: Context,
) : Scanner {

    companion object {
        private val TAG = ScannerImpl::class.simpleName
    }

    override fun scan(
        timeout: Duration,
        filters: List<ScanFilter>?,
        settings: ScanSettings
    ): Flow<ScanEvent> {
        checkScanningPermission()

        return callbackFlow {
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    trySendBlocking(ScanSuccess(result))
                        .onFailure { throwable ->
                            Log.d(
                                TAG,
                                "Received exception: ${throwable}. It may be an internal failure or downstream cancellation"
                            )
                        }
                }

                override fun onScanFailed(errorCode: Int) {
                    trySendBlocking(ScanError(errorCode))
                        .onFailure { throwable ->
                            Log.d(
                                TAG,
                                "Received exception: ${throwable}. It may be an internal failure or downstream cancellation"
                            )
                        }
                    channel.close()
                }
            }

            adapter.bluetoothLeScanner.startScan(scanCallback)

            val timerJob = launch {
                delay(timeout)
                channel.close()
            }

            // close on error or when this
            awaitClose {
                Log.d(TAG, "Closing channel")
                adapter.bluetoothLeScanner.stopScan(scanCallback)
                timerJob.cancel()
            }
        }
    }

    private fun checkScanningPermission() {
        checkOtherwiseExec(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("BLUETOOTH_SCAN permission not granted")
        }
    }
}