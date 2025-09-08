#include "view/text/text_area.h"

#include "fonts/NotoMono18pt.h"
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
      m_oldCursorCoordinates{Coordinates::none},
      m_wrap{true},
      m_center{false} {
    auto frameSz = getSize();
    m_tft->setTextDatum(TL_DATUM);
    m_tft->setTextColor(m_fgColour, m_bgColour);

#if UNICODE
    ESP_LOGD(TAG, "State of the memory before loading the font:");
    ResourceMonitor::printRemainingHeapSizeInfo();
    if (!isFontAlreadyLoaded)
        m_tft->loadFont(NotoMono_18pt);
    isFontAlreadyLoaded = true;
#else
    m_tft->setTextFont(2);
    m_tft->setTextSize(1);
#endif

    m_tft->setTextWrap(false, false);

#if UNICODE
    ESP_LOGD(TAG, "State of the memory after loading the font:");
    ResourceMonitor::printRemainingHeapSizeInfo();

    ESP_LOGD(TAG, "State of the memory before reserving space in advance");
    ResourceMonitor::printRemainingHeapSizeInfo();
#endif

    m_charHeight = m_tft->fontHeight();
    ESP_LOGD(TAG, "char height: %u", m_charHeight);

    auto numCharsWorstCase = getNumCharsWorstCase();

    ESP_LOGD(TAG, "max num of chars per frame in the worst case: %u\n",
             numCharsWorstCase);

    m_currentFrame.allocate(numCharsWorstCase);
    m_oldFrame.allocate(numCharsWorstCase);

    prepareLineBuffer();

    setContent(content);

    ESP_LOGD(TAG, "Text created");
}

TextArea::~TextArea() {}

size_t TextArea::setContent(std::string const& content) {
    m_cursorCoordinates = getCoordinates();

    m_currentFrame.reset();

    m_curLine[0] = '\0';

    size_t charactersWritten = appendContent(content);
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

    auto numBytesWorstCaseForALine = getNumBytesForALineWorstCase();
    if (numBytesWorstCaseForALine == 0) {
        ESP_LOGD(TAG,
                 "The frame is too small: width = %d, height = %d. No text can "
                 "be inserted",
                 frameSz.m_width, frameSz.m_height);
        return 0;
    }

    // use a line because counting each character individually may result in
    // error as the computation would not take into account space adjustements
    char line[numBytesWorstCaseForALine * 4 + 1];

    // when appending text, the previous line needs to be restored
    restoreLine(line);

    auto cursorCoordinates = m_cursorCoordinates;
    byte inc = 0;
    for (uint32_t i = 0; i < content.size(); i += inc) {
        std::string glyph = getUTF8Sequence(content, i);
        assert(glyph.size() > 0 &&
               "the sequence must contain only valid utf-8 encoded strings");

        inc = glyph.size();

        if (overflowOnX(glyph, line)) {
            ESP_LOGD(TAG,
                     "Overflowing along the x axis for the glyph '%s' and the "
                     "cursor's coordinates: "
                     "(%d,%d)",
                     glyph.c_str(), cursorCoordinates.m_x,
                     cursorCoordinates.m_y);

            if (glyph == " " || glyph == "\n")
                continue;

            // to know when drawing the string to break the line
            m_currentFrame.addGlyph("\n");

            cursorCoordinates.m_x = frameCoordinates.m_x;
            cursorCoordinates.m_y += m_tft->fontHeight();
            line[0] = '\0';
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
            line[0] = '\0';
        } else {
            ESP_LOGD(TAG, "'%s' of width %u would be printed at (%u,%u)",
                     glyph.c_str(), m_tft->textWidth(glyph.c_str()),
                     cursorCoordinates.m_x, cursorCoordinates.m_y);
            strcat(line, glyph.c_str());
            cursorCoordinates.m_x =
                frameCoordinates.m_x + m_tft->textWidth(line);
        }
    }

    m_cursorCoordinates = cursorCoordinates;
    strcpy(m_curLine.get(), line);
    return content.size();
}

