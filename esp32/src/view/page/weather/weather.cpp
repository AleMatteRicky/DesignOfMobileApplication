#include "view/page/weather/weather.h"

#include <ArduinoJson.h>
#include "ble/remote_dispatcher.h"
#include "controller/central_controller.h"
#include "input/input_manager.h"
#include "utility/resource_monitor.h"
#include "view/bin_pngs/24/swipe_left.h"
#include "view/bin_pngs/24/swipe_right.h"
#include "view/bin_pngs/32/missed-call.h"
#include "view/bin_pngs/32/no-wifi.h"
#include "view/bin_pngs/32/share.h"
#include "view/bin_pngs/32/text.h"
#include "view/bin_pngs/32/weather_conditions/clear.h"
#include "view/bin_pngs/32/weather_conditions/clear_night.h"
#include "view/bin_pngs/32/weather_conditions/clouds_1.h"
#include "view/bin_pngs/32/weather_conditions/clouds_1_night.h"
#include "view/bin_pngs/32/weather_conditions/clouds_2.h"
#include "view/bin_pngs/32/weather_conditions/clouds_3.h"
#include "view/bin_pngs/32/weather_conditions/fog.h"
#include "view/bin_pngs/32/weather_conditions/rain_1.h"
#include "view/bin_pngs/32/weather_conditions/rain_1_night.h"
#include "view/bin_pngs/32/weather_conditions/rain_2.h"
#include "view/bin_pngs/32/weather_conditions/rain_3.h"
#include "view/bin_pngs/32/weather_conditions/rain_4.h"
#include "view/bin_pngs/32/weather_conditions/snow_1.h"
#include "view/bin_pngs/32/weather_conditions/snow_1_night.h"
#include "view/bin_pngs/32/weather_conditions/snow_2.h"
#include "view/bin_pngs/32/weather_conditions/snow_3.h"
#include "view/bin_pngs/32/weather_conditions/squall.h"
#include "view/bin_pngs/32/weather_conditions/thunderstorm_1.h"
#include "view/bin_pngs/32/weather_conditions/thunderstorm_1_3_night.h"
#include "view/bin_pngs/32/weather_conditions/thunderstorm_2.h"
#include "view/bin_pngs/32/weather_conditions/thunderstorm_3.h"
#include "view/bin_pngs/32/weather_conditions/tornado.h"

#include "view/notifications/notification.h"
#include "view/text/text.h"

