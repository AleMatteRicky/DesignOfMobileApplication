#include "view/window.h"
#include <ArduinoJson.h>
#include "ble/remote_dispatcher.h"
#include "controller/central_controller.h"
#include "view/bin_pngs/24/connected.h"
#include "view/bin_pngs/24/incoming_call.h"
#include "view/bin_pngs/24/incoming_message.h"
#include "view/bin_pngs/24/no_connection.h"
#include "view/image/connection_state.h"
#include "view/notifications/notification.h"

namespace view {

Window::Window(std::unique_ptr<view::PageFactory>&& pageFactory)
    : View::View(RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
                 nullptr,
                 "window"),
      m_pageFactory(std::move(pageFactory)),
      m_currentPage(nullptr) {
    auto inputManager = InputManager::getInstance();
    inputManager->addObserver(Press::name, this);

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();
    remoteDispatcher->addObserver(ble::UpdateMessage::name, this);

    Image* connectionImage =
        new Image(RectType{Coordinates{8, 8}, Size{24, 24}}, this,
                  {BIN_IMG(24, 24, connected_24)});

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

    // disconnection and connection image overlap, and mutually excludes
    // themselves based on the connection's state
    Image* disconnectionImage = new Image(
        RectType{connectionImage->getCoordinates(), connectionImage->getSize()},
        this, {BIN_IMG(24, 24, no_connection_24)});

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

    remoteDispatcher->addObserver(ble::ConnectionState::name, connectionImage);

    remoteDispatcher->addObserver(ble::ConnectionState::name,
                                  disconnectionImage);

    auto [xConnectionImg, yConnectionImg] = connectionImage->getCoordinates();
    auto [connectionImgWidth, connectionImgHeight] = connectionImage->getSize();
    auto offset = 8;

    Notification* callNotification = new Notification(
        RectType{Coordinates{xConnectionImg + connectionImgWidth + offset,
                             yConnectionImg},
                 Size{24, 24}},
        this, "Incoming call", BIN_IMG(24, 24, incoming_call_24),
        ble::CallNotification::name);

    Notification* messageNotification = new Notification(
        RectType{
            Coordinates{xConnectionImg + 2 * (connectionImgHeight + offset),
                        yConnectionImg},
            Size{24, 24}},
        this, "Incoming message", BIN_IMG(24, 24, incoming_message),
        ble::MessageNotification::name);

    callNotification->makeVisible(false);
    messageNotification->makeVisible(false);

    remoteDispatcher->addObserver(ble::CallNotification::name,
                                  callNotification);

    remoteDispatcher->addObserver(ble::MessageNotification::name,
                                  messageNotification);

    auto firstPage = m_pageFactory->createPage(PageType::HOME);

    setPage(std::move(firstPage));
}

void Window::setPage(PageType pageType) {
    setPage(m_pageFactory->createPage(pageType));
}

void Window::detachCurrentPage() {
    if (m_currentPage)
        detach(*m_currentPage);
}

void Window::setPage(std::unique_ptr<Page>&& page) {
    ESP_LOGD(TAG, "Changing page");
    detachCurrentPage();

    m_currentPage = page.get();

    appendSubView(std::move(page));

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