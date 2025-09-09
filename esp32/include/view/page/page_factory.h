#pragma once
#include "view/page/page.h"
#include "view/view.h"

namespace view {
/**
 * Factory to instantiate a page based on their identifier
 */
class PageFactory {
public:
    virtual std::unique_ptr<Page> createPage(PageType type) = 0;
};
}  // namespace view