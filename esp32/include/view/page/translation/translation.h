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
#include "view/text/text.h"
#include "view/window.h"

namespace view {
class TranslationPage : public Page {
public:
    class Factory {
    public:
        static std::unique_ptr<TranslationPage> create();
    };

    void onEvent(ble::UpdateMessage const&) override;

    void onEvent(SwipeAntiClockwise const&) override;
    void onEvent(SwipeClockwise const&) override;

protected:
    void drawOnScreen() override;

private:
    TranslationPage();

    inline static std::string const commandName = "t";  // TODO: check this

private:
    Text* m_text;
};

}  // namespace view