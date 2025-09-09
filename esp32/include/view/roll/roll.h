#pragma once

#include <memory>
#include <vector>

#include "utility/arithmetic_operations.h"
#include "view/image/image.h"

namespace view {
class Roll : public View {
public:
    Roll(RectType frame, View* superiorView)
        : View::View(frame, superiorView, "roll") {}

    void onEvent(SwipeClockwise const& ev) override {
        changeCenter(1, true);
        draw();
    }

    void onEvent(SwipeAntiClockwise const& ev) override {
        changeCenter(1, false);
        draw();
    }

    void onEvent(Click const& ev) override {
        ESP_LOGD(TAG, "Roll has received a click event");
        // inform about the click only the View that is currently selected
        View& v = getSubViewAtIndex(m_idxImageAtTheCenter);
        ESP_LOGD(TAG, "Click event forwarded to the view at %p\n", &v);
        v.onEvent(ev);
    }

    View& getImageAtIndex(int16_t i, bool clockwise) {
        return getSubViewAtIndex(getIdx(i, clockwise));
    }

protected:
    void drawOnScreen() override;

private:
    int16_t getIdx(int16_t i, bool clockwise) {
        int16_t dir = clockwise ? i : -i;
        return mod(m_idxImageAtTheCenter + dir, getNumSubViews());
    }

    void changeCenter(int16_t i, bool clockwise) {
        m_idxImageAtTheCenter = mod(getIdx(i, clockwise), getNumSubViews());
    }

    std::pair<byte, bool> drawRoll(bool, byte);

private:
    inline static char const TAG[] = "Roll";

private:
    int16_t m_idxImageAtTheCenter = 0;
};
}  // namespace view