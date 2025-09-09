#pragma once

#include <cstdint>

namespace view {
struct Coordinates {
    int m_x;
    int m_y;
    static Coordinates none;

    bool operator==(Coordinates const& other) {
        return m_x == other.m_x && m_y == other.m_y;
    }

    bool operator!=(Coordinates const& other) {
        return !(this->operator==(other));
    }
};
}  // namespace view