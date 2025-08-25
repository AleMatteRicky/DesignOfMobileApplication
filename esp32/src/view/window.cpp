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
void Window::setPage(std::unique_ptr<Page> page) {
    // destroy the current page and add the new one
    Serial.printf("The window had %d direct subviews\n", getNumSubViews());

    detachAll();

    View::appendSubView(std::move(page));

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

    Image* connectionImage =
        new Image(RectType{Coordinates{0, 0}, Size{32, 32}}, this,
                  {BinaryImageInfo{32, 32, sizeof(share), share}});

    auto controller = controller::CentralController::getInstance();
    bool isConnected = controller->isConnected();

    connectionImage->makeVisible(isConnected);
    connectionImage->makeVisible(true);
    connectionImage->setOnConnectionState(
        [connectionImage](ble::ConnectionState event) {
            Serial.println("Disconnection image received a connection event");
            if (event.phase == ble::ConnectionState::CONNECTED) {
                connectionImage->makeVisible(true);
                Serial.println("connection image is now visible");
            } else {
                connectionImage->makeVisible(false);
                Serial.println("connection image is now invisible");
            }
            connectionImage->draw();
        });

    Image* disconnectionImage =
        new Image(RectType{Coordinates{0, 0}, Size{32, 32}}, this,
                  {BinaryImageInfo{32, 32, sizeof(no_wifi), no_wifi}});

    disconnectionImage->makeVisible(!isConnected);
    disconnectionImage->setOnConnectionState(
        [disconnectionImage](ble::ConnectionState event) {
            Serial.println("Disconnection image received a connection event");
            if (event.phase == ble::ConnectionState::DISCONNECTED) {
                disconnectionImage->makeVisible(true);
                Serial.println("disconnection image is now visible");

            } else {
                disconnectionImage->makeVisible(false);
                Serial.println("disconnection image is now invisible");
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

    printTree();
    draw();
}

}  // namespace view