#pragma once

#include "view/view.h"

namespace view {
class Page : public View {
public:
    Page(RectType frame, View* superiorView)
        : View::View(frame, superiorView, "page") {}

    void draw() override {
        while (true) {
            Serial.println("printing the page");
            delay(2000);
        }
    }
};

}  // namespace view
