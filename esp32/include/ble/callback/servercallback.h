#pragma once
#include <BLEServer.h>
#include "ble/remote_events_handler.h"

namespace ble {
class ServerCallaback : public BLEServerCallbacks {
public:
    ServerCallaback(RemoteEventsHandler* handler) : m_handler(handler) {}

    void onConnect(BLEServer* pServer) override {
        m_handler->onConnectionStateChange(
            ConnectionState{.phase = ConnectionState::CONNECTED});
    };

    void onDisconnect(BLEServer* pServer) override {
        m_handler->onConnectionStateChange(
            ConnectionState{.phase = ConnectionState::DISCONNECTED});
    }

private:
    RemoteEventsHandler* m_handler;
};
}  // namespace ble