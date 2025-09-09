#include <Arduino.h>

#include <memory>

#include "ble/connection_manager.h"
#include "controller/central_controller.h"
#include "esp_heap_caps.h"
#include "view/main_event_queue.h"
#include "view/page/page_factory_impl.h"
#include "view/ui_event.h"
#include "view/window.h"

auto mainEventQueue = view::MainEventQueue::getInstance();

char const TAG[] = "main";

void heap_caps_alloc_failed_hook(size_t requested_size,
                                 uint32_t caps,
                                 const char* function_name) {
    ESP_LOGE(TAG,
             "%s was called but failed to allocate %d bytes with 0x%X "
             "capabilities, but only %lu bytes were available\n",
             function_name, requested_size, caps,
             heap_caps_get_largest_free_block(caps));
    ESP_LOGD(TAG, "Heap dump: ");
    ResourceMonitor::printRemainingHeapSizeInfo();
    ESP_LOGD(TAG, "######");
}

void setup() {
    Serial.begin(115200);
    delay(3000);
    // force the initialization
    auto* tft = tft::Tft::getTFT_eSPI();

    // be notified when a memory allocation fails
    esp_err_t error =
        heap_caps_register_failed_alloc_callback(heap_caps_alloc_failed_hook);
    ESP_ERROR_CHECK(error);

    controller::CentralController* controller =
        controller::CentralController::getInstance();

    std::unique_ptr<ble::ConnectionManager> connectionManager =
        std::make_unique<ble::ConnectionManager>();

    std::unique_ptr<view::PageFactory> pageFactory =
        std::make_unique<view::PageFactoryImpl>();

    std::unique_ptr<view::Window> window =
        std::make_unique<view::Window>(std::move(pageFactory));

    window->draw();

    controller->setRemoteController(std::move(connectionManager));
    controller->setWindow(std::move(window));

    ESP_LOGD(TAG, "Setup finished");
    ESP_LOGD(TAG, "Available heap: %lu", heap_caps_get_free_size(DEFAULT));
    delay(1000);
}

void loop() {
    auto inputManager = InputManager::getInstance();
    auto controller = controller::CentralController::getInstance();
    if (!mainEventQueue->isEmpty()) {
        ESP_LOGD(TAG, "pulling an event out from the main queue");
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
    delay(100);
}