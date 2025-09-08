#pragma once

#include "view/page/page.h"
#include "view/screen/screen.h"
#include "view/text/text_area.h"
#include "view/window.h"

namespace view {
class ConnectionPage : public Page {
public:
    class Factory {
    public:
        static std::unique_ptr<ConnectionPage> create();
    };

    void drawOnScreen() override;

    void onEvent(ble::ConnectionState const&) override;

    void onEvent(ble::BondingState const&) override;

    void onEvent(Click const&) override;

    PageType getType() override { return CONNECTION; }

private:
    inline static char const TAG[] = "ConnectionPage";

private:
    ConnectionPage();
    TextArea* m_text;
};

}  // namespace view