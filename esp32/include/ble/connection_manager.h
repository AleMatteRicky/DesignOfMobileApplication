#pragma once

#include <BLE2902.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>

#include <Arduino.h>
#include <esp_log.h>

#include <atomic>
#include <memory>
#include "ble/remote_events_handler.h"
#include "controller/remote_controller.h"
#include "model/model_list.h"

namespace ble {
/**
 * Class to manage remote events
 */
class ConnectionManager : public controller::RemoteController,
                          public RemoteEventsHandler {
public:
    ConnectionManager();

    void send(std::string const& msg) override {
        ESP_LOGD(TAG, "sending the message '%s'\n", msg.c_str());
        m_txCharacteristic->setValue(msg);
    }

    void advertise() override;

    void disconnect() override;

    bool isConnected() override {
        ESP_LOGD(TAG, "Checking connection");
        ConnectionState currentState = m_connectionState.load();
        return currentState.phase == ConnectionState::CONNECTED;
    }

    std::vector<std::string> getMessages() override {
        std::vector<std::string>&& messages = m_messageDb->drop(0);

        // messages are stored from the latest to the recent, invert the order
        size_t numMessages = messages.size();
        for (size_t i = 0; i < numMessages / 2; i++) {
            std::swap(messages[i], messages[numMessages - 1 - i]);
        }
        return messages;
    }

    void onConnectionStateChange(ConnectionState const&) override;

    void onBondingStateChange(BondingState const&) override;

    void onCharacteristicChange(std::string const&) override;

private:
    void setupConnectionMonitoring();
    void setupCharacteristics();
    void setupBonding();

private:
    inline static char const TAG[] = "ConnectionManager";

private:
    std::unique_ptr<BLEServer> m_server;
    std::unique_ptr<BLECharacteristic> m_rxCharacteristic;
    std::unique_ptr<BLECharacteristic> m_txCharacteristic;
    std::unique_ptr<model::ModelList<std::string>> m_messageDb;

    // messages arrive scattered, thus we need to store them waiting for the
    // terminal character before performing any action
    std::string buffer;

    std::atomic<bool> m_isAdvertising;
    std::atomic<ConnectionState> m_connectionState;
    std::atomic<BondingState> m_bondingState;
};

}  // namespace ble