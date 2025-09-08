#pragma once

#include <string>
#include "SparkFun_CAP1203.h"
#include "input/input_events.h"
#include "notifications/notification_manager.h"

#include <Wire.h>

class InputManager : public NotificationManagerImpl<Press,
                                                    Click,
                                                    SwipeClockwise,
                                                    SwipeAntiClockwise> {
public:
    InputManager();

    void handleInput();

    static InputManager* getInstance();

    InputManager(const InputManager&) = delete;

    InputManager& operator=(const InputManager&) = delete;

private:
    inline static char const TAG[] = "InputManager";

private:
    CAP1203 sensor;

    static InputManager* instance;

    //    std::thread inputThread;
    //    std::atomic_bool isActive;
};

/*
private:
    enum USER_ACTION {
        NONE,
        LEFT,
        MIDDLE,
        RIGHT,
        SWIPE_CLOCKWISE,
        SWIPE_ANTI_CLOCKWISE
    };

    USER_ACTION detectTouchFromLeftToRight();
    USER_ACTION detectTouchFromRightToLeft();
*/