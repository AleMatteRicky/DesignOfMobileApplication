#pragma once

#include "view/delay_call.h"
#include "view/image/bin_image_info.h"
#include "view/image/image.h"
#include "view/screen/screen.h"
#include "view/tft.h"
#include "view/view.h"

namespace view {
/**
 * Class representing a notification of an event coming from remote.
 * When the notification arrives, the View becomes visible.
 */
class Notification : public View {
public:
    Notification(RectType frame,
                 View* superiorView,
                 BinaryImageInfo imageWhenTheEventIsTriggered,
                 std::string const& nameOfTheTriggeringEvent,
                 int durationOfTheNotificationInMs = 3000)
        : View::View(frame, superiorView, "notification"),
          m_imageWhenTheEventIsTriggered(frame,
                                         nullptr,
                                         {imageWhenTheEventIsTriggered}),
          m_nameOfTheTriggeringEvent(nameOfTheTriggeringEvent),
          m_durationOfTheNotificationInMs(durationOfTheNotificationInMs),
          m_isVisible{false} {}

    ~Notification() override {
        Serial.println("Destroying notification");
        // cancel to avoid the function 'close' be invoked after the object has
        // been destroyed
        m_timer.cancel();
    }

    /**
     * Closes this notification
     */
    void close() {
        m_isVisible = false;
        clearFromScreen();
    }

    void onEvent(ble::CallNotification const& event) override {
        if (event.name == m_nameOfTheTriggeringEvent) {
            doOnEvent();
        }
    }

    void onEvent(ble::MessageNotification const& event) override {
        if (event.name == m_nameOfTheTriggeringEvent) {
            doOnEvent();
        }
    }

    void drawOnScreen() override {
        m_imageWhenTheEventIsTriggered.draw();
    }

private:
    void doOnEvent() {
        m_isVisible = true;
        drawOnScreen();
        m_timer.delay(m_durationOfTheNotificationInMs, [this]() { close(); });
    }

private:
    Image m_imageWhenTheEventIsTriggered;
    std::string m_nameOfTheTriggeringEvent;
    int m_durationOfTheNotificationInMs;
    Timer m_timer;
    bool m_isVisible;
};
}  // namespace view