#pragma once
#include "ble/events/remote_events.h"
#include "notifications/notification_manager.h"

namespace ble {
using RemoteDispatcher =
    DistributedNotificationManager<ble::ConnectionState,
                                   ble::BondingState,
                                   ble::ChangePage,
                                   ble::UpdateMessage,
                                   ble::MessageNotification,
                                   ble::CallNotification>;
}