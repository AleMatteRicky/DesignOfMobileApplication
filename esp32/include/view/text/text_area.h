#pragma once

#include "view/text/text.h"
#include "view/view.h"

namespace view {
class TextArea : public Text, public View {
public:
    TextArea(RectType frame,
             View* superiorView,
             std::string const& content = "");

    /*
    TextArea() : TextArea(RectType{Coordinates{0, 0}, Size{0, 0}}, nullptr) {}
    */

    size_t setContent(std::string const& content) override;

    size_t appendContent(std::string const& content) override;

    void wrapTextVertically(bool wrap) override { m_wrap = wrap; }

    bool isEmpty() override { return m_content.empty(); }

    void clearFromScreen() override;

protected:
    void drawOnScreen() override;

public:
    static std::string getUTF8Sequence(std::string const& str, size_t idx);

    static byte getUTF8SequenceLength(unsigned char firstByte);

    std::pair<size_t, byte> sizeContent(std::string const&);

private:
    void hideGlyph(std::string const& glyph);
    void printGlyph(std::string const& glyph, uint16_t fg, uint16_t bg);
    void hide(std::vector<std::string> const& content, uint32_t beg);
    bool overflowOnX(std::string const& glyph, Coordinates cursorCoordinates) {
        auto frameSz = getSize();
        auto frameCoordinates = getCoordinates();
        return cursorCoordinates.m_x + m_tft->textWidth(glyph.c_str()) >
               frameCoordinates.m_x + frameSz.m_width;
    }

    bool overFlowOnY(std::string const& glyph, Coordinates cursorCoordinates) {
        auto frameSz = getSize();
        auto frameCoordinates = getCoordinates();
        return cursorCoordinates.m_y + m_tft->fontHeight() >
               frameCoordinates.m_y + frameSz.m_height;
    }

    bool beginningOfTheLine(Coordinates cursorCoordinates) {
        auto frameCoordinates = getCoordinates();
        return cursorCoordinates.m_x == frameCoordinates.m_x;
    }

private:
    inline static char const TAG[] = "Text";

private:
    Coordinates m_cursorCoordinates;
    std::vector<std::string> m_content;
    size_t m_contentSz;
    std::vector<std::string> m_oldContent;
    size_t m_oldContentSz;

    bool m_wrap;

    int const m_font;
    TFT_eSPI* m_tft;
    uint16_t const m_fgColour;
    uint16_t const m_bgColour;
    int16_t m_charHeight;
};
}  // namespace view
