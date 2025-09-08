#pragma once

#include <Arduino.h>
#include <PNGdec.h>
#include <SPI.h>
#include <TFT_eSPI.h>

#include "view/image/image.h"
#include "view/page/page.h"
#include "view/screen/screen.h"
#include "view/text/text.h"
#include "view/text/text_area.h"
#include "view/window.h"

namespace view {
class WeatherPage : public Page {
public:
    class Factory {
    public:
        static std::unique_ptr<WeatherPage> create();
    };

    void onEvent(ble::UpdateMessage const&);

    void onEvent(SwipeClockwise const&);

    void onEvent(SwipeAntiClockwise const&);

    PageType getType() override { return PageType::WEATHER; }

protected:
    void drawOnScreen() override;

private:
    inline static std::string const commandName = "w";

    inline static char const TAG[] = "Weather";

private:
    WeatherPage();

    TextArea* m_txtWhenNoData;
    TextArea* m_location;
    TextArea* m_time;
    TextArea* m_temperature;
    TextArea* m_pressure;
    Image* m_leftArrow;
    Image* m_rightArrow;
    std::unordered_map<std::string, Image*> m_mapIdToIcon;

    struct Condition {
        std::string m_time;
        std::string m_temperature;
        std::string m_pressure;
        std::string m_icon;
    };
    std::vector<Condition> m_conditions;
    byte m_idxCurCondition;
};
}  // namespace view