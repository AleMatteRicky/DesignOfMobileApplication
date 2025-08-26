#include "view/page/connectionpage/connectionpage.h"
#include "ble/remote_dispatcher.h"
#include "controller/controller.h"
#include "utility/resource_monitor.h"
#include "view/tft.h"

namespace view {

ConnectionPage::ConnectionPage()
    : Page::Page(RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
                 nullptr),
      // TODO: dimension and position
      m_text(new Text(RectType{Coordinates{10, 50}, Size{240, 120}},
                      nullptr,
                      "Click for advertising")) {
    appendSubView(std::unique_ptr<Text>(m_text));
}

void ConnectionPage::onEvent(ble::ConnectionState const& event) {
    std::string content;
    if (event.phase == ble::ConnectionState::CONNECTED)
        content = "device connected, proceed with authentication";
    else
        content = "disconnection received";
    m_text->setContent(content);
    m_text->draw();
}

void ConnectionPage::onEvent(ble::BondingState const& event) {
    Serial.println("Received update on bonding state");
    std::string content;
    switch (event.phase) {
        case ble::BondingState::BONDED:
            content = "bonded";
            break;
        case ble::BondingState::BONDING:
            content = "bonding\n";
            if (event.passkey != 0) {
                content += "insert passkey: " + std::to_string(event.passkey);
            }
            break;
        default:
            content = "not bonded";
    }
    m_text->setContent(content);
    m_text->draw();
}

void ConnectionPage::onEvent(Click const& event) {
    Serial.println("ConnectionPage received click, proceed with advertising");
    auto controller = controller::CentralController::getInstance();
    controller->advertise();
    m_text->setContent("Advertising");
    m_text->draw();
}

std::unique_ptr<ConnectionPage> ConnectionPage::Factory::create() {
    std::unique_ptr<ConnectionPage> connectionPage =
        std::unique_ptr<ConnectionPage>(new ConnectionPage());

    // add observers here
    auto inputManager = InputManager::getInstance();
    inputManager->addObserver(Click::name, connectionPage.get());

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();
    remoteDispatcher->addObserver(ble::BondingState::name,
                                  connectionPage.get());
    remoteDispatcher->addObserver(ble::ConnectionState::name,
                                  connectionPage.get());
    remoteDispatcher->addObserver(ble::UpdateMessage::name,
                                  connectionPage.get());

    return connectionPage;
}

void ConnectionPage::drawOnScreen() {
    Serial.println("Drawing the Connection Page");
    m_text->draw();
}
}  // namespace view
