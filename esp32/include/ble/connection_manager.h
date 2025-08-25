#pragma once

#include <BLE2902.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>

#include <memory>
#include "ble/remote_events_handler.h"
#include "model/model.h"

namespace ble {
/**
 * Class to manage remote events
 */
class ConnectionManager : public Model, public RemoteEventsHandler {
public:
    ConnectionManager();

    void send(std::string const& msg) override {
        // TODO: check for message's length and performance on synchronous
        // write, alternatively use an apposite thread + threa-safe queue
        m_txCharacteristic->setValue(msg);
    }

    void advertise() override;

    void disconnect() override;

    bool isConnected() override {
        Serial.println("Checking connection");
        return m_connectionState.phase == ConnectionState::CONNECTED;
    }

    void onConnectionStateChange(ConnectionState const&) override;

    void onBondingStateChange(BondingState const&);

    void onCharacteristicChange(std::string const&) override;

private:
    void setupConnectionMonitoring();
    void setupCharacteristics();
    void setupBonding();

private:
    std::unique_ptr<BLEServer> m_server;
    std::unique_ptr<BLECharacteristic> m_rxCharacteristic;
    std::unique_ptr<BLECharacteristic> m_txCharacteristic;

    // messages arrive scattered, thus we need to store them waiting for the
    // terminal character before performing any action
    std::string buffer;

    // TODO: for now let us keep them non thread safe
    bool m_isAdvertising;
    ConnectionState m_connectionState;
    BondingState m_bondingState;
};

}  // namespace ble