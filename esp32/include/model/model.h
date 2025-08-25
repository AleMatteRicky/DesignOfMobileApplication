#pragma once

#include "ble/events/remote_events.h"
#include "notifications/notification_manager.h"

class Model {
public:
    virtual bool isConnected() = 0;
    virtual void send(std::string const&) = 0;
    virtual void advertise() = 0;
    virtual void disconnect() = 0;
};