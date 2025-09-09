#include "ble/connection_manager.h"
#include <ArduinoJson.h>
#include "ble/callback/bondingcallback.h"
#include "ble/callback/characteristiccallback.h"
#include "ble/callback/servercallback.h"
#include "ble/constants.h"
#include "ble/remote_dispatcher.h"
#include "view/main_event_queue.h"

namespace ble {

ConnectionManager::ConnectionManager()
    : m_connectionState(ConnectionState{ConnectionState::DISCONNECTED}),
      m_bondingState(BondingState{BondingState::NOTBONDED, 0}),
      m_isAdvertising{false},
      m_messageDb{std::make_unique<model::ModelList<std::string>>()} {
    // Create the BLE Device
    BLEDevice::init("ESP32 device");
    // set the maximum supported MTU so that more bytes can be written in one
    // shot
    BLEDevice::setMTU(500);
    setupBonding();
    setupConnectionMonitoring();
    setupCharacteristics();
    ESP_LOGD(TAG, "ConnectionManager setup correctly\n");
}

void ConnectionManager::setupBonding() {
    randomSeed(analogRead(0));

    BLEDevice::setEncryptionLevel(ESP_BLE_SEC_ENCRYPT_MITM);

    BondingCallaback* bondingCallback = new BondingCallaback(this);

    BLEDevice::setSecurityCallbacks(bondingCallback);

    BLESecurity* pSecurity = new BLESecurity();
    // the device can only prints stuff. This is important to find out the
    // mechanism to carry out authentication
    pSecurity->setCapability(ESP_IO_CAP_OUT);
    pSecurity->setAuthenticationMode(ESP_LE_AUTH_REQ_SC_MITM_BOND);
}

void ConnectionManager::setupConnectionMonitoring() {
    m_server = std::unique_ptr<BLEServer>(BLEDevice::createServer());

    ServerCallaback* serverCallback = new ServerCallaback(this);

    m_server->setCallbacks(serverCallback);
}

void ConnectionManager::setupCharacteristics() {
    // Create the NUS BLE Service
    BLEService* pService = m_server->createService(service_uuid);

    m_txCharacteristic =
        std::unique_ptr<BLECharacteristic>(pService->createCharacteristic(
            characteristic_uuid_tx, BLECharacteristic::PROPERTY_INDICATE));

    m_txCharacteristic->addDescriptor(new BLE2902());

    m_rxCharacteristic =
        std::unique_ptr<BLECharacteristic>(pService->createCharacteristic(
            characteristic_uuid_rx, BLECharacteristic::PROPERTY_WRITE));

    CharacteristicCallback* characteristicCallback =
        new CharacteristicCallback(this);

    m_rxCharacteristic->setCallbacks(characteristicCallback);

    // Start the service
    pService->start();
}

void ConnectionManager::advertise() {
    ESP_LOGD(TAG, "Start advertising");
    ConnectionState currentState = m_connectionState.load();
    if (currentState.phase == ConnectionState::CONNECTED) {
        disconnect();
    }
    BLEAdvertising* pAdvertising = m_server->getAdvertising();
    if (m_isAdvertising)
        pAdvertising->stop();
    pAdvertising->addServiceUUID(service_uuid);
    pAdvertising->setScanResponse(true);
    // preferred minimum interval after which two devices exchange info
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMinPreferred(0x12);
    pAdvertising->start();
    m_isAdvertising = true;
}

void ConnectionManager::disconnect() {
    auto connectionId = m_server->getConnId();
    m_server->disconnect(connectionId);
}

void ConnectionManager::onConnectionStateChange(ConnectionState const& ev) {
    if (ev.phase == ConnectionState::CONNECTED) {
        ESP_LOGD(TAG, "Received network event: device is connected\n");
    } else {
        ESP_LOGD(TAG, "Received network event: device is not connected\n");
    }
    m_isAdvertising = false;
    m_connectionState = ev;
    auto dispatcher = RemoteDispatcher::getInstance();
    dispatcher->notify(ConnectionState::name, ConnectionState(ev));
}

void ConnectionManager::onBondingStateChange(BondingState const& ev) {
    m_bondingState = ev;
    auto dispatcher = RemoteDispatcher::getInstance();
    dispatcher->notify(BondingState::name, BondingState(ev));
}

void ConnectionManager::onCharacteristicChange(std::string const& msg) {
    ESP_LOGD(TAG, "A message arrived: '%s'\n", msg.c_str());

    JsonDocument doc;
    DeserializationError error(deserializeJson(doc, msg));
    if (error) {
        ESP_LOGD(TAG, "Error '%s' when deserializing\n", error.c_str());
        return;
    }

    std::string command(doc["command"]);

    auto dispatcher = RemoteDispatcher::getInstance();

    if (command == "n") {
        ESP_LOGD(TAG, "Notification arrived", msg);
        std::string typeOfNotification = doc["source"];

        if (typeOfNotification == "call") {
            ESP_LOGD(TAG, "Call notification arrived");
            dispatcher->notify(CallNotification::name, CallNotification());
        } else {
            m_messageDb->add(msg);
            ESP_LOGD(TAG, "Message notification arrived");
            dispatcher->notify(MessageNotification::name,
                               MessageNotification());
        }
    } else {
        ESP_LOGD(TAG, "notifying about the message");
        dispatcher->notify(UpdateMessage::name, UpdateMessage{msg});
    };
}

}  // namespace ble
