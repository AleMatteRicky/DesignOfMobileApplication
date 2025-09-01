#include "view/text/text_area.h"

#include "fonts/RobotoMono_Regular9pt7b.h"
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
      m_contentSz{0},
      m_oldContentSz{0},
      m_wrap{true} {
#if 0
    auto size = getSize();
    ESP_LOGD(TAG,"Creating a sprite of size: %d and %d\n", size.m_width,
                  size.m_height);
    ESP_LOGD(TAG,"The sprite is %u bytes\n", sizeof(m_sprite));
    // ResourceMonitor::printRemainingHeapSizeInfo();
    m_sprite.setColorDepth(4);
    m_sprite.createSprite(size.m_width, size.m_height);
    //  if the sprite fails draw directly on the screen
    assert(m_sprite.getPointer() && "no enough memory for the sprite");
    if (!m_sprite.getPointer()) {
        ESP_LOGD(TAG,"Sprite creation failed!");
        //        assert(false && "No enough memory for the sprite");
    } else {
        ESP_LOGD(TAG,"Sprite created successfully.");
    }
    m_sprite.setTextColor(m_fgColour, m_bgColour);
    m_sprite.setTextFont(m_font);
    m_sprite.setTextSize(1);

    /*
    for (int i = 0; i < 500; i++) {
        m_sprite.printf("line %d\n", i);
    }
    */
    auto coordinates = getCoordinates();
    m_charWidth = m_sprite.textWidth("A");
    m_charHeight = m_sprite.fontHeight();
    m_frames.push_back(TextFrame{""});

    setContent(content);
#endif
    auto frameSz = getSize();
// create a sprite for the current line
#if 0
    auto frameSz = getSize();
    m_sprite.setTextFont(m_font);
    m_sprite.setTextSize(1);
    m_sprite.setColorDepth(4);
    m_charWidth = m_sprite.textWidth("A");
    m_charHeight = m_sprite.fontHeight();
    m_sprite.createSprite(frameSz.m_width, m_charHeight);
#endif

    m_tft->setTextDatum(TL_DATUM);
    m_tft->setTextColor(m_fgColour, m_bgColour);
    m_tft->setFreeFont(&RobotoMono_Regular9pt7b);
    m_charWidth = m_tft->textWidth("A");
    m_charHeight = m_tft->fontHeight();
    ESP_LOGD(TAG, "char width: %u, char height: %u", m_charWidth, m_charHeight);
    m_maxChars =
        (frameSz.m_width * frameSz.m_height) / (m_charWidth * m_charHeight);

    ESP_LOGD(TAG, "max char per frame: %u\n", m_maxChars);
    m_maxNumOfColumns = frameSz.m_width / m_charWidth;
    ESP_LOGD(TAG, "max num of chars per column: %u\n", m_maxNumOfColumns);
    // reserve space before hand to avoid dynamic reallocations as new
    // characters are added to the text
    m_content.resize(m_maxChars, ' ');
    m_oldContent.resize(m_maxChars, ' ');

    setContent(content);
    ESP_LOGD(TAG, "Text created");
}

TextArea::~TextArea() {
    ESP_LOGD(TAG, "Deleting the sprite");
}

void TextArea::setContent(std::string const& content) {
    m_contentSz = 0;
    appendContent(content);
}

void TextArea::appendContent(std::string const& content) {
    if (!m_wrap && m_contentSz == m_maxChars) {
        ESP_LOGD(TAG, "Text already  full, no space for '%s'\n",
                 content.c_str());
        m_onExceedingText(content);
        return;
    }

    ESP_LOGD(TAG, "Appending content '%s'\n", content.c_str());
    for (size_t i = 0; i < content.size(); i++) {
        char c = content[i];
        m_content[m_contentSz] = c;
        if (c == '\n') {
            uint16_t col = getCol(m_contentSz);
            m_contentSz += m_maxNumOfColumns - col;
        } else {
            bool beginningOfNewLine = m_contentSz % m_maxNumOfColumns == 0;
            bool indenting = beginningOfNewLine && c == ' ';
            if (!indenting)
                m_contentSz += 1;
        }

        if (m_contentSz == m_maxChars) {
            if (m_wrap) {
                m_contentSz = 0;
            } else {
                m_onExceedingText(content.substr(i + 1));
                return;
            }
        }
    }
}

void TextArea::hideChar(uint32_t row, uint32_t col, char c) {
    printChar(row, col, c, m_bgColour, m_bgColour);
}

void TextArea::printChar(uint32_t row,
                         uint32_t col,
                         char c,
                         uint16_t fg,
                         uint16_t bg) {
    m_tft->setTextColor(fg, bg);
    auto [x, y] = getCoordinates();
    m_tft->setCursor(x + col * m_charWidth, y + row * m_charHeight);
    m_tft->print(c);
}

void TextArea::hide(std::string const& data, uint32_t beg, uint32_t end) {
    if (beg == end)
        return;
    auto frameSz = getSize();
    uint16_t maxNumOfColumns = frameSz.m_width / m_charWidth;
    for (uint32_t i = beg; i < end; i++) {
        // new line is not printed
        if (data[i] != '\n') {
            uint16_t row = getRow(i);
            uint16_t col = getCol(i);
            hideChar(row, col, data[i]);
            ESP_LOGD(TAG, "hide char %c at (%u, %u)\n", data[i], row, col);
        }
    }
}

void TextArea::drawOnScreen() {
    ESP_LOGD(TAG, "Screen size: %u\n", m_contentSz);
    ESP_LOGD(TAG, "Content to print:\n");
    for (int i = 0; i < m_contentSz; i++) {
        ESP_LOGD(TAG, "%c", m_content[i]);
    }
    ESP_LOGD(TAG, "\nOld content:\n");
    for (int i = 0; i < m_oldContentSz; i++) {
        ESP_LOGD(TAG, "%c", m_oldContent[i]);
    }
    ESP_LOGD(TAG, );

    assert(m_contentSz < m_maxChars);

    auto frameSz = getSize();

    uint32_t i = 0;
    while (i < m_contentSz) {
        ESP_LOGD(TAG, "i = %u\n", i);
        char curChar = m_content[i];
        if (i < m_oldContentSz) {
            char oldChar = m_oldContent[i];
            if (curChar != oldChar) {
                uint16_t numCharsToHide = 1;
                if (curChar == '\n') {
                    uint16_t col = getCol(i);
                    numCharsToHide = std::min(
                        m_oldContentSz - i,
                        static_cast<uint32_t>(m_maxNumOfColumns - col - 1));
                }
                hide(m_oldContent, i, i + numCharsToHide);
            }
        }

        uint16_t col = getCol(i);
        uint16_t row = getRow(i);
        if (curChar == '\n') {
            i += m_maxNumOfColumns - col;
        } else {
            ESP_LOGD(TAG, "Print char %c at (%u, %u)\n", curChar, row, col);
            printChar(row, col, curChar, m_fgColour, m_bgColour);
            m_oldContent[i] = curChar;
            i += 1;
        }
    }

    // the oldContent might be longer, hence hide all remaining characters
    if (m_oldContentSz > m_contentSz) {
        ESP_LOGD(TAG, "Hiding the remaining characters");
        hide(m_oldContent, m_contentSz, m_oldContentSz);
    }
    m_oldContentSz = m_contentSz;
}

}  // namespace view
