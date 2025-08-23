#pragma once

#include "events/remote_events.h"

namespace ble {
class RemoteEventsHandler {
public:
    virtual void onConnectionStateChange(ConnectionState const&) = 0;
    virtual void onBondingStateChange(BondingState const&) = 0;
    virtual void onCharacteristicChange(std::string const&) = 0;
};
}  // namespace ble