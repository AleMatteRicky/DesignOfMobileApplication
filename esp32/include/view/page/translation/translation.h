#pragma once

#include <Arduino.h>
#include <PNGdec.h>
#include <SPI.h>
#include <TFT_eSPI.h>

#include "view/image/image.h"
#include "view/page/page.h"
#include "view/screen/screen.h"
#include "view/text/text_area.h"
#include "view/window.h"

namespace view {
class TranslationPage : public Page {
public:
    class Factory {
    public:
        static std::unique_ptr<TranslationPage> create();
    };

    void onEvent(ble::UpdateMessage const&) override;

    PageType getType() override { return PageType::TRANSLATION; }

protected:
    void drawOnScreen() override;

private:
    TranslationPage();

    inline static std::string const commandName = "t";

    inline static char const TAG[] = "TranslationPage";

private:
    TextArea* m_text;
};

}  // namespace view