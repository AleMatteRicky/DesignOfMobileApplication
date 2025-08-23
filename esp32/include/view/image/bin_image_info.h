#pragma once

#include <Arduino.h>
#include <cstdint>

namespace view {
struct BinaryImageInfo {
    uint16_t m_height;
    uint16_t m_width;
    size_t m_sz;
    byte const* m_binData;
};

}  // namespace view