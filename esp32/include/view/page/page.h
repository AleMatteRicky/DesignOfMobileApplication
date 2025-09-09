#pragma once

#include "view/view.h"

namespace view {
class Page : public View {
public:
    Page(RectType frame, View* superiorView)
        : View::View(frame, superiorView, "page") {}
    virtual PageType getType() = 0;
};
}  // namespace view
