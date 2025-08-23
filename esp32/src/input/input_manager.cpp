#include "input/input_manager.h"

InputManager* InputManager::instance = nullptr;

InputManager* InputManager::getInstance() {
    if (instance)
        return instance;
    instance = new InputManager();
    return instance;
}

InputManager::InputManager() {
    Wire.begin();
    delay(1000);

    // Setup sensor
    if (sensor.begin() == false) {
        while (1) {
            Serial.println(
                "Not connected. Please check connections and read the "
                "hookup "
                "guide.");
            delay(1000);
        }
    } else {
        Serial.println("Connected!");
        delay(2000);
    }

    /*
    inputThread = std::thread([&]() {
        while (isActive) {
            delay(500);
        }
    });
    */
}

void InputManager::handleInput() {
    /*
    The device used to detect input has no double click or press key
    functionality, hence the following mappings will be used
    instead:  - left: pressing right: double click
    */
    if (sensor.isLeftTouched() == true) {
        notify(Press::name, Press());
        Serial.println("Left");
    }

    if (sensor.isMiddleTouched() == true) {
        notify(Click::name, Click());
        Serial.println("Middle");
    }

    if (sensor.isRightTouched() == true) {
        notify(DoubleClick::name, DoubleClick());
        Serial.println("Right");
    }

    if (sensor.isRightSwipePulled() == true) {
        notify(SwipeClockwise::name, SwipeClockwise());
        Serial.println("Right Swipe");
    }

    if (sensor.isLeftSwipePulled() == true) {
        notify(SwipeAntiClockwise::name, SwipeAntiClockwise());
        Serial.println("Left Swipe");
    }
}