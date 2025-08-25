#include "controller/controller.h"
#include "ble/remote_dispatcher.h"

namespace controller {
void CentralController::changePage(view::PageType page) {
    Serial.println("Chaning page ");
    // TODO: complete the message accordingly to the protocol
    std::string msg = "change page";
    if (m_model->isConnected())
        m_model->send(msg);
    removeFromSubjects();
    m_window->setPage(m_pageFactory->createPage(view::PageType::HOME));
}
}  // namespace controller