#define IMG_MAP_ENTRY(name, frame, parent, w, h) \
    {#name, new Image(frame, parent, {BIN_IMG(w, h, name)}, false)}

namespace view {
WeatherPage::WeatherPage()
    : Page::Page(RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
                 nullptr),
      m_txtWhenNoData(
          new TextArea(RectType{Coordinates{0, 60}, Size{SCREEN_WIDTH, 20}},
                       this)),
      m_location(
          new TextArea(RectType{Coordinates{0, 60}, Size{SCREEN_WIDTH, 20}},
                       this)),
      m_time(new TextArea(RectType{Coordinates{0, 0}, Size{100, 20}}, this)),
      m_temperature(
          new TextArea(RectType{Coordinates{0, 0}, Size{80, 20}}, this)),
      m_pressure(
          new TextArea(RectType{Coordinates{0, 0}, Size{100, 20}}, this)),

      m_leftArrow(new Image(RectType{Coordinates{0, 0}, Size{32, 32}},
                            this,
                            {BIN_IMG(32, 32, swipe_left)})),
      m_rightArrow(new Image(RectType{Coordinates{0, 0}, Size{32, 32}},
                             this,
                             {BIN_IMG(32, 32, swipe_right)})),

      m_idxCurCondition{0} {
    auto [x, y] = getCoordinates();
    auto [w, h] = getSize();
    auto arrowSz = m_leftArrow->getSize();
    int16_t yCenter = y + h / 2;
    byte distanceFromHorizontalMargin = 20;
    byte horizontalSpaceForArrows =
        (distanceFromHorizontalMargin + arrowSz.m_width +
         distanceFromHorizontalMargin);

    m_leftArrow->moveRespectToTheCenter(
        Coordinates{x + horizontalSpaceForArrows / 2, yCenter});

    ESP_LOGD(TAG, "left arrow coordinates: (%d,%d)",
             m_leftArrow->getCoordinates().m_x,
             m_leftArrow->getCoordinates().m_y);

    m_rightArrow->moveRespectToTheCenter(
        Coordinates{x + w - horizontalSpaceForArrows / 2, yCenter});

    ESP_LOGD(TAG, "right arrow coordinates: (%d,%d)",
             m_rightArrow->getCoordinates().m_x,
             m_rightArrow->getCoordinates().m_y);

    m_txtWhenNoData->setContent("No data arrived yet");
    m_txtWhenNoData->move(
        Coordinates{x + horizontalSpaceForArrows, yCenter - 50});

    m_location->move(
        Coordinates{x + horizontalSpaceForArrows + 20, yCenter - 50});

    auto [locationX, locationY] = m_location->getCoordinates();

    auto iconSz = Size{32, 32};
    auto center = (iconSz.m_width + m_pressure->getSize().m_width) / 2;

    m_time->move(
        Coordinates{locationX, locationY + m_location->getSize().m_height});

    auto [timeX, timeY] = m_time->getCoordinates();

    uint16_t imgWidth = 32;
    uint16_t imgHeight = 32;
    auto iconCoordinates =
        Coordinates{locationX, timeY + m_time->getSize().m_height};
    auto frameIcon = RectType{iconCoordinates, iconSz};
    auto parentView = this;

    m_mapIdToIcon = std::unordered_map<std::string, Image*>{
        IMG_MAP_ENTRY(clear, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(clear_night, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(clouds_1, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(clouds_1_night, frameIcon, parentView, imgWidth,
                      imgHeight),
        IMG_MAP_ENTRY(clouds_2, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(clouds_3, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(rain_1, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(rain_1_night, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(rain_2, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(rain_3, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(rain_4, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(snow_1, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(snow_1_night, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(snow_2, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(snow_3, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(thunderstorm_1, frameIcon, parentView, imgWidth,
                      imgHeight),
        IMG_MAP_ENTRY(thunderstorm_1_3_night, frameIcon, parentView, imgWidth,
                      imgHeight),
        IMG_MAP_ENTRY(thunderstorm_2, frameIcon, parentView, imgWidth,
                      imgHeight),
        IMG_MAP_ENTRY(thunderstorm_3, frameIcon, parentView, imgWidth,
                      imgHeight),
        IMG_MAP_ENTRY(fog, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(tornado, frameIcon, parentView, imgWidth, imgHeight),
        IMG_MAP_ENTRY(squall, frameIcon, parentView, imgWidth, imgHeight),
    };

    ESP_LOGD(TAG, "Size of the map: %lu", sizeof(m_mapIdToIcon));

    m_temperature->move(Coordinates{iconCoordinates.m_x + iconSz.m_width + 10,
                                    iconCoordinates.m_y});
    auto [temperatureX, temperatureY] = m_temperature->getCoordinates();
    auto [_, temperatureHeight] = m_temperature->getSize();
    m_pressure->move(
        Coordinates{temperatureX, temperatureY + temperatureHeight});
}

std::unique_ptr<WeatherPage> WeatherPage::Factory::create() {
    std::unique_ptr<WeatherPage> weatherPage =
        std::unique_ptr<WeatherPage>(new WeatherPage());

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();
    remoteDispatcher->addObserver(ble::UpdateMessage::name, weatherPage.get());
    auto inputManager = InputManager::getInstance();
    inputManager->addObserver(SwipeClockwise::name, weatherPage.get());
    inputManager->addObserver(SwipeAntiClockwise::name, weatherPage.get());
    return weatherPage;
}

void WeatherPage::onEvent(ble::UpdateMessage const& event) {
    JsonDocument doc;

    DeserializationError error = deserializeJson(doc, event.msg);
    if (error) {
        ESP_LOGD(TAG, "Error '%s' when deserializing\n", error.c_str());
        return;
    }

    if (doc["command"] != commandName)
        return;

    const char* location = doc["location"];

    assert(location);

    m_location->setContent(location);

    JsonArrayConst conditions = doc["conditions"];
    if (conditions.isNull()) {
        ESP_LOGD(TAG, "Error in parsing the Json for the conditions");
        return;
    }

    // make the current icon invisible, skip if none has been displayed yet
    if (!m_conditions.empty()) {
        m_mapIdToIcon[m_conditions[m_idxCurCondition].m_icon]->makeVisible(
            false);
    }
    m_conditions.clear();
    m_idxCurCondition = 0;
    for (JsonVariantConst cond : conditions) {
        char const* time = cond["time"];
        assert(time);
        std::string temperature = cond["temperature"];
        temperature += " °C";
        std::string pressure = cond["pressure"];
        pressure += " hPa";
        char const* iconName = cond["iconName"];
        assert(iconName);
        ESP_LOGD(TAG,
                 "time: '%s'\ttemperature: %u\tpressure: %u\ticonName: '%s'",
                 time, temperature, pressure, iconName);
        m_conditions.push_back(
            Condition{time, temperature, pressure, iconName});
    }

    draw();
}

void WeatherPage::onEvent(SwipeClockwise const&) {
    ESP_LOGD(TAG, "Clockwise is detected with %u conditions",
             m_conditions.size());
    if (m_idxCurCondition + 1 < m_conditions.size()) {
        m_idxCurCondition += 1;
        ESP_LOGD(TAG, "Current index is %u", m_idxCurCondition);
        draw();
    }
}

void WeatherPage::onEvent(SwipeAntiClockwise const&) {
    ESP_LOGD(TAG, "Anti-clockwise is detected");
    if (m_idxCurCondition - 1 >= 0) {
        m_idxCurCondition -= 1;
        ESP_LOGD(TAG, "Current index is %u", m_idxCurCondition);
        draw();
    }
}

void WeatherPage::drawOnScreen() {
    ResourceMonitor::printRemainingStackSize();

    // no message with conditions is arrived yet
    if (m_conditions.empty()) {
        m_txtWhenNoData->draw();
        return;
    }

    m_txtWhenNoData->clearFromScreen();
    // update based on the current condition
    Condition condition = m_conditions[m_idxCurCondition];
    std::string time = condition.m_time;
    ESP_LOGD(TAG, "time: '%s'", time.c_str());
    m_time->setContent(time);
    ESP_LOGD(TAG, "temperature: '%s'", condition.m_temperature.c_str());
    m_temperature->setContent(condition.m_temperature);
    ESP_LOGD(TAG, "pressure: '%s'", condition.m_pressure.c_str());
    m_pressure->setContent(condition.m_pressure);
    m_mapIdToIcon[condition.m_icon]->makeVisible(true);

    // draw the condition
    m_location->draw();
    m_time->draw();
    m_temperature->draw();
    m_pressure->draw();
    m_mapIdToIcon[condition.m_icon]->draw();

    if (m_idxCurCondition + 1 < m_conditions.size())
        m_rightArrow->draw();
    else
        m_rightArrow->clearFromScreen();

    if (m_idxCurCondition - 1 >= 0)
        m_leftArrow->draw();
    else
        m_leftArrow->clearFromScreen();
}
}  // namespace view