#include "view/roll/roll.h"

namespace view {
void Roll::draw() {
    Serial.println("Drawing a roll");

    View& viewAtCenter = getSubViewAtIndex(m_idxImageAtTheCenter);

    viewAtCenter.resize(Size{64, 64});

    // found empirically
    uint16_t distanceFromSelectionArrow = 30;

    Coordinates centerOfTheRoll = getCenter();
    viewAtCenter.moveRespectToTheCenter(centerOfTheRoll);

    viewAtCenter.draw();

    uint16_t numSubViews = getNumSubViews();

    auto [numDrawnedImages, clockwiseImagesOverflow] =
        drawRoll(true, numSubViews - 1);

    // half anti-clockwise for the remaining views
    int16_t remainingImagesAntiClockWise = numSubViews - numDrawnedImages - 1;

    auto [_, anticlockwiseImagesOverflow] =
        drawRoll(false, remainingImagesAntiClockWise);
}

std::pair<byte, bool> Roll::drawRoll(bool clockwise, byte numFeaturesToDraw) {
    int16_t deltaX = 5;
    int16_t deltaY = 20;

    bool imagesOverflow = false;
    byte numImagesDrawned = 0;
    for (byte i = 1; i <= numFeaturesToDraw && !imagesOverflow; i++) {
        View& prevImg = getImageAtIndex(i - 1, clockwise);
        View& img = getImageAtIndex(i, clockwise);
        img.resize(Size{32, 32});
        auto center = prevImg.getCenter();
        auto prevImgSz = prevImg.getSize();
        auto imgSz = img.getSize();
        int16_t dist = prevImgSz.m_width / 2 + imgSz.m_width / 2 + deltaX;
        int8_t direction = clockwise ? -1 : 1;
        int16_t newCenterX = center.m_x + direction * dist;
        int16_t newCenterY =
            center.m_y + prevImgSz.m_height / 2 - imgSz.m_height / 2 - deltaY;
        if (!areValidCoordinates(
                Coordinates{newCenterX - imgSz.m_width / 2,
                            newCenterY - imgSz.m_height / 2})) {
            imagesOverflow = true;
        } else {
            img.moveRespectToTheCenter(Coordinates{newCenterX, newCenterY});
            img.draw();
            numImagesDrawned = i;
        }
    }

    return {numImagesDrawned, imagesOverflow};
}
}  // namespace view