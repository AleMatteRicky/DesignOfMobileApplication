#include "view/view.h"
#include "ble/remote_dispatcher.h"
#include "utility/resource_monitor.h"

namespace view {

View::~View() {
    ESP_LOGD(TAG, "%s at %p is being destroyed\n", m_tag.c_str(), this);
    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();
    auto inputManager = InputManager::getInstance();
    remoteDispatcher->removeFromAllEvents(*this);
    inputManager->removeFromAllEvents(*this);
    ESP_LOGD(
        TAG,
        "%s at %p has been removed succesfully from all possible dispatchers\n",
        m_tag.c_str(), this);
}

void View::applyRecursively(std::function<void(View&)> f) {
    for (auto const& subView : m_subViews) {
        subView->applyRecursively(f);
    }
    f(*this);
}

void View::printTree() const {
    auto printTreeAux = [](View const& view, uint16_t numSpaces,
                           auto&& printTreeAux) -> void {
        for (uint16_t i = 0; i < numSpaces; i++) {
            Serial.printf(" ");
        }
        Serial.printf("%s at %p\n", view.m_tag.c_str(), &view);

        uint8_t numSubViews = view.m_subViews.size();
        for (uint8_t i = 0; i < numSubViews; i++) {
            printTreeAux(*view.m_subViews[i], numSpaces + 5, printTreeAux);
        }
    };

    printTreeAux(*this, 1, printTreeAux);
}

}  // namespace view