#include "view/page/translation/translation.h"

#include "ble/remote_dispatcher.h"
#include "controller/controller.h"
#include "input/input_manager.h"
#include "view/bin_pngs/32/missed-call.h"
#include "view/bin_pngs/32/no-wifi.h"
#include "view/bin_pngs/32/share.h"
#include "view/bin_pngs/32/text.h"
#include "view/bin_pngs/64/message.h"
#include "view/notifications/notification.h"
#include "view/text/text.h"
#include <ArduinoJson.h>
#include "utility/resource_monitor.h"

namespace view {
std::unique_ptr<TranslationPage> TranslationPage::Factory::create() {
    std::unique_ptr<TranslationPage> translationPage =
        std::unique_ptr<TranslationPage>(new TranslationPage(nullptr));

    ConnectionStateImage* connectionStateImg = new ConnectionStateImage(
        RectType{Coordinates{0, SCREEN_HEIGHT - 32}, Size{32, 32}},
        translationPage.get(), BinaryImageInfo{32, 32, sizeof(share), share},
        BinaryImageInfo{32, 32, sizeof(no_wifi), no_wifi});

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();

    remoteDispatcher->addObserver(ble::ConnectionState::name,
                                  connectionStateImg);

    Notification* callNotification = new Notification(
        RectType{Coordinates{20, SCREEN_HEIGHT - 32}, Size{32, 32}},
        translationPage.get(),
        BinaryImageInfo{32, 32, sizeof(missed_call), missed_call},
        ble::CallNotification::name);

    Notification* messageNotification = new Notification(
        RectType{Coordinates{40, SCREEN_WIDTH - 32}, Size{32, 32}},
        translationPage.get(), BinaryImageInfo{32, 32, sizeof(text), text},
        ble::MessageNotification::name);

    remoteDispatcher->addObserver(ble::CallNotification::name,
                                  callNotification);

    remoteDispatcher->addObserver(ble::MessageNotification::name,
                                  messageNotification);

    auto inputManager = InputManager::getInstance();

    Text* text = new Text(
        RectType{Coordinates{0, 0}, Size{0, 0}},
        translationPage.get(),
        [](ble::UpdateMessage const& event, std::string& content){
            JsonDocument doc;
            deserializeJson(doc, event.msg);

            if (doc["command"] != commandName){
                return;
            }

            //TODO
            //content = std::string(doc["text"]);
        },
        ""
    );

    remoteDispatcher->addObserver(ble::UpdateMessage::name, text);

    return translationPage;
}

void TranslationPage::drawOnScreen(){
    ResourceMonitor::printRemainingStackSize();
    for (byte i = 0; i < getNumSubViews(); i++) {
        getSubViewAtIndex(i).draw();
    }
}

}  // namespace view