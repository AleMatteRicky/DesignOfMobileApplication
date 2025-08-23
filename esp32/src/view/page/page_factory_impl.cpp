#include "view/page/page_factory_impl.h"

namespace view {
std::unique_ptr<Page> PageFactoryImpl::createPage(PageType type) {
    // TODO: add here the remaining cases
    switch (type) {
        case PageType::HOME:
            return Homepage::Factory::create();
        default:
            return Homepage::Factory::create();
    }
}
}  // namespace view
