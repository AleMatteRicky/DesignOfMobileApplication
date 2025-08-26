#include "view/page/page_factory_impl.h"

namespace view {
std::unique_ptr<Page> PageFactoryImpl::createPage(PageType type) {
    // TODO: add here the remaining cases
    switch (type) {
        case PageType::HOME:
            Serial.println("Creating the home");
            return Homepage::Factory::create();
        case PageType::WEATHER:
            return WeatherPage::Factory::create();
        case PageType::TRANSLATION:
            Serial.println("Creating the translation");
            return TranslationPage::Factory::create();
        case PageType::CONNECTION:
            return ConnectionPage::Factory::create();
        default:
            Serial.println("No suitable page is found, fallback to the home");
            return Homepage::Factory::create();
    }
}
}  // namespace view
