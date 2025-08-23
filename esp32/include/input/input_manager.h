#pragma once

#include <string>
#include "SparkFun_CAP1203.h"
#include "input/input_events.h"
#include "notifications/notification_manager.h"

#include <Wire.h>

class InputManager : public NotificationManagerImpl<Press,
                                                    Click,
                                                    SwipeClockwise,
                                                    SwipeAntiClockwise,
                                                    DoubleClick> {
public:
    InputManager();

    void handleInput();

    static InputManager* getInstance();

    InputManager(const InputManager&) = delete;

    InputManager& operator=(const InputManager&) = delete;

private:
    CAP1203 sensor;

    static InputManager* instance;

    //    std::thread inputThread;
    //    std::atomic_bool isActive;
};