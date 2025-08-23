#pragma once

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
                 std::string const& nameOfTheTriggeringEvent)
        : View::View(frame, superiorView, "popup"),
          m_imageWhenTheEventIsTriggered(frame,
                                         nullptr,
                                         {imageWhenTheEventIsTriggered}),
          m_nameOfTheTriggeringEvent(nameOfTheTriggeringEvent) {}

    /**
     * Closes this notification
     */
    void close() {
        auto tft = tft::Tft::getTFT_eSPI();
        auto coordinates = getCoordinates();
        auto size = getSize();
        // equivalent to hide the component
        tft->fillRect(coordinates.m_x, coordinates.m_y, size.m_width,
                      size.m_height, TFT_BLACK);
    }

    void onEvent(ble::CallNotification const& event) override {
        if (event.name == m_nameOfTheTriggeringEvent)
            m_imageWhenTheEventIsTriggered.draw();
    }

    void onEvent(ble::MessageNotification const& event) override {
        if (event.name == m_nameOfTheTriggeringEvent)
            m_imageWhenTheEventIsTriggered.draw();
    }

    void draw() override { m_imageWhenTheEventIsTriggered.draw(); }

private:
    Image m_imageWhenTheEventIsTriggered;
    std::string m_nameOfTheTriggeringEvent;
};
}  // namespace view