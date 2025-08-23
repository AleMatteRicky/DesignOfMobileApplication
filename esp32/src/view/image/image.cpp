#include "view/image/image.h"
#include "utility/member_fun_bridge.h"
#include "view/image/drawer_impl.h"
#include "view/png_decoder.h"
#include "view/tft.h"

namespace view {
void Image::draw() {
    BinaryImageInfo curBinImg = m_binImages[m_idxCurImage];

    MemberFunctionBridge<Image, int, PNGDRAW*>::setup(this, &Image::pngDraw);

    // load the image
    PNG* png = png::PngDecoder::getPNG();
    int16_t rc =
        png->openFLASH((uint8_t*)curBinImg.m_binData, curBinImg.m_sz,
                       MemberFunctionBridge<Image, int, PNGDRAW*>::wrapper);

    // error when opening the binary image from memory
    if (rc != PNG_SUCCESS) {
        Serial.println("An error occured while drawing the image");
        return;
    }

    /*
    Serial.println("Successfully opened png file");
    Serial.printf("image specs: (%d x %d), %d bpp, pixel type: %d\n",
                  png->getWidth(), png->getHeight(), png->getBpp(),
                  png->getPixelType());
    */

    // setup was a success, proceed
    auto tft = tft::Tft::getTFT_eSPI();
    tft->startWrite();
    rc = png->decode(NULL, 0);

    tft->endWrite();
    /*
   TODO: see maybe it can be optimized further. I guess this is because we are
   not storing the binary format of the image in RAM but getting it from the
   flash memory (use of PROGMEM for this purpose).
    */
    //
    // png.close(); // not needed for memory->memory decode

    MemberFunctionBridge<Image, int, PNGDRAW*>::setup(nullptr, nullptr);
}

int Image::pngDraw(PNGDRAW* pDraw) {
    uint16_t lineBuffer[SCREEN_WIDTH];
    // load the line containing the image here
    PNG* png = png::PngDecoder::getPNG();
    png->getLineAsRGB565(pDraw, lineBuffer, PNG_RGB565_BIG_ENDIAN, 0xffffffff);

    /*
    pDraw->y is the current line of the png to be drawned
    filtering can be applied to exclued some lines, useful to make the effect
    of partially covered icon.
    */
    auto coordinates = getCoordinates();
    TFT_eSPI* tft = tft::Tft::getTFT_eSPI();
    tft->pushImage(coordinates.m_x, coordinates.m_y + pDraw->y, pDraw->iWidth,
                   1, lineBuffer);

    return 1;
}

std::unique_ptr<BinaryImageInfo> Image::toBinary() {
    return std::unique_ptr<BinaryImageInfo>(
        new BinaryImageInfo(m_binImages[m_idxCurImage]));
}
}  // namespace view