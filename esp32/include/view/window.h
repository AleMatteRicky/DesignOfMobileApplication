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
    Window(std::unique_ptr<Page>&& firstPage);

    void setPage(std::unique_ptr<Page> page);

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
    byte const m_idxPage;
};
}  // namespace view