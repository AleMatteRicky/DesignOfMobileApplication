#include "view/window.h"
#include "ble/remote_dispatcher.h"
#include "controller/controller.h"
#include "view/bin_pngs/32/missed-call.h"
#include "view/bin_pngs/32/no-wifi.h"
#include "view/bin_pngs/32/share.h"
#include "view/bin_pngs/32/text.h"
#include "view/image/connection_state.h"
#include "view/notifications/notification.h"

namespace view {

Window::Window(std::unique_ptr<Page>&& firstPage)
    : View::View(RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
                 nullptr,
                 "window"),
      m_idxPage{4} {
    auto inputManager = InputManager::getInstance();
    inputManager->addObserver(Press::name, this);
    inputManager->addObserver(DoubleClick::name, this);

    Notification* callNotification = new Notification(
        RectType{Coordinates{0, 32}, Size{32, 32}}, this,
        BinaryImageInfo{32, 32, sizeof(missed_call), missed_call},
        ble::CallNotification::name);

    Notification* messageNotification =
        new Notification(RectType{Coordinates{0, 64}, Size{32, 32}}, this,
                         BinaryImageInfo{32, 32, sizeof(text), text},
                         ble::MessageNotification::name);

    callNotification->makeVisible(false);
    messageNotification->makeVisible(false);

    Image* connectionImage =
        new Image(RectType{Coordinates{0, 0}, Size{32, 32}}, this,
                  {BinaryImageInfo{32, 32, sizeof(share), share}});

    auto controller = controller::CentralController::getInstance();
    bool isConnected = controller->isConnected();

    connectionImage->makeVisible(isConnected);
    connectionImage->makeVisible(true);
    connectionImage->setOnConnectionState(
        [connectionImage](ble::ConnectionState event) {
            ESP_LOGD(TAG, "Disconnection image received a connection event");
            if (event.phase == ble::ConnectionState::CONNECTED) {
                connectionImage->makeVisible(true);
                ESP_LOGD(TAG, "connection image is now visible");
            } else {
                connectionImage->makeVisible(false);
                ESP_LOGD(TAG, "connection image is now invisible");
            }
            connectionImage->draw();
        });

    Image* disconnectionImage =
        new Image(RectType{Coordinates{0, 0}, Size{32, 32}}, this,
                  {BinaryImageInfo{32, 32, sizeof(no_wifi), no_wifi}});

    disconnectionImage->makeVisible(!isConnected);
    disconnectionImage->setOnConnectionState(
        [disconnectionImage](ble::ConnectionState event) {
            ESP_LOGD(TAG, "Disconnection image received a connection event");
            if (event.phase == ble::ConnectionState::DISCONNECTED) {
                disconnectionImage->makeVisible(true);
                ESP_LOGD(TAG, "disconnection image is now visible");

            } else {
                disconnectionImage->makeVisible(false);
                ESP_LOGD(TAG, "disconnection image is now invisible");
            }
            disconnectionImage->draw();
        });

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();

    remoteDispatcher->addObserver(ble::ConnectionState::name, connectionImage);

    remoteDispatcher->addObserver(ble::ConnectionState::name,
                                  disconnectionImage);

    remoteDispatcher->addObserver(ble::CallNotification::name,
                                  callNotification);

    remoteDispatcher->addObserver(ble::MessageNotification::name,
                                  messageNotification);

    appendSubView(std::move(firstPage));
}

void Window::setPage(std::unique_ptr<Page> page) {
    // destroy the current page and add the new one
    Serial.printf("The window had %d direct subviews\n", getNumSubViews());

    // detach only the page, notifications need to have the same liveness of the
    // window
    detach(getSubViewAtIndex(m_idxPage));

    View::appendSubView(std::move(page));

    printTree();
    draw();
}

void Window::onEvent(Press const&) {
    controller::CentralController* controller =
        controller::CentralController::getInstance();
    controller->changePage(PageType::HOME);
}

void Window::onEvent(ble::UpdateMessage const& event) {
    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, event.msg);

    if (error)
        return;

    std::string command = doc["command"];
    PageType pageReferredByTheMessage;

    if (command == "t") {
        pageReferredByTheMessage = PageType::TRANSLATION;
    } else if (command == "w") {
        pageReferredByTheMessage = PageType::WEATHER;
    } else {
        ESP_LOGD(TAG, "command not recognised, return");
        return;
    }

    PageType currentPage = m_currentPage->getType();
    if (currentPage != pageReferredByTheMessage) {
        setPage(m_pageFactory->createPage(pageReferredByTheMessage));
        m_currentPage->onEvent(event);
}
}
}  // namespace view