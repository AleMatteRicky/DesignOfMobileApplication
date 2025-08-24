#include <Arduino.h>

#include <memory>

#include "ble/connection_manager.h"
#include "controller/controller.h"
#include "view/main_event_queue.h"
#include "view/page/page_factory_impl.h"
#include "view/tft.h"
#include "view/ui_event.h"
#include "view/window.h"

auto mainEventQueue = view::MainEventQueue::getInstance();

void setup() {
    Serial.begin(115200);
    delay(500);

    controller::CentralController* controller =
        controller::CentralController::getInstance();

    std::unique_ptr<ble::ConnectionManager> connectionManager =
        std::make_unique<ble::ConnectionManager>();

    std::unique_ptr<view::PageFactory> pageFactory =
        std::make_unique<view::PageFactoryImpl>();

    std::unique_ptr<view::Window> window =
        std::make_unique<view::Window>([](view::PageType type) {
            controller::CentralController* controller =
                controller::CentralController::getInstance();

            controller->changePage(type);
        });

    window->setPage(pageFactory->createPage(view::PageType::HOME));

    controller->setModel(std::move(connectionManager));
    controller->setPageFactory(std::move(pageFactory));
    controller->setWindow(std::move(window));

    delay(3000);
    Serial.println("Setup finished");
}

void loop() {
    auto inputManager = InputManager::getInstance();
#if 0
    auto controller = controller::CentralController::getInstance();
    if (!mainEventQueue->isEmpty()) {
        std::unique_ptr<view::UIEvent> event =
            std::move(mainEventQueue->remove());
        view::UIEventTag tag = event->getTag();
        switch (tag) {
            case view::UIEventTag::RemoteProcedure:
                view::RemoteProcedure* remoteProcedure =
                    static_cast<view::RemoteProcedure*>(event.get());
                remoteProcedure->call();
                break;
        }
    }
    delay(3000);
#endif
    inputManager->handleInput();
}