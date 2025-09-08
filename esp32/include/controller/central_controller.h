#pragma once

#include <Wire.h>

#include <atomic>
#include <thread>

#include "SparkFun_CAP1203.h"
#include "controller/remote_controller.h"
#include "input/input_manager.h"
#include "view/page/page_factory.h"
#include "view/page/type.h"
#include "view/window.h"

namespace controller {
/**
 * Controller class responsible for handling events form the view in order to
 * change the Model
 */
class CentralController {
public:
    void setRemoteController(
        std::unique_ptr<RemoteController>&& remoteController) {
        m_remoteController = std::move(remoteController);
    }

    void setWindow(std::unique_ptr<view::Window>&& window) {
        m_window = std::move(window);
    }

    static CentralController* getInstance() {
        if (instance)
            return instance;
        instance = new CentralController();
        return instance;
    }

    void changePage(view::PageType);

    void advertise() { m_remoteController->advertise(); }

    void disconnect() { m_remoteController->disconnect(); }

    bool isConnected() {
        return m_remoteController && m_remoteController->isConnected();
    }

    std::vector<std::string> getMessages(byte numMessages) {
        std::vector<std::string> messages = m_remoteController->getMessages();
        for (auto const& msg : messages) {
            ESP_LOGD(TAG, "stored messages: '%s'", msg.c_str());
        }

        byte numMessagesToextract =
            std::min((byte)messages.size(), numMessages);
        return std::vector<std::string>(messages.end() - numMessagesToextract,
                                        messages.end());
    }

private:
    CentralController() {}

private:
    inline static char const TAG[] = "CentralControllre";

private:
    std::unique_ptr<RemoteController> m_remoteController;
    std::unique_ptr<view::Window> m_window;

    static inline CentralController* instance = nullptr;
};

}  // namespace controller