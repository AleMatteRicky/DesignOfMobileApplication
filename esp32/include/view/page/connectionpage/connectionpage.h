#pragma once

#include "view/page/page.h"

namespace view {
class Connectionpage : public Page {
public:
    virtual void draw() {}

    virtual void displayConnectionStateChange() {}

    virtual void showNotification() {}

    virtual void swipe(bool) {}

    virtual void click() {}

    static Connectionpage* getInstance();

private:
    static Connectionpage* instance;
};

}