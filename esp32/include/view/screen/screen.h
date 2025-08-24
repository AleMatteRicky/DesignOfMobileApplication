#pragma once

#define SCREEN_WIDTH 320
#define SCREEN_HEIGHT 240

#include <cstdint>
#include "view/coordinates.h"

namespace view {
bool areValidCoordinates(Coordinates const&);
}