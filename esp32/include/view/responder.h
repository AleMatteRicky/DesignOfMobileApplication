#pragma once

#include "input/input_events.h"
#include "notifications/notification_manager.h"

namespace view {
class Responder
    : public Observer<Press, Click, SwipeClockwise, SwipeAntiClockwise> {};

}  // namespace view