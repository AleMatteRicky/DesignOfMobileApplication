#pragma once
#include "utility/blocking_queue.h"
#include "view/ui_event.h"

namespace view {
class MainEventQueue : public BlockingQueue<std::unique_ptr<UIEvent>> {
public:
    static MainEventQueue* getInstance() {
        if (instance)
            return instance;
        instance = new MainEventQueue();
        return instance;
    }

private:
    static inline MainEventQueue* instance = nullptr;

    MainEventQueue() {}
};

}  // namespace view
