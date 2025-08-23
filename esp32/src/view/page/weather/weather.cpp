#include "view/page/weather/weather.h"

#include "ble/remote_dispatcher.h"
#include "controller/controller.h"
#include "input/input_manager.h"
#include "view/bin_pngs/32/missed-call.h"
#include "view/bin_pngs/32/no-wifi.h"
#include "view/bin_pngs/32/share.h"
#include "view/bin_pngs/32/text.h"
#include "view/bin_pngs/64/message.h"
#include "view/notifications/notification.h"
#include "view/bin_pngs/32/clear.h"
#include "view/bin_pngs/32/clear_n.h"
#include "view/text/text.h"
#include <ArduinoJson.h>

namespace view {
std::unique_ptr<WeatherPage> WeatherPage::Factory::create() {
    std::unique_ptr<WeatherPage> weatherPage =
        std::unique_ptr<WeatherPage>(new WeatherPage(nullptr));

    ConnectionStateImage* connectionStateImg = new ConnectionStateImage(
        RectType{Coordinates{0, SCREEN_HEIGHT - 32}, Size{32, 32}},
        weatherPage.get(), BinaryImageInfo{32, 32, sizeof(share), share},
        BinaryImageInfo{32, 32, sizeof(no_wifi), no_wifi});

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();

    remoteDispatcher->addObserver(ble::ConnectionState::name,
                                  connectionStateImg);

    Notification* callNotification = new Notification(
        RectType{Coordinates{20, SCREEN_HEIGHT - 32}, Size{32, 32}},
        weatherPage.get(),
        BinaryImageInfo{32, 32, sizeof(missed_call), missed_call},
        ble::CallNotification::name);

    Notification* messageNotification = new Notification(
        RectType{Coordinates{40, SCREEN_WIDTH - 32}, Size{32, 32}},
        weatherPage.get(), BinaryImageInfo{32, 32, sizeof(text), text},
        ble::MessageNotification::name);

    remoteDispatcher->addObserver(ble::CallNotification::name,
                                  callNotification);

    remoteDispatcher->addObserver(ble::MessageNotification::name,
                                  messageNotification);

    auto inputManager = InputManager::getInstance();

    Text* locationText = new Text(
        RectType{Coordinates{0, 0}, Size{0, 0}},
        weatherPage.get(),
        [](ble::UpdateMessage const& event, std::string& content){
            JsonDocument doc;
            deserializeJson(doc, event.msg);

            if (doc["command"] != commandName){
                return;
            }

            content = std::string(doc["location"]);
        },
        ""
    );

    remoteDispatcher->addObserver(ble::UpdateMessage::name, locationText);


    Text* tmpText = new Text(
        RectType{Coordinates{0, 0}, Size{0, 0}},
        weatherPage.get(),
        [](ble::UpdateMessage const& event, std::string& content){
            JsonDocument doc;
            deserializeJson(doc, event.msg);

            if (doc["command"] != commandName){
                return;
            }

            content = "Temp: " + std::string(doc["temperature"]) + "°C";
        },
        ""
    );

    remoteDispatcher->addObserver(ble::UpdateMessage::name, tmpText);

    
    Text* pressureText = new Text(
        RectType{Coordinates{0, 0}, Size{0, 0}},
        weatherPage.get(),
        [](ble::UpdateMessage const& event, std::string& content){
            JsonDocument doc;
            deserializeJson(doc, event.msg);

            if (doc["command"] != commandName){
                return;
            }

            content = "Pressure: " + std::string(doc["pressure"]) + " hPa";
        },
        ""
    );

    remoteDispatcher->addObserver(ble::UpdateMessage::name, pressureText);

    //TODO: add wind info

    //TODO: add icons
    Image* weatherImage = new Image(
        RectType{Coordinates{0, 0}, Size{0, 0}},
        weatherPage.get(),
        std::vector{
            BinaryImageInfo{32, 32, sizeof(clear), clear},
            BinaryImageInfo{32, 32, sizeof(clear_n), clear_n},
        },
        [](ble::UpdateMessage const& event, int& index){
            JsonDocument doc;
            deserializeJson(doc, event.msg);

            if(doc["icon"] == "clear"){
                index = 0;
            }else if(doc["icon"] == "clear_n"){
                index = 1;
            }
        }
    );

    remoteDispatcher->addObserver(ble::UpdateMessage::name, weatherImage);

    return weatherPage;
}

void WeatherPage::draw() {
    unsigned int remaining_stack = uxTaskGetStackHighWaterMark(NULL);
    Serial.printf("Remaining stack size: %u bytes\n", remaining_stack);
    for (byte i = 0; i < getNumSubViews(); i++) {
        getSubViewAtIndex(i).draw();
    }
}

}  // namespace view