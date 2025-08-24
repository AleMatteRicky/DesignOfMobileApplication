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
    int16_t deltaX = 20;
    int16_t deltaY = 5;

    bool imagesOverflow = false;
    byte numImagesDrawned = 0;
    for (byte i = 1; i <= numFeaturesToDraw && !imagesOverflow; i++) {
        View& prevImg = getImageAtIndex(i - 1, clockwise);
        View& img = getImageAtIndex(i, clockwise);
        img.resize(Size{32, 32});
        auto prevImgCoordinates = prevImg.getCenter();
        auto prevImgSz = prevImg.getSize();
        auto imgSz = img.getSize();
        int16_t newX = prevImgCoordinates.m_x + deltaX;
        int8_t direction = clockwise ? -1 : 1;
        // use of the center for symmetry when drawing both clockwise and
        // anticlockwise
        int16_t newCenterY =
            prevImgCoordinates.m_y +
            direction * (prevImgSz.m_height / 2 + deltaY + imgSz.m_height / 2);
        int16_t newY = newCenterY - imgSz.m_height / 2;
        if (!areValidCoordinates(Coordinates{newX, newY})) {
            imagesOverflow = true;
        } else {
            img.move(Coordinates{newX, newY});
            img.draw();
            numImagesDrawned = i;
        }
    }

    return {numImagesDrawned, imagesOverflow};
}
}  // namespace view