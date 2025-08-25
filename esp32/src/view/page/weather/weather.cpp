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
#include "view/bin_pngs/32/weather_conditions/clear.h"
#include "view/bin_pngs/32/weather_conditions/clear_night.h"
#include "view/bin_pngs/32/weather_conditions/clouds_1.h"
#include "view/bin_pngs/32/weather_conditions/clouds_1_night.h"
#include "view/bin_pngs/32/weather_conditions/clouds_2.h"
#include "view/bin_pngs/32/weather_conditions/clouds_3.h"
#include "view/bin_pngs/32/weather_conditions/rain_1.h"
#include "view/bin_pngs/32/weather_conditions/rain_1_night.h"
#include "view/bin_pngs/32/weather_conditions/rain_2.h"
#include "view/bin_pngs/32/weather_conditions/rain_3.h"
#include "view/bin_pngs/32/weather_conditions/rain_4.h"
#include "view/bin_pngs/32/weather_conditions/snow_1.h"
#include "view/bin_pngs/32/weather_conditions/snow_1_night.h"
#include "view/bin_pngs/32/weather_conditions/snow_2.h"
#include "view/bin_pngs/32/weather_conditions/snow_3.h"
#include "view/bin_pngs/32/weather_conditions/thunderstorm_1.h"
#include "view/bin_pngs/32/weather_conditions/thunderstorm_1_3_night.h"
#include "view/bin_pngs/32/weather_conditions/thunderstorm_2.h"
#include "view/bin_pngs/32/weather_conditions/thunderstorm_3.h"
#include "view/text/text.h"
#include <ArduinoJson.h>
#include "utility/resource_monitor.h"

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

            if (doc["command"] != commandName) return;

            content = std::string(doc["location"]);
        },
        ""
    );

    remoteDispatcher->addObserver(ble::UpdateMessage::name, locationText);


    Text* timeText = new Text(
        RectType{Coordinates{0, 0}, Size{0, 0}},
        weatherPage.get(),
        [](ble::UpdateMessage const& event, std::string& content){
            JsonDocument doc;
            deserializeJson(doc, event.msg);

            if (doc["command"] != commandName) return;

            JsonVariant cond = doc["conditions"][0]; //FIXME: allow to switch conditions
            if (cond.isNull()) return;

            content = std::string(cond["time"]);
        },
        ""
    );       

    remoteDispatcher->addObserver(ble::UpdateMessage::name, timeText);


    Text* tmpText = new Text(
        RectType{Coordinates{0, 0}, Size{0, 0}},
        weatherPage.get(),
        [](ble::UpdateMessage const& event, std::string& content){
            JsonDocument doc;
            deserializeJson(doc, event.msg);

            if (doc["command"] != commandName) return;

            JsonVariant cond = doc["conditions"][0]; //FIXME: allow to switch conditions
            if (cond.isNull()) return;

            content = "Temp: " + std::string(cond["temperature"]) + "°"; 
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

            if (doc["command"] != commandName) return;

            JsonVariant cond = doc["conditions"][0]; //FIXME: allow to switch conditions
            if (cond.isNull()) return;

            content = "Pressure: " + std::string(cond["pressure"]) + " hPa";
        },
        ""
    );
    
    remoteDispatcher->addObserver(ble::UpdateMessage::name, pressureText);

    
    //TODO: add wind info

    Image* weatherImage = new Image(
        RectType{Coordinates{0, 0}, Size{0, 0}},
        weatherPage.get(),
        std::vector{
            BinaryImageInfo{32, 32, sizeof(clear), clear},
            BinaryImageInfo{32, 32, sizeof(clear_night), clear_night},
            BinaryImageInfo{32, 32, sizeof(clouds_1), clouds_1},
            BinaryImageInfo{32, 32, sizeof(clouds_1_night), clouds_1_night},
            BinaryImageInfo{32, 32, sizeof(clouds_2), clouds_2},
            BinaryImageInfo{32, 32, sizeof(clouds_3), clouds_3},
            BinaryImageInfo{32, 32, sizeof(rain_1), rain_1},
            BinaryImageInfo{32, 32, sizeof(rain_1_night), rain_1_night},
            BinaryImageInfo{32, 32, sizeof(rain_2), rain_2},
            BinaryImageInfo{32, 32, sizeof(rain_3), rain_3},
            BinaryImageInfo{32, 32, sizeof(rain_4), rain_4},
            BinaryImageInfo{32, 32, sizeof(snow_1), snow_1},
            BinaryImageInfo{32, 32, sizeof(snow_1_night), snow_1_night},
            BinaryImageInfo{32, 32, sizeof(snow_2), snow_2},
            BinaryImageInfo{32, 32, sizeof(snow_3), snow_3},
            BinaryImageInfo{32, 32, sizeof(thunderstorm_1), thunderstorm_1},
            BinaryImageInfo{32, 32, sizeof(thunderstorm_1_3_night), thunderstorm_1_3_night},
            BinaryImageInfo{32, 32, sizeof(thunderstorm_2), thunderstorm_2},
            BinaryImageInfo{32, 32, sizeof(thunderstorm_3), thunderstorm_3},            
        });

    auto onUpdateMessage = [](ble::UpdateMessage const& event, int& index){
            JsonDocument doc;
            deserializeJson(doc, event.msg);

            if (doc["command"] != commandName) return;

            JsonVariant cond = doc["conditions"][0]; //FIXME: allow to switch conditions
            if (cond.isNull()) return;

            std::string iconName = cond["iconName"];
            
            if(iconName == "clear"){
                index = 0;
            }else if(iconName == "clear_night"){
                index = 1;
            }else if(iconName == "clouds_1"){
                index = 2;
            }else if(iconName == "clouds_1_night"){
                index = 3;
            }else if(iconName == "clouds_2"){
                index = 4;
            }else if(iconName == "clouds_3"){
                index = 5;
            }else if(iconName == "rain_1"){
                index = 6;
            }else if(iconName == "rain_1_night"){
                index = 7;
            }else if(iconName == "rain_2"){
                index = 8;
            }else if(iconName == "rain_3"){
                index = 9;
            }else if(iconName == "rain_4"){
                index = 10;
            }else if(iconName == "snow_1"){
                index = 11;
            }else if(iconName == "snow_1_night"){
                index = 12;
            }else if(iconName == "snow_2"){
                index = 13;
            }else if(iconName == "snow_3"){
                index = 14;
            }else if(iconName == "thunderstorm_1"){
                index = 15;
            }else if(iconName == "thunderstorm_1_3_night"){
                index = 16;
            }else if(iconName == "thunderstorm_2"){
                index = 17;
            }else if(iconName == "thunderstorm_3"){
                index = 18;
            }else{
                //iconName not valid. Default: clear icon
                index = 0;
            }
        };

    weatherImage->setOnUpdateMessage(onUpdateMessage);

    remoteDispatcher->addObserver(ble::UpdateMessage::name, weatherImage);

    return weatherPage;
}

void WeatherPage::drawOnScreen(){
    ResourceMonitor::printRemainingStackSize();
    for (byte i = 0; i < getNumSubViews(); i++) {
        getSubViewAtIndex(i).draw();
    }
}

}  // namespace view