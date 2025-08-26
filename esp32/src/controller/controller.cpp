#include "controller/controller.h"
#include "ble/remote_dispatcher.h"
#include "utility/resource_monitor.h"

namespace controller {
void CentralController::changePage(view::PageType page) {
    Serial.println("Changing page");
    ResourceMonitor::printRemainingStackSize();
    // TODO: complete the message accordingly to the protocol
    std::string msg = "change page";
    if (m_model->isConnected()) {
        Serial.println("Model is connected, send the change page message");
        m_model->send(msg);
    }
    Serial.println("Setting the current page");
    m_window->setPage(m_pageFactory->createPage(page));
}
}  // namespace controller