#pragma once

#include <string>
#include "view/page/type.h"

namespace ble {

struct BondingState {
    inline static char const name[] = "bonding_state";
    enum { BONDED, NOTBONDED, BONDING } phase;
    uint32_t passkey;
};

struct ConnectionState {
    inline static char const name[] = "connection_state";
    enum { CONNECTED, DISCONNECTED } phase;
};

struct UpdateMessage {
    inline static char const name[] = "message_from_remote";
    std::string const msg;
};

struct MessageNotification {
    inline static char const name[] = "message_notification";
};

struct CallNotification {
    inline static char const name[] = "call_notification";
};
}