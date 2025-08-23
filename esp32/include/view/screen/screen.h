#pragma once

#define SCREEN_WIDTH 240
#define SCREEN_HEIGHT 320

#include <cstdint>
#include "view/coordinates.h"

namespace view {
bool areValidCoordinates(Coordinates const&);
}