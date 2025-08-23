#pragma once

#include <memory>
#include "input/input_manager.h"
#include "page/page.h"
#include "page/type.h"
#include "view/screen/screen.h"
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

    void draw() {
        View& curPage = getSubViewAtIndex(0);
        curPage.draw();
    }

    void setPage(std::unique_ptr<Page> page) {
        // destroy the current page and add the new one
        if (View::getNumSubViews() > 0)
            View::detach(View::getSubViewAtIndex(0));
        View::appendSubView(std::move(page));
        auto inputManager = InputManager::getInstance();
        inputManager->addObserver(Press::name, this);
        inputManager->addObserver(DoubleClick::name, this);
        draw();
        printTree();
    }

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