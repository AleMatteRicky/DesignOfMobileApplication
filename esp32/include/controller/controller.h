#pragma once

#include <Wire.h>

#include <atomic>
#include <thread>

#include "SparkFun_CAP1203.h"
#include "input/input_manager.h"
#include "model/model.h"
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
    void setModel(std::unique_ptr<Model>&& model) {
        m_model = std::move(model);
    }

    void setWindow(std::unique_ptr<view::Window>&& window) {
        m_window = std::move(window);
    }

    void setPageFactory(std::unique_ptr<view::PageFactory>&& pageFactory) {
        m_pageFactory = std::move(pageFactory);
    }

    static CentralController* getInstance() {
        if (instance)
            return instance;
        instance = new CentralController();
        return instance;
    }

    void changePage(view::PageType);

    void drawUI() {
        assert(m_window != nullptr);

        /*
        Serial.println("Printing the UI tree");
        delay(1000);
        m_window->printTree();
        */

        Serial.println("Drawing the UI");
        m_window->draw();
    }

private:
    // remove the Views from the respective listeners to avoid them referencing
    // to deleted memory
    void removeFromSubjects();

    CentralController() {}

private:
    std::unique_ptr<Model> m_model;
    std::unique_ptr<view::Window> m_window;
    std::unique_ptr<view::PageFactory> m_pageFactory;

    static inline CentralController* instance = nullptr;
};

}  // namespace controller