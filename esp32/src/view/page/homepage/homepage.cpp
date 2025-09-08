#include "view/page/homepage/homepage.h"

#include "ble/remote_dispatcher.h"
#include "controller/central_controller.h"
#include "input/input_manager.h"
#include "utility/resource_monitor.h"
#include "view/bin_pngs/32/chat.h"
#include "view/bin_pngs/32/connection_to_smartphone.h"
#include "view/bin_pngs/32/select-arrow.h"
#include "view/bin_pngs/32/translate.h"
#include "view/bin_pngs/32/weather-forecast.h"
#include "view/bin_pngs/64/chat.h"
#include "view/bin_pngs/64/connection_to_smartphone.h"
#include "view/bin_pngs/64/translate.h"
#include "view/bin_pngs/64/weather-forecast.h"

namespace view {
std::unique_ptr<Homepage> Homepage::Factory::create() {
    std::unique_ptr<Homepage> homepage =
        std::unique_ptr<Homepage>(new Homepage());

    Image* selectionArrow = new Image(
        RectType{Coordinates{SCREEN_WIDTH - 64, SCREEN_HEIGHT / 2 - 16},
                 Size{32, 32}},
        homepage.get(),
        std::vector<BinaryImageInfo>{BIN_IMG(16, 16, select_arrow)});

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();

    Roll* roll = new Roll(
        RectType{Coordinates{64, 0}, Size{SCREEN_WIDTH - 32, SCREEN_HEIGHT}},
        homepage.get());

    auto inputManager = InputManager::getInstance();
    inputManager->addObserver(SwipeAntiClockwise::name, roll);
    inputManager->addObserver(SwipeClockwise::name, roll);
    inputManager->addObserver(Click::name, roll);

    Image* connectionImage =
        new Image(RectType{Coordinates{0, 0}, Size{64, 64}}, roll,
                  std::vector<BinaryImageInfo>{
                      BIN_IMG(32, 32, connection_to_smartphone_32),
                      BIN_IMG(64, 64, connection_to_smartphone_64)});

    connectionImage->setOnClick([]() {
        auto controller = controller::CentralController::getInstance();
        controller->changePage(PageType::CONNECTION);
    });

    Image* translation =
        new Image(RectType{Coordinates{0, 0}, Size{64, 64}}, roll,
                  std::vector<BinaryImageInfo>{BIN_IMG(32, 32, translate_32),
                                               BIN_IMG(64, 64, translate_64)});

    translation->setOnClick([]() {
        auto controller = controller::CentralController::getInstance();
        controller->changePage(PageType::TRANSLATION);
    });

    Image* weather = new Image(
        RectType{Coordinates{0, 0}, Size{64, 64}}, roll,
        std::vector<BinaryImageInfo>{BIN_IMG(32, 32, weather_forecast_32),
                                     BIN_IMG(64, 64, weather_forecast_64)});

    weather->setOnClick([]() {
        auto controller = controller::CentralController::getInstance();
        controller->changePage(PageType::WEATHER);
    });

    Image* messages =
        new Image(RectType{Coordinates{0, 0}, Size{64, 64}}, roll,
                  std::vector<BinaryImageInfo>{BIN_IMG(32, 32, chat_32),
                                               BIN_IMG(64, 64, chat_64)});

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