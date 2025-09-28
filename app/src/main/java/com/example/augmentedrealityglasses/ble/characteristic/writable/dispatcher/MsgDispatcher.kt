package com.example.augmentedrealityglasses.ble.characteristic.writable.dispatcher

import com.example.augmentedrealityglasses.ble.characteristic.writable.message.Message

/**
 * Message dispatcher to send data to the remote device
 *
 * All methods are thread safe
 */
interface MsgDispatcher {
    /**
     * Add a new Message to the queue of messages to send
     */
    suspend fun add(msg: Message)

    /**
     * Close the dispatcher
     */
    suspend fun close()

    /**
     * Get the message at the top of the queue or null if the dispatcher is empty
     * @return the message
     */
    suspend fun getFirst() : Message?

    /**
     * Remove the head of the queue
     */
    suspend fun removeFirst()
}