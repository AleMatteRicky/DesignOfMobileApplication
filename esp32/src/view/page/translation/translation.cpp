#include "view/page/translation/translation.h"

#include <ArduinoJson.h>
#include "ble/remote_dispatcher.h"
#include "input/input_manager.h"
#include "utility/resource_monitor.h"

namespace view {

TranslationPage::TranslationPage()
    : Page::Page(RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
                 nullptr),
      m_text(new TextArea(
          RectType{Coordinates{0, 40}, Size{SCREEN_WIDTH, SCREEN_HEIGHT - 40}},
          this,
          "No data arrived yet")) {
    m_text->wrapTextVertically(true);
    m_text->setCenter(true, getFrame());
}

std::unique_ptr<TranslationPage> TranslationPage::Factory::create() {
    std::unique_ptr<TranslationPage> translationPage =
        std::unique_ptr<TranslationPage>(new TranslationPage());

    auto inputManager = InputManager::getInstance();

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();
    remoteDispatcher->addObserver(ble::UpdateMessage::name,
                                  translationPage.get());

    return translationPage;
}

void TranslationPage::onEvent(const ble::UpdateMessage& event) {
    ESP_LOGD(TAG, "The translation page received a new message: %s\n",
             event.msg.c_str());

    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, event.msg);
    if (error) {
        ESP_LOGD(TAG, "Error '%s' when deserializing\n", error.c_str());
        return;
    }

    if (doc["command"] != commandName) {
        return;
    }

    std::string msg = doc["text"];
    if (msg.empty())
        return;
    // do not center the text when it arrives from a translation
    m_text->setCenter(false, RectType::none);
    m_text->setContent(msg);
    m_text->draw();
}

void TranslationPage::drawOnScreen() {
    ResourceMonitor::printRemainingStackSize();
    for (byte i = 0; i < getNumSubViews(); i++) {
        getSubViewAtIndex(i).draw();
    }
}

}  // namespace view