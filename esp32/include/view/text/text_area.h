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

    ~TextArea() override;

    void setContent(std::string const& content) override;

    void appendContent(std::string const& content) override;

    void wrapTextVertically() override { m_wrap = true; }

    void doNotWrapText(
        std::function<void(std::string const&)> onExceedingText) override {
        m_wrap = false;
        m_onExceedingText = onExceedingText;
    }

protected:
    void drawOnScreen() override;

private:
    void hideChar(uint32_t, uint32_t, char);
    void printChar(uint32_t row,
                   uint32_t col,
                   char c,
                   uint16_t fg,
                   uint16_t bg);
    void hide(std::string const&, uint32_t, uint32_t);

    uint16_t getRow(uint32_t idx) { return idx / m_maxNumOfColumns; }

    uint16_t getCol(uint32_t idx) { return idx % m_maxNumOfColumns; }

private:
    inline static char const TAG[] = "Text";

private:
    std::string m_content;
    // the characters that can be printed on the screen are in the range [0,
    // m_contentSz)
    uint32_t m_contentSz;

    std::string m_oldContent;
    uint32_t m_oldContentSz;

    bool m_wrap;
    std::function<void(std::string const&)> m_onExceedingText;

    int const m_font;
    TFT_eSPI* m_tft;
    uint16_t const m_fgColour;
    uint16_t const m_bgColour;
    int16_t m_charWidth;
    int16_t m_charHeight;
    uint32_t m_maxChars;
    uint32_t m_maxNumOfColumns;
};
}  // namespace view
