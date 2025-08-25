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
class TranslationPage : public Page {
public:
    class Factory {
    public:
        static std::unique_ptr<TranslationPage> create();
    };

protected:
    void drawOnScreen() override;

private:
    TranslationPage(Window* window)
        : Page::Page(
              RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
              window) {};
    inline static std::string const commandName = "t"; //TODO: check this
};

}  // namespace view