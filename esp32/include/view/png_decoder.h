#pragma once

#include <PNGdec.h>

namespace png {
class PngDecoder {
public:
    static PNG* getPNG() {
        if (png)
            return png;
        png = new PNG();
        return png;
    }

private:
    static inline PNG* png = nullptr;
};
}  // namespace png