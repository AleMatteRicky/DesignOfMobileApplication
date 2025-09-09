#pragma once

#include <string>

// Input events
struct Press {
    inline static char const name[] = "press";
};

struct Click {
    inline static char const name[] = "click";
};

struct SwipeClockwise {
    inline static char const name[] = "swipe_clockwise";
};

struct SwipeAntiClockwise {
    inline static char const name[] = "swipe_anti_clockwise";
};