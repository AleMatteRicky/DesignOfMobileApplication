#pragma once

#include <memory>
#include "input/input_manager.h"
#include "page/page.h"
#include "page/page_factory.h"
#include "page/type.h"
#include "view/screen/screen.h"
#include "view/tft.h"
#include "view/view.h"

namespace view {
class Window : public View {
public:
    Window(std::unique_ptr<PageFactory>&& pageFactory);

    void setPage(PageType pageType);

    void onEvent(Press const& ev) override;

    void onEvent(ble::UpdateMessage const&) override;

protected:
    void drawOnScreen() override {
        clearFromScreen();

        for (byte i = 0; i < getNumSubViews(); i++) {
            View& subView = getSubViewAtIndex(i);
            subView.draw();
        }
    }

private:
    void detachCurrentPage();
    void setPage(std::unique_ptr<Page>&& page);

private:
    inline static char const TAG[] = "Window";

private:
    std::unique_ptr<PageFactory> m_pageFactory;
    Page* m_currentPage;
};
}  // namespace view