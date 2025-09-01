#pragma once

#include "view/view.h"

namespace view {
class Text {
public:
    virtual void setContent(std::string const& content) = 0;

    virtual void appendContent(std::string const& content) = 0;

    virtual void wrapTextVertically() = 0;

    virtual void doNotWrapText(
        std::function<void(std::string const&)> onExceedingText) = 0;
};

}  // namespace view