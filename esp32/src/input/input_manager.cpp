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
            ESP_LOGD(TAG,
                "Not connected. Please check connections and read the "
                "hookup "
                "guide.");
            delay(1000);
        }
    } else {
        ESP_LOGD(TAG, "Input is ready!");
    }
    delay(1000);

    sensor.setSensitivity(SENSITIVITY_2X);
    ESP_LOGD(TAG, "current sensitivity is %ldX\n", sensor.getSensitivity());
        }

    /*
    The device used to detect input has no double click or press key
    functionality, hence the following mappings will be used
    instead:  - left: pressing right: double click
    */

void InputManager::handleInput() {
    if (sensor.isRightSwipePulled()) {
        notify(SwipeClockwise::name, SwipeClockwise());
        ESP_LOGD(TAG, "Right Swipe");
        return;
    }

    if (sensor.isLeftSwipePulled()) {
        notify(SwipeAntiClockwise::name, SwipeAntiClockwise());
        ESP_LOGD(TAG, "Left Swipe");
        return;
    }

    if (sensor.isLeftTouched()) {
        ESP_LOGD(TAG, "Left");
        notify(Press::name, Press());
        return;
    }

    if (sensor.isMiddleTouched()) {
        notify(Click::name, Click());
        ESP_LOGD(TAG, "Middle");
        return;
    }
}