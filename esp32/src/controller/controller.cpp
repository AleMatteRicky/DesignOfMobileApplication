#include "ble/remote_dispatcher.h"
#include "controller/central_controller.h"
#include "utility/resource_monitor.h"

namespace controller {
void CentralController::changePage(view::PageType page) {
    m_window->setPage(page);
}
}  // namespace controller