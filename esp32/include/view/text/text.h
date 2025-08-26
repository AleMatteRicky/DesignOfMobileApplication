#pragma once

#include "view/view.h"

namespace view {
class Text : public View {
public:
    Text(RectType frame, View* superiorView, std::string const& content = "")
        : View::View(frame, superiorView, "text"),
          m_font{2},
          m_tft{tft::Tft::getTFT_eSPI()},
          m_fgColour{TFT_WHITE},
          m_bgColour{TFT_BLACK},
          m_currentTextFrame{0} {
        m_tft->setTextDatum(TL_DATUM);
        auto coordinates = getCoordinates();
        auto size = getSize();
        m_tft->setTextFont(2);
        m_charWidth = m_tft->textWidth("A");
        m_charHeight = m_tft->fontHeight();
        m_frames.push_back(TextFrame{""});

        setContent(content);
    }

    void setContent(std::string const& content);

    void appendContent(std::string const& content);

    void onEvent(SwipeClockwise const&) override;

    void onEvent(SwipeAntiClockwise const&) override;

protected:
    void drawOnScreen() override;

private:
    void drawFrame(byte);

private:
    struct TextFrame {
        std::string m_content;
    };
    std::vector<TextFrame> m_frames;  // list of frames
    byte m_currentTextFrame;  // index to keep track of the current frame

    int const m_font;
    TFT_eSPI* m_tft;
    uint16_t const m_fgColour;
    uint16_t const m_bgColour;
    int16_t m_charWidth;
    int16_t m_charHeight;
};
}  // namespace view
