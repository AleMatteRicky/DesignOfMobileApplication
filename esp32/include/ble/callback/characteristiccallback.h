#pragma once

#include <BLEServer.h>
#include "ble/remote_events_handler.h"

namespace ble {
class CharacteristicCallback : public BLECharacteristicCallbacks {
public:
    CharacteristicCallback(RemoteEventsHandler* handler) : m_handler(handler) {}

    void onWrite(BLECharacteristic* pCharacteristic,
                 esp_ble_gatts_cb_param_t* param) {
        std::string value(pCharacteristic->getValue().c_str());
        m_handler->onCharacteristicChange(value);
    }

private:
    RemoteEventsHandler* m_handler;
};
}  // namespace ble