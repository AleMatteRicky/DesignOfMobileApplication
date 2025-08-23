#include "ble/connection_manager.h"
#include "ble/callback/bondingcallback.h"
#include "ble/callback/characteristiccallback.h"
#include "ble/callback/servercallback.h"
#include "ble/constants.h"
#include "ble/remote_dispatcher.h"
#include "view/main_event_queue.h"

namespace ble {

ConnectionManager::ConnectionManager()
    : m_connectionState(ConnectionState{ConnectionState::DISCONNECTED}),
      m_bondingState(BondingState{BondingState::NOTBONDED, 0}) {
    // Create the BLE Device
    BLEDevice::init("ESP32 device");
    setupBonding();
    setupConnectionMonitoring();
    setupCharacteristics();
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
    BLEAdvertising* pAdvertising = m_server->getAdvertising();
    pAdvertising->addServiceUUID(service_uuid);
    pAdvertising->setScanResponse(true);
    // preferred minimum interval after which two devices exchange info
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMinPreferred(0x12);
    pAdvertising->start();
}

void ConnectionManager::onConnectionStateChange(ConnectionState const& ev) {
    auto dispatcher = RemoteDispatcher::getInstance();
    dispatcher->notify(ConnectionState::name, ConnectionState(ev));
}

void ConnectionManager::onBondingStateChange(BondingState const& ev) {
    auto dispatcher = RemoteDispatcher::getInstance();
    dispatcher->notify(BondingState::name, BondingState(ev));
}

void ConnectionManager::onCharacteristicChange(std::string const& msg) {
    // TODO: add msg to the buffer until one arrives contianing '$' (terminal
    // character). From that moment, the content of the buffer can be
    // interpreted as it refers to a valid message and so it can be parsed.

    // TODO: change with the protocol to know what to do
    auto dispatcher = RemoteDispatcher::getInstance();
    if (msg == "page changed") {
        // TODO: add logic for converting the string into the enum. For now
        // let's pretend it is always an HOME
        dispatcher->notify(ChangePage::name, ChangePage{view::PageType::HOME});
    } else {
        dispatcher->notify(UpdateMessage::name, UpdateMessage{msg});
    };
}

}  // namespace ble