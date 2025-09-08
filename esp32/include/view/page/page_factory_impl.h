#pragma once

#include "view/page/page_factory.h"

namespace view {

class PageFactoryImpl : public PageFactory {
public:
    std::unique_ptr<Page> createPage(PageType type) override;

private:
    inline static char const TAG[] = "PageFactoryImpl";
};
}  // namespace view