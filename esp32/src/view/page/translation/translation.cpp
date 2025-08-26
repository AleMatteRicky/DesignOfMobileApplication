#include "view/page/translation/translation.h"

#include "ble/remote_dispatcher.h"
#include "input/input_manager.h"
#include "utility/resource_monitor.h"
#include "view/bin_pngs/32/missed-call.h"
#include "view/bin_pngs/32/no-wifi.h"
#include "view/bin_pngs/32/share.h"
#include "view/bin_pngs/32/text.h"
#include "view/bin_pngs/64/message.h"

namespace view {

TranslationPage::TranslationPage()
    : Page::Page(RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
                 nullptr),
      m_text(new Text(RectType{Coordinates{0, 0}, Size{120, 120}}, nullptr)) {
    appendSubView(std::unique_ptr<Text>(m_text));
};

std::unique_ptr<TranslationPage> TranslationPage::Factory::create() {
    std::unique_ptr<TranslationPage> translationPage =
        std::unique_ptr<TranslationPage>(new TranslationPage());

    auto inputManager = InputManager::getInstance();
    inputManager->addObserver(SwipeClockwise::name, translationPage.get());
    inputManager->addObserver(SwipeAntiClockwise::name, translationPage.get());

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();
    remoteDispatcher->addObserver(ble::UpdateMessage::name,
                                  translationPage.get());

    return translationPage;
}

void TranslationPage::onEvent(const ble::UpdateMessage& event) {
    Serial.printf("The translation page received a new message: %s\n",
                  event.msg.c_str());

#if 0
    // TODO: complete with the correct parsing
    //
    JsonDocument doc;
    deserializeJson(doc, event.msg);

    if (doc["command"] != commandName) {
        return;
    }
    // TODO: Ask teo?
    // content = std::string(doc["text"]);
    m_text->appendContent(doc["text"]);
#endif
    m_text->appendContent(event.msg);
    m_text->draw();
}

void TranslationPage::onEvent(SwipeAntiClockwise const& ev) {
    m_text->onEvent(ev);
}

void TranslationPage::onEvent(SwipeClockwise const& ev) {
    m_text->onEvent(ev);
}

void TranslationPage::drawOnScreen() {
    ResourceMonitor::printRemainingStackSize();
    for (byte i = 0; i < getNumSubViews(); i++) {
        getSubViewAtIndex(i).draw();
    }
}

}  // namespace view