#include "view/text/text_area.h"

#include "fonts/NotoSansMono15pt.h"
#include "utility/resource_monitor.h"
#include "view/screen/screen.h"

namespace view {
TextArea::TextArea(RectType frame,
                   View* superiorView,
                   std::string const& content)
    : View::View(frame, superiorView, "Text"),
      m_font{2},
      m_tft{tft::Tft::getTFT_eSPI()},
      m_fgColour{TFT_LIGHTGREY},
      m_bgColour{TFT_BLACK},
      m_cursorCoordinates{Coordinates{getCoordinates()}},
      m_wrap{true} {
    auto frameSz = getSize();
    m_tft->setTextDatum(TL_DATUM);
    m_tft->setTextColor(m_fgColour, m_bgColour);

    ESP_LOGD(TAG, "State of the memory before loading the font:");
    ResourceMonitor::printRemainingHeapSizeInfo();
    // m_tft->loadFont(NotoSansMono_15pt);
    m_tft->setTextFont(1);
    m_tft->setTextSize(2);
    m_tft->setTextWrap(false, false);

    ESP_LOGD(TAG, "State of the memory after loading the font:");
    ResourceMonitor::printRemainingHeapSizeInfo();

    m_charHeight = m_tft->fontHeight();
    ESP_LOGD(TAG, "char height: %u", m_charHeight);
    auto minCharWidth = m_tft->textWidth("a");
    auto numCharsWorstCase =
        (frameSz.m_width * frameSz.m_height) / (minCharWidth * m_charHeight);
    auto numRowsWorstCase = frameSz.m_height / m_charHeight;
    ESP_LOGD(TAG, "max num of chars per frame: %u\n", numCharsWorstCase);

    // reserve space before hand to avoid dynamic reallocations as new
    // characters are added to the text
    // reserve space for new lines to know when break the line as the frame's
    // borders may not correspond to the screen one.
    ESP_LOGD(TAG, "Pre allocating %u characters",
             numCharsWorstCase + numRowsWorstCase);
    m_currentFrame.allocate(numCharsWorstCase + numRowsWorstCase);
    m_oldFrame.allocate(numCharsWorstCase + numRowsWorstCase);

    setContent(content);
    ESP_LOGD(TAG, "Text created");
}

size_t TextArea::setContent(std::string const& content) {
    ESP_LOGD(TAG, "Before setting the content:");
    ResourceMonitor::printRemainingHeapSizeInfo();

    m_cursorCoordinates = getCoordinates();

    m_currentFrame.reset();

    ESP_LOGD(TAG, "After cleaning the current content:");
    ResourceMonitor::printRemainingHeapSizeInfo();

    size_t charactersWritten = appendContent(content);
    ESP_LOGD(TAG, "After setting the content:");
    ResourceMonitor::printRemainingHeapSizeInfo();

    return charactersWritten;
}

size_t TextArea::appendContent(std::string const& content) {
    if (content.empty())
        return 0;

    auto frameSz = getSize();
    auto frameCoordinates = getCoordinates();
    if (!m_wrap &&
        m_cursorCoordinates.m_y >= frameCoordinates.m_y + frameSz.m_height) {
        ESP_LOGD(TAG,
                 "Text already full, no space for '%s'. The cursorY is at %u, "
                 "but the maximum y is %u",
                 content.c_str(), m_cursorCoordinates.m_y,
                 frameCoordinates.m_y + frameSz.m_height);
        return 0;
    }

    ESP_LOGD(TAG, "Appending content '%s'\n", content.c_str());

    auto cursorCoordinates = m_cursorCoordinates;
    byte inc = 0;

    for (uint32_t i = 0; i < content.size(); i += inc) {
        std::string glyph = getUTF8Sequence(content, i);
        assert(glyph.size() > 0 &&
               "the sequence must contain only valid utf-8 encoded strings");

        inc = glyph.size();

        if (overflowOnX(glyph, cursorCoordinates)) {
            ESP_LOGD(TAG,
                     "Overflowing along the x axis for cursor's coordinates: "
                     "(%d,%d)",
                     cursorCoordinates.m_x, cursorCoordinates.m_y);
            if (glyph == " " || glyph == "\n")
                continue;

            // to know when drawing the string to break the line
            m_currentFrame.addGlyph("\n");

            cursorCoordinates.m_x = frameCoordinates.m_x;
            cursorCoordinates.m_y += m_tft->fontHeight();
        }

        if (overFlowOnY(glyph, cursorCoordinates)) {
            if (glyph == " " || glyph == "\n")
                continue;
            ESP_LOGD(TAG,
                     "Overflowing along the y axis for the cursor at (%d,%d)",
                     cursorCoordinates.m_x, cursorCoordinates.m_y);
            if (m_wrap) {
                ESP_LOGD(TAG,
                         "Wrapping the text: cursor back at the initial "
                         "coordinates");
                cursorCoordinates = getCoordinates();
                m_currentFrame.reset();
            } else {
                ESP_LOGD(TAG, "Not wrapping text: truncate it");
                return i;
            }
        }

        // if it is not an indentation, skip it
        if (glyph == " " && beginningOfTheLine(cursorCoordinates) &&
            cursorCoordinates.m_y != getCoordinates().m_y)
            continue;

        m_currentFrame.addGlyph(glyph.c_str());

        if (glyph == "\n") {
            cursorCoordinates = Coordinates{
                frameCoordinates.m_x, cursorCoordinates.m_y + m_charHeight};
        } else {
            ESP_LOGD(TAG, "'%s' of width %u would be printed at (%u,%u)",
                     glyph.c_str(), m_tft->textWidth(glyph.c_str()),
                     cursorCoordinates.m_x, cursorCoordinates.m_y);
            cursorCoordinates.m_x += m_tft->textWidth(glyph.c_str()) + 1;
        }
    }

    m_cursorCoordinates = cursorCoordinates;
    return content.size();
}

void TextArea::drawOnScreen() {
    m_currentFrame.print();

    bool invalidated{false};
    auto frameCoordinates = getCoordinates();
    auto frameSz = getSize();
    m_tft->setCursor(frameCoordinates.m_x, frameCoordinates.m_y);

    for (uint32_t i = 0; i < m_currentFrame.size(); i++) {
        char const* glyph = m_currentFrame.getGlyphAt(i);

        if (i < m_oldFrame.size() && !m_oldFrame.hasGlyph(glyph, i)) {
            if (!invalidated) {
                int16_t x = m_tft->getCursorX();
                int16_t y = m_tft->getCursorY();
                hide(m_oldFrame, i);
                m_oldFrame.eraseFrom(i);

                invalidated = true;
                // restore the cursor's coordinates changed when hiding the old
                // printed content
                m_tft->setCursor(x, y);
            }
        }

        ESP_LOGD(TAG, "Drawing '%s' at (%d,%d)", glyph, m_tft->getCursorX(),
                 m_tft->getCursorY());

        printGlyph(glyph, m_fgColour, m_bgColour);

        if (m_oldFrame.size() < m_currentFrame.size())
            m_oldFrame.addGlyph(glyph);

        if (strcmp(glyph, "\n") == 0) {
            // the library automatically sets the cursor on a new line but at
            // the first column of the screen, which may not be what we want:
            // move it at the first column of the frame
            m_tft->setCursor(frameCoordinates.m_x, m_tft->getCursorY());
        }
    }

    auto curFrameSz = m_currentFrame.size();
    if (m_oldFrame.size() > curFrameSz)
        hide(m_oldFrame, curFrameSz);
}

void TextArea::hideGlyph(char const* glyph) {
    ESP_LOGD(TAG, "Hiding glyph: '%s' at (%d,%d)", glyph, m_tft->getCursorX(),
             m_tft->getCursorY());
    printGlyph(glyph, m_bgColour, m_bgColour);
}

void TextArea::printGlyph(char const* glyph, uint16_t fg, uint16_t bg) {
    m_tft->setTextColor(fg, bg);
    m_tft->print(glyph);
}

void TextArea::hide(Frame const& textFrame, uint32_t beg) {
    auto frameCoordinates = getCoordinates();
    for (uint32_t i = beg; i < textFrame.size(); i++) {
        hideGlyph(textFrame.getGlyphAt(i));

        if (textFrame.hasGlyph("\n", i)) {
            ESP_LOGD(TAG, "Glyph at %lu there is a new line", i);
            m_tft->setCursor(frameCoordinates.m_x, m_tft->getCursorY());
        }
    }
}

std::pair<size_t, byte> TextArea::sizeContent(std::string const& content) {
    size_t totWidth{0};
    byte inc{0};
    for (size_t i = 0; i < content.size(); i++) {
        byte inc = getUTF8SequenceLength(content[i]);
        assert(inc > 0);
        totWidth += m_tft->textWidth(content.substr(i, inc).c_str());
    }
    return {totWidth, m_charHeight};
}

std::string TextArea::getUTF8Sequence(std::string const& str, size_t idx) {
    unsigned char fstByte = str[idx];
    byte length = getUTF8SequenceLength(fstByte);
    assert(length != 0 && "invalid utf-8 string");
    return str.substr(idx, length);
}

byte TextArea::getUTF8SequenceLength(unsigned char firstByte) {
    if (firstByte <= 0x7F)
        return 1;  // ASCII
    if ((firstByte & 0xE0) == 0xC0)
        return 2;  // 2-byte sequence
    if ((firstByte & 0xF0) == 0xE0)
        return 3;  // 3-byte sequence
    if ((firstByte & 0xF8) == 0xF0)
        return 4;  // 4-byte sequence
    return 0;      // Invalid
}

void TextArea::clearFromScreen() {
    if (m_currentFrame.size() == 0)
        return;
    auto [x, y] = getCoordinates();
    m_tft->setCursor(x, y);
    hide(m_currentFrame, 0);
}

}  // namespace view
