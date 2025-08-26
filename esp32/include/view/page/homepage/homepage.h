#pragma once

#include <Arduino.h>
#include <PNGdec.h>
#include <SPI.h>
#include <TFT_eSPI.h>

#include "model/model.h"
#include "view/image/connection_state.h"
#include "view/image/image.h"
#include "view/page/page.h"
#include "view/roll/roll.h"
#include "view/screen/screen.h"
#include "view/window.h"

namespace view {
class Homepage : public Page {
public:
    class Factory {
    public:
        static std::unique_ptr<Homepage> create();
    };

    void drawOnScreen() override;

private:
    Homepage()
        : Page::Page(
              RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
              nullptr) {}
};

}  // namespace view