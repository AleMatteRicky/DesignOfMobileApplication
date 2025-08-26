#pragma once

#include <Arduino.h>
#include <PNGdec.h>
#include <SPI.h>
#include <TFT_eSPI.h>

#include "model/model.h"
#include "view/image/connection_state.h"
#include "view/image/image.h"
#include "view/page/page.h"
#include "view/screen/screen.h"
#include "view/window.h"

namespace view {
class WeatherPage : public Page {
public:
    class Factory {
    public:
        static std::unique_ptr<WeatherPage> create();
    };

protected:
    void drawOnScreen() override;

private:
    inline static std::string const commandName = "w";

    WeatherPage()
        : Page::Page(
              RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
              nullptr) {};
};

}  // namespace view