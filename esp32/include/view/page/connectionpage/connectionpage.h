#pragma once

#include "view/page/page.h"
#include "view/screen/screen.h"
#include "view/window.h"

namespace view {
class Connectionpage : public Page {
public:
    class Factory {
    public:
        static std::unique_ptr<Connectionpage> create();
    };

    Connectionpage(Window* window, std::function<void(void)> onClick)
        : Page::Page(
              RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
              window),
          m_title("Click for connecting"),
          m_onClick(onClick) {}

    void drawOnScreen() override;

    void onEvent(Click const& event) {
        Serial.println("Received click event, proceed with the advertising\n");
        m_title = "advertising";
        draw();
        m_onClick();
    }

    void onEvent(ble::ConnectionState const& event) override {
        if (event.phase == ble::ConnectionState::CONNECTED)
            m_title = "device connected, proceed with authentication";
        else
            m_title = "disconnection received";

        draw();
    }

    void onEvent(ble::BondingState const& event) override {
        Serial.println("Received update on bonding state");
        switch (event.phase) {
            case ble::BondingState::BONDED:
                m_title = "bonded";
                break;
            case ble::BondingState::BONDING:
                m_title = "bonding";
                if (event.passkey != 0) {
                    m_title =
                        "insert passkey: " + std::to_string(event.passkey);
                }
                break;
            default:
                m_title = "not bonded";
        }
        draw();
    }

    void onEvent(ble::UpdateMessage const& event) override {
        Serial.printf("Received update message: %s\n", event.msg.c_str());
        m_title = event.msg;
        draw();
    }

private:
    // TODO: change with the class text on Teo's push
    std::string m_title;
    std::function<void(void)> m_onClick;
};

}  // namespace view