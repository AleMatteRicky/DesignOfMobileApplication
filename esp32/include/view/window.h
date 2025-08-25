#pragma once

#include <memory>
#include "input/input_manager.h"
#include "page/page.h"
#include "page/type.h"
#include "view/screen/screen.h"
#include "view/tft.h"
#include "view/view.h"

namespace view {
class Window : public View {
public:
    Window(std::function<void(PageType)> onChangingPageCallback)
        : View::View(
              RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
              nullptr,
              "window"),
          m_onChangingPageCallback(onChangingPageCallback) {}

    void drawOnScreen() override {
        clearFromScreen();

        for (byte i = 0; i < getNumSubViews(); i++) {
            View& subView = getSubViewAtIndex(i);
            subView.draw();
        }
    }

    void setPage(std::unique_ptr<Page> page);

    void onEvent(Press const& ev) override {
        m_onChangingPageCallback(PageType::HOME);
    }

    void onEvent(DoubleClick const& ev) override {
        m_onChangingPageCallback(PageType::CONNECTION);
    }

private:
    std::function<void(PageType)> m_onChangingPageCallback;
};
}  // namespace view