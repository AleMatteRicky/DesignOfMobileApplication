#pragma once

#include <string>

// Input events
struct Press {
    inline static std::string const name = "press";
};

struct Click {
    inline static std::string const name = "click";
};

struct SwipeClockwise {
    inline static std::string const name = "swipe_clockwise";
};

struct SwipeAntiClockwise {
    inline static std::string const name = "swipe_anti_clockwise";
};

struct DoubleClick {
    inline static std::string const name = "double_click";
};
