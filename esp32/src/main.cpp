#include <Arduino.h>

#include <memory>

#include "ble/connection_manager.h"
#include "controller/controller.h"
#include "view/main_event_queue.h"
#include "view/page/page_factory_impl.h"
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

    std::unique_ptr<view::Window> window = std::make_unique<view::Window>(
        pageFactory->createPage(view::PageType::CONNECTION));

    window->draw();

    controller->setModel(std::move(connectionManager));
    controller->setPageFactory(std::move(pageFactory));
    controller->setWindow(std::move(window));

    delay(1000);
    Serial.println("Setup finished");
}

void loop() {
    auto inputManager = InputManager::getInstance();
    auto controller = controller::CentralController::getInstance();
    if (!mainEventQueue->isEmpty()) {
        Serial.println("Pulling event out from the main event queue");
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
    inputManager->handleInput();
    delay(500);
}