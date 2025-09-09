#pragma once

#include <PNGdec.h>

namespace png {
class PngDecoder {
public:
    static PNG* getPNG() { return &png; }

private:
    static inline PNG png;
};
}  // namespace png