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

#if 0
InputManager::USER_ACTION InputManager::detectTouchFromLeftToRight() {
    bool isLeftTouched = false;
    unsigned long startTime = millis();
    while ((millis() - startTime) < 100) {
        if (sensor.isLeftTouched()) {
            isLeftTouched = true;
            break;
        }
    }

    startTime = millis();
    bool isMiddleTouched = false;
    while ((millis() - startTime) < 100) {
        if (sensor.isMiddleTouched()) {
            isMiddleTouched = true;
            break;
        }
    }

    if (!isMiddleTouched) {
        if (isLeftTouched)
            return LEFT;
    } else {
        if (!isLeftTouched)
            return MIDDLE;
    }

    startTime = millis();
    bool isRightSwiped = false;
    while ((millis() - startTime) < 100) {
        if (sensor.isRightTouched() == true) {
            isRightSwiped = true;
            break;
        }
    }

    if (isRightSwiped) {
        // swipe from left->right completed
        if (isLeftTouched) {
            return SWIPE_CLOCKWISE;
        } else {
            return RIGHT;
        }
    }

    return NONE;
}
#endif

#if 0
InputManager::USER_ACTION InputManager::detectTouchFromLeftToRight() {
    bool isLeftTouched = sensor.isLeftTouched();
    bool isMiddleTouched = sensor.isMiddleTouched();
    bool isRightTouched = sensor.isRightTouched();

    if (isLeftTouched) {
        if (!isMiddleTouched)
            return LEFT;
        if (isRightTouched)
            return SWIPE_CLOCKWISE;
        return NONE;
    } else if (isMiddleTouched) {
        return MIDDLE;
    } else {

    }

    bool isLeftTouched = false;
    unsigned long startTime = millis();
    while ((millis() - startTime) < 100) {
        if (sensor.isLeftTouched()) {
            isLeftTouched = true;
            break;
        }
    }

    startTime = millis();
    bool isMiddleTouched = false;
    while ((millis() - startTime) < 100) {
        if (sensor.isMiddleTouched()) {
            isMiddleTouched = true;
            break;
        }
    }

    if (!isMiddleTouched) {
        if (isLeftTouched)
            return LEFT;
    } else {
        if (!isLeftTouched)
            return MIDDLE;
    }

    startTime = millis();
    bool isRightSwiped = false;
    while ((millis() - startTime) < 100) {
        if (sensor.isRightTouched() == true) {
            isRightSwiped = true;
            break;
        }
    }

    if (isRightSwiped) {
        // swipe from left->right completed
        if (isLeftTouched) {
            return SWIPE_CLOCKWISE;
        } else {
            return RIGHT;
        }
    }

    return NONE;
}

InputManager::USER_ACTION InputManager::detectTouchFromRightToLeft() {
    bool isRightTouched = false;

    unsigned long startTime = millis();
    while ((millis() - startTime) < 100) {
        if (sensor.isRightTouched()) {
            isRightTouched = true;
            break;
        }
    }

    startTime = millis();
    bool isMiddleTouched = false;
    while ((millis() - startTime) < 100) {
        if (sensor.isMiddleTouched()) {
            isMiddleTouched = true;
            break;
        }
    }

    if (!isMiddleTouched) {
        if (isRightTouched)
            return RIGHT;
    } else {
        if (!isRightTouched)
            return MIDDLE;
    }

    startTime = millis();
    bool isLeftTouched = false;
    while ((millis() - startTime) < 100) {
        if (sensor.isLeftTouched()) {
            isLeftTouched = true;
            break;
        }
    }

    if (isLeftTouched) {
        // swipe from left->right completed
        if (isRightTouched) {
            return SWIPE_ANTI_CLOCKWISE;
        } else {
            return LEFT;
        }
    }

    return NONE;
}

void InputManager::handleInput() {
    auto action = detectTouchFromLeftToRight();
    if (action == NONE)
        action = detectTouchFromRightToLeft();

    if (action == NONE)
        return;

    switch (action) {
        case LEFT:
            ESP_LOGD(TAG, "Left");
            notify(Press::name, Press());
            break;
        case MIDDLE:
            ESP_LOGD(TAG, "Middle");
            notify(Click::name, Click());
            break;
        case RIGHT:
            ESP_LOGD(TAG, "Right");
            notify(DoubleClick::name, DoubleClick());
            break;
        case SWIPE_CLOCKWISE:
            ESP_LOGD(TAG, "Swipe clockwise");
            notify(SwipeClockwise::name, SwipeClockwise());
            break;
        case SWIPE_ANTI_CLOCKWISE:
            ESP_LOGD(TAG, "Swipe anti clockwise");
            notify(SwipeAntiClockwise::name, SwipeAntiClockwise());
            break;
        default:
            ESP_LOGD(TAG, "No action");
    }
}
#endif
