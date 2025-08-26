#include "view/page/homepage/homepage.h"

#include "ble/remote_dispatcher.h"
#include "controller/controller.h"
#include "input/input_manager.h"
#include "utility/resource_monitor.h"
#include "view/bin_pngs/32/calendar.h"
#include "view/bin_pngs/32/message.h"
#include "view/bin_pngs/32/select-arrow.h"
#include "view/bin_pngs/32/smart-glasses.h"
#include "view/bin_pngs/32/translate.h"
#include "view/bin_pngs/32/weather-forecast.h"
#include "view/bin_pngs/64/calendar.h"
#include "view/bin_pngs/64/message.h"
#include "view/bin_pngs/64/smart-glasses.h"
#include "view/bin_pngs/64/translate.h"
#include "view/bin_pngs/64/weather-forecast.h"
#include "view/notifications/notification.h"

namespace view {
std::unique_ptr<Homepage> Homepage::Factory::create() {
    std::unique_ptr<Homepage> homepage =
        std::unique_ptr<Homepage>(new Homepage());

    Image* selectionArrow = new Image(
        RectType{Coordinates{SCREEN_WIDTH - 64, SCREEN_HEIGHT / 2 - 16},
                 Size{32, 32}},
        homepage.get(),
        std::vector<BinaryImageInfo>{
            BinaryImageInfo{16, 16, sizeof(select_arrow), select_arrow},
        });

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();

    Roll* roll = new Roll(
        RectType{Coordinates{64, 0}, Size{SCREEN_WIDTH - 32, SCREEN_HEIGHT}},
        homepage.get());

    auto inputManager = InputManager::getInstance();
    inputManager->addObserver(SwipeAntiClockwise::name, roll);
    inputManager->addObserver(SwipeClockwise::name, roll);
    inputManager->addObserver(Click::name, roll);

    Image* translation = new Image(
        RectType{Coordinates{0, 0}, Size{64, 64}}, roll,
        std::vector<BinaryImageInfo>{
            BinaryImageInfo{32, 32, sizeof(translate_32), translate_32},
            BinaryImageInfo{64, 64, sizeof(translate_64), translate_64}});

    translation->setOnClick([]() {
        auto controller = controller::CentralController::getInstance();
        controller->changePage(PageType::TRANSLATION);
    });

    Image* weather =
        new Image(RectType{Coordinates{0, 0}, Size{64, 64}}, roll,
                  std::vector<BinaryImageInfo>{
                      BinaryImageInfo{32, 32, sizeof(weather_forecast_32),
                                      weather_forecast_32},
                      BinaryImageInfo{64, 64, sizeof(weather_forecast_64),
                                      weather_forecast_64}});

    weather->setOnClick([]() {
        auto controller = controller::CentralController::getInstance();
        controller->changePage(PageType::WEATHER);
    });

    Image* settings = new Image(
        RectType{Coordinates{0, 0}, Size{64, 64}}, roll,
        std::vector<BinaryImageInfo>{
            BinaryImageInfo{32, 32, sizeof(smart_glasses_32), smart_glasses_32},
            BinaryImageInfo{64, 64, sizeof(smart_glasses_64), smart_glasses_64}}

    );

    settings->setOnClick([]() {
        auto controller = controller::CentralController::getInstance();
        controller->changePage(PageType::SETTINGS);
    });

    Image* messages =
        new Image(RectType{Coordinates{0, 0}, Size{64, 64}}, roll,
                  std::vector<BinaryImageInfo>{
                      BinaryImageInfo{32, 32, sizeof(message_32), message_32},
                      BinaryImageInfo{64, 64, sizeof(message_64), message_64}});

    messages->setOnClick([]() {
        auto controller = controller::CentralController::getInstance();
        controller->changePage(PageType::MESSAGES);
    });

    return homepage;
}

void Homepage::drawOnScreen() {
    ResourceMonitor::printRemainingStackSize();
    for (byte i = 0; i < getNumSubViews(); i++) {
        getSubViewAtIndex(i).draw();
    }
}

}  // namespace view