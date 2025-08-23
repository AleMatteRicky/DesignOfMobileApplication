#include "view/screen/screen.h"

namespace view {
bool areValidCoordinates(Coordinates const& coordinates) {
    return 0 <= coordinates.m_x && coordinates.m_x < SCREEN_WIDTH &&
           0 <= coordinates.m_y && coordinates.m_y < SCREEN_HEIGHT;
}
}  // namespace view