void TextArea::drawOnScreen() {
    ESP_LOGD(TAG, "Is centered? %d", m_center);
    ESP_LOGD(TAG, "Cursor at (%d, %d)", m_cursorCoordinates.m_x,
             m_cursorCoordinates.m_y);
    ESP_LOGD(TAG, "Old cursor at (%d, %d)", m_oldCursorCoordinates.m_x,
             m_oldCursorCoordinates.m_y);

    // if the old cursor contains data that means the current content will be
    // drawned at a different position than the old content, hence we need to
    // clean the screen even if the two contents may have a common prefix
    if (m_oldCursorCoordinates != Coordinates::none) {
        auto oldCenter = center(m_oldCursorCoordinates);
        m_tft->setCursor(oldCenter.m_x, oldCenter.m_y);
        hide(m_oldFrame, 0);
    }

    Coordinates cursorCoordinates;

    if (m_center) {
        cursorCoordinates = center(m_cursorCoordinates);
    } else {
        cursorCoordinates = getCoordinates();
    }

    ESP_LOGD(TAG, "cursor coordinates: (%d, %d)", cursorCoordinates.m_x,
             cursorCoordinates.m_y);
    auto frameSz = getSize();
    m_tft->setCursor(cursorCoordinates.m_x, cursorCoordinates.m_y);
    bool invalidated{false};
    for (uint32_t i = 0; i < m_currentFrame.size(); i++) {
        char const* glyph = m_currentFrame.getGlyphAt(i);

        if (i < m_oldFrame.size() && !m_oldFrame.hasGlyph(glyph, i)) {
            if (!invalidated) {
                int16_t x = m_tft->getCursorX();
                int16_t y = m_tft->getCursorY();
                // not called if center==true because the oldFrame is hidden at
                // the beginning and its size reset
                hide(m_oldFrame, i);

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
            m_tft->setCursor(cursorCoordinates.m_x, m_tft->getCursorY());
        }
    }

    auto curFrameSz = m_currentFrame.size();
    if (m_oldFrame.size() > curFrameSz)
        hide(m_oldFrame, curFrameSz);
    if (m_center)
        m_oldCursorCoordinates = m_cursorCoordinates;
    else
        // needed when the text is switched from centered to not centered
        m_oldCursorCoordinates = Coordinates::none;
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
    m_oldFrame.eraseFrom(beg);
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

void TextArea::setCenter(bool center, RectType const& reference) {
    m_center = center;

    if (!m_center)
        return;

    auto sz = getSize();
    ESP_LOGD(TAG, "reference dim: w = %u, h = %u; current dim: w = %u, h = %u",
             reference.m_size.m_width, reference.m_size.m_height, sz.m_width,
             sz.m_height);

    assert(reference.m_size.m_width >= sz.m_width &&
           reference.m_size.m_height >= sz.m_height &&
           "The reference must be bigger than this text area");

    m_reference = reference;
}

bool TextArea::resize(Size const& newSize) {
    View::resize(newSize);
    prepareLineBuffer();
    return true;
}

void TextArea::clearFromScreen() {
    if (m_currentFrame.size() == 0)
        return;

    Coordinates cursor = m_oldCursorCoordinates == Coordinates::none
                             ? getCoordinates()
                             : m_oldCursorCoordinates;

    ESP_LOGD(TAG, "Clearing from (%d, %d)", cursor.m_x, cursor.m_y);

    m_tft->setCursor(cursor.m_x, cursor.m_y);

    // before: hide(m_currentFrame, 0);
    hide(m_oldFrame, 0);
}

Coordinates TextArea::center(Coordinates cursor) {
    auto [x, y] = m_reference.m_coordinates;
    auto [w, h] = m_reference.m_size;
    ESP_LOGD(TAG, "info: pos=(%u, %u), w = %u, h = %u, cursor=(%d,%d)", x, y, w,
             h, cursor.m_x, cursor.m_y);

    int16_t textWidth;
    auto frameCoordinates = getCoordinates();
    auto frameSz = getSize();
    if (cursor.m_y > frameCoordinates.m_y)
        textWidth = frameSz.m_width;
    else
        textWidth = cursor.m_x - frameCoordinates.m_x;

    ESP_LOGD(TAG, "Computed text width: %d vs actual width: %d", textWidth,
             m_tft->textWidth(m_curLine.get()));

    int16_t textHeight = cursor.m_y + m_charHeight - frameCoordinates.m_y;
    ESP_LOGD(TAG, "Text height: %d", textHeight);

    uint16_t deltaX = (std::max(w - textWidth, 0)) / 2;
    uint16_t deltaY = (std::max(h - textHeight, 0)) / 2;

    Coordinates centeredCoordinates = {x + deltaX, y + deltaY};
    ESP_LOGD(TAG, "Centered coordinates: (%d,%d)", centeredCoordinates.m_x,
             centeredCoordinates.m_y);
    return centeredCoordinates;
}

void TextArea::restoreLine(char* line) {
    line[0] = '\0';
    ESP_LOGD(TAG, "Current line: '%s'", m_curLine.get());
    strcpy(line, m_curLine.get());
}

// reserve space before hand to avoid dynamic reallocations as new
// characters are added to the text
// reserve space for new lines to know when break the line as the frame's
// borders may not correspond to the screen one.
// NOTE. This function depends on the current frame's size and thus needs to be
// called every time the text area is resized
void TextArea::prepareLineBuffer() {
    auto numCharsWorstCase = getNumCharsWorstCase();
    ESP_LOGD(TAG, "Pre allocating %u characters", numCharsWorstCase);

    m_curLine = std::make_unique<char[]>(getNumBytesForALineWorstCase() + 1);
    m_curLine[0] = '\0';
}
}  // namespace view
