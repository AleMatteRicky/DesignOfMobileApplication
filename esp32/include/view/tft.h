#pragma once

#include <SPI.h>
#include <TFT_eSPI.h>  // always matching the pin configuration

namespace tft {
class Tft {
public:
    static TFT_eSPI* getTFT_eSPI() {
        if (instance)
            return instance;
        instance = new TFT_eSPI();
        instance->begin();
        instance->setRotation(3);
        instance->fillScreen(TFT_BLACK);
        return instance;
    }

private:
    Tft();
    static inline TFT_eSPI* instance = nullptr;
};
}  // namespace tft