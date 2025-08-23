#pragma once

#include <Arduino.h>
#include <BLEServer.h>
#include "ble/remote_events_handler.h"

namespace ble {
class BondingCallaback : public BLESecurityCallbacks {
public:
    BondingCallaback(RemoteEventsHandler* handler) : m_handler(handler) {}
    /*
    The esp32 is asked to return the password displayed by the android app. It
    is useful if the android displays the passkey and the esp32 inserts it, but
    in our case it will be the other way around.
    */
    uint32_t onPassKeyRequest() { return 12345; }

    /*
     * Notify this device about the generated pass_key to be displayed in order
     * for the user to input it on the smartphone. This function is called
     * because the ESP32 has only output capabilities (ESP_IO_CAP_OUT) and the
     * smartphone can handle the user's input.
     */
    void onPassKeyNotify(uint32_t pass_key) {
        m_handler->onBondingStateChange(
            BondingState{.phase = BondingState::BONDING, .passkey = pass_key});

        Serial.println("========================================");
        Serial.println("🔐 NEW BLE PAIRING IN PROGRESS");
        Serial.println("========================================");
        Serial.printf("PASSKEY: %06lu\n", pass_key);
        Serial.println("========================================");
        Serial.println("Enter this 6-digit passkey on your smartphone:");
        Serial.printf(">>> %06lu <<<\n", pass_key);
        Serial.println("========================================");
        Serial.println("Waiting for passkey confirmation...");
    }

    /* return true whether the user's input to the device matches pass_key, not
     * used for Passkey Enter mode. Not used in our case. */
    bool onConfirmPIN(uint32_t pass_key) { return true; }

    /**
     * Accepts or refuses an incoming a pairing request (e.g. 'bond' on
     * nRFConnect app)
     */
    bool onSecurityRequest() {
        Serial.println("Security request received.");
        return true;
    }

    /**
     * Notifies about the result of the authentication process
     */
    void onAuthenticationComplete(esp_ble_auth_cmpl_t cmpl) {
        if (cmpl.success) {
            m_handler->onBondingStateChange(
                BondingState{.phase = BondingState::BONDED});
            Serial.println("Authentication Success. Device bonded.");
        } else {
            m_handler->onBondingStateChange(
                BondingState{.phase = BondingState::NOTBONDED});

            Serial.printf("Authentication failed\n");
            Serial.printf("\t\t-code: 0x%02X\n", cmpl.fail_reason);
            Serial.printf("\t\t-address: %d\n", cmpl.addr_type);
            Serial.printf("\t\t-authentication mode: %d\n", cmpl.auth_mode);
        }
    }

private:
    RemoteEventsHandler* m_handler;
};

}  // namespace ble