package com.example.augmentedrealityglasses.ble.scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
@OptIn(ExperimentalCoroutinesApi::class)
class ScannerImplTest {
    private lateinit var context: Context
    private lateinit var adapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var scanner: ScannerImpl

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        adapter = mockk(relaxed = true)
        bluetoothLeScanner = mockk(relaxed = true)
        // use the mock scanner
        every { adapter.bluetoothLeScanner } returns bluetoothLeScanner
        scanner = ScannerImpl(adapter, context)
    }

    @Test
    fun `scan should emit ScanSuccess when device is found`() = runTest {
        // Arrange
        val fakeResult: ScanResult = mockk(relaxed = true)
        val slot = slot<ScanCallback>()
        // when the function is invoked, the callback will be captured and the flag set
        every { bluetoothLeScanner.startScan(capture(slot)) } just Runs

        val flow = scanner.scan(
            timeout = 5.seconds,
            filters = null,
            settings = mockk(relaxed = true)
        )

        // activate the flow: startScan is triggered
        val deferredEvent = async {
            flow.first()
        }

        /*
        since the flow returned by 'scan' is collected on a coroutine, it is not guaranteed that
        the slot contains a value when 'capture' is invoked, thus use a flag to know if it is ready
         */
        while (!slot.isCaptured) {
            println("Waiting for starting scanning")
            // yield to let other coroutines progress
            kotlinx.coroutines.yield()
        }

        slot.captured.onScanResult(0, fakeResult)

        // value got from the flow
        val event = deferredEvent.await()

        // Assert
        assertTrue(event is ScanSuccess)
        assertEquals(event.scanResult, fakeResult)
    }

    @Test
    fun `scan should emit ScanError with the correct status when scan fails`() = runTest {
        val slot = slot<ScanCallback>()
        // when the function is invoked, the callback will be captured and the flag set
        every { bluetoothLeScanner.startScan(capture(slot)) } just Runs

        val flow = scanner.scan(
            timeout = 5.seconds,
            filters = null,
            settings = mockk(relaxed = true)
        )

        // activate the flow: startScan is triggered
        val deferredEvent = async {
            flow.first()
        }

        /*
        since the flow returned by 'scan' is collected on a coroutine, it is not guaranteed that
        the slot contains a value when 'capture' is invoked, thus use a flag to know if it is ready
         */
        while (!slot.isCaptured) {
            println("Waiting for starting scanning")
            // yield to let other coroutines progress
            kotlinx.coroutines.yield()
        }

        val errorCode = 1
        slot.captured.onScanFailed(errorCode)

        // value got from the flow
        val event = deferredEvent.await()

        // Assert
        assertTrue(event is ScanError)
        assertEquals(event.state, errorCode)
    }

    @Test
    fun `scan should terminate and close the flow after the specified duration`() = runTest {
        val slot = slot<ScanCallback>()
        // when the function is invoked, the callback will be captured and the flag set
        every { bluetoothLeScanner.startScan(capture(slot)) } just Runs

        val timeout = 500.milliseconds

        val flow = scanner.scan(
            timeout = timeout,
            filters = null,
            settings = mockk(relaxed = true)
        )

        // activate the flow: startScan is triggered
        val results = mutableListOf<ScanEvent>()
        val job = launch {
            flow.collect { results.add(it) }
        }

        // Advance time to trigger timeout
        testDispatcher.scheduler.advanceTimeBy(timeout.inWholeMilliseconds)
        testDispatcher.scheduler.advanceUntilIdle()

        job.join()

        assertTrue { results.isEmpty() }

        // check the scanner is started and closed correctly
        verify { bluetoothLeScanner.startScan(slot.captured) }
        verify { bluetoothLeScanner.stopScan(slot.captured) }
    }

    @Test
    fun `scan should stop scanning when flow is cancelled`() = runTest {
        val slot = slot<ScanCallback>()
        // when the function is invoked, the callback will be captured and the flag set
        every { bluetoothLeScanner.startScan(capture(slot)) } just Runs

        val timeout = 100.milliseconds

        val flow = scanner.scan(
            timeout = timeout,
            filters = null,
            settings = mockk(relaxed = true)
        )

        val job = launch {
            withTimeoutOrNull(timeout) {
                flow.collect { }
            }
        }

        testDispatcher.scheduler.advanceTimeBy(timeout * 2)
        testDispatcher.scheduler.advanceUntilIdle()
        job.join()

        // Verify
        verify { bluetoothLeScanner.startScan(any<ScanCallback>()) }
        verify { bluetoothLeScanner.stopScan(slot.captured) }
    }

    @Test
    fun `scan should throw SecurityException when BLUETOOTH_SCAN permission is not granted on Android S+`() =
        runTest {
            every {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            } returns PackageManager.PERMISSION_DENIED

            // When & Then
            assertFailsWith<SecurityException> {
                scanner.scan(
                    timeout = 1.seconds,
                    filters = null,
                    settings = mockk(relaxed = true)
                ).first()
            }
        }

    @Test
    fun `multiple scan results should be emitted in the correct order`() = runTest {

        val slot = slot<ScanCallback>()
        var isScanning = false
        every { bluetoothLeScanner.startScan(capture(slot)) } answers {
            isScanning = true
        }

        every { bluetoothLeScanner.stopScan(any<ScanCallback>()) } just Runs

        val mockScanResult: ScanResult = mockk(relaxed = true)
        val mockScanResult2: ScanResult = mockk(relaxed = true)

        // When
        val flow = scanner.scan(
            timeout = 5.seconds,
            filters = null,
            settings = mockk(relaxed = true)
        )

        val results = mutableListOf<ScanEvent>()
        val job = launch {
            flow.take(2).collect {
                println("adding element $it")
                results.add(it)
            }
        }

        while (!isScanning) {
            println("Waiting for starting scanning")

            kotlinx.coroutines.yield()
        }

        slot.captured.onScanResult(
            0,
            mockScanResult
        )

        slot.captured.onScanResult(
            0,
            mockScanResult2
        )

        job.join()

        // Verify
        assertEquals(2, results.size)

        assertTrue(results[0] is ScanSuccess)
        assertTrue(results[1] is ScanSuccess)
        assertEquals(mockScanResult, (results[0] as ScanSuccess).scanResult)
        assertEquals(mockScanResult2, (results[1] as ScanSuccess).scanResult)
    }
}

