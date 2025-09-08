#include "view/page/page_factory_impl.h"

namespace view {
std::unique_ptr<Page> PageFactoryImpl::createPage(PageType type) {
    // TODO: add here the remaining cases
    switch (type) {
        case PageType::HOME:
            ESP_LOGD(TAG, "Creating the home page");
            return Homepage::Factory::create();
        case PageType::WEATHER:
            ESP_LOGD(TAG, "Creating the weather page");
            return WeatherPage::Factory::create();
        case PageType::TRANSLATION:
            ESP_LOGD(TAG, "Creating the translation page");
            return TranslationPage::Factory::create();
        case PageType::CONNECTION:
            ESP_LOGD(TAG, "Creating the connection page");
            return ConnectionPage::Factory::create();
        case PageType::MESSAGES:
            ESP_LOGD(TAG, "Creating the messages page");
            return MessageNotificationPage::Factory::create();
        default:
            ESP_LOGE(TAG, "No suitable page is found, fallback to the home");
            return Homepage::Factory::create();
    }
}
}  // namespace view
