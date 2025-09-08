#pragma once

#include "view/text/text.h"
#include "view/tft.h"
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

    ~TextArea();

    size_t setContent(std::string const& content) override;

    size_t appendContent(std::string const& content) override;

    void wrapTextVertically(bool wrap) override { m_wrap = wrap; }

    bool isEmpty() override { return m_currentFrame.size() == 0; }

    void clearFromScreen() override;

    void setCenter(bool center, RectType const& reference);

    bool resize(Size const& newSize) override;

protected:
    void drawOnScreen() override;

private:
    void prepareLineBuffer();

    uint16_t getMinCharWidth() { return m_tft->textWidth("i"); }

    uint16_t getNumRowsWorstCase() { return getSize().m_height / m_charHeight; }

    byte getNumColumnsWorstCase() {
        auto w = getMinCharWidth();
        assert(w > 0 && "minimum char width cannot be zero");
        return getSize().m_width / w;
    }

    size_t getNumCharsWorstCase() {
        auto frameSz = getSize();
        // the additional numRowsWorstCase is due to the '\n' added to break the
        // line when printing characters
        return (frameSz.m_width * frameSz.m_height) /
                   (getMinCharWidth() * m_charHeight) +
               getNumRowsWorstCase();
    }

    byte getNumBytesForALineWorstCase() {
        // 4 because an unicode character can be encoded in at most 4 bytes
        return getNumColumnsWorstCase() * 4;
    }

public:
    static std::string getUTF8Sequence(std::string const& str, size_t idx);

    static byte getUTF8SequenceLength(unsigned char firstByte);

    std::pair<size_t, byte> sizeContent(std::string const&);

private:
    class Frame {
    public:
        Frame() : m_textSz{0} {}
        // Frame(size_t numGlyphs) : m_textSz{0} {
        // m_frameText.resize(numGlyphs); }

        void addGlyph(char const* glyph) {
            assert(strlen(glyph) < 5);
            if (m_textSz >= m_frameText.size()) {
                m_frameText.push_back(std::array<char, 5>());
            }
            strcpy(std::data(m_frameText[m_textSz++]), glyph);
        }

        void allocate(size_t numGlyphs) { m_frameText.resize(numGlyphs); }

        void reset() { m_textSz = 0; }

        size_t size() const { return m_textSz; }

        char const* getGlyphAt(size_t i) const {
            assert(i < m_textSz);
            return std::data(m_frameText[i]);
        }

        bool hasGlyph(char const* glyph, size_t i) const {
            assert(i < m_textSz);
            return strcmp(std::data(m_frameText[i]), glyph) == 0;
        }

        void eraseFrom(size_t i) { m_textSz = i; }

        void print() {
            Serial.println("#####");
            Serial.println("Frame: ");
            for (size_t i = 0; i < m_textSz; i++) {
                Serial.printf("%s", std::data(m_frameText[i]));
            }
            Serial.println("#####");
        }

    private:
        std::vector<std::array<char, 5>> m_frameText;
        size_t m_textSz;
    };

private:
    void hideGlyph(char const* glyph);
    void printGlyph(char const* glyph, uint16_t fg, uint16_t bg);
    void hide(Frame const& frame, uint32_t beg);
    bool overflowOnX(std::string const& glyph, char const* line) {
        auto frameSz = getSize();
        auto frameCoordinates = getCoordinates();
        char buf[strlen(line) + glyph.length() + 1];
        buf[0] = '\0';
        strcpy(buf, line);
        strcat(buf, glyph.c_str());
        uint16_t xCursorIfAddingTheGlyph =
            frameCoordinates.m_x + m_tft->textWidth(buf);
        ESP_LOGD(TAG, "cursorX if the glyph '%s' was added: %u", glyph.c_str(),
                 xCursorIfAddingTheGlyph);
        return xCursorIfAddingTheGlyph > frameCoordinates.m_x + frameSz.m_width;
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

    // compute the coordinates of the center based on the text stored in this
    // TextArea
    Coordinates center(Coordinates cursor);

    void restoreLine(char* line);

private:
    inline static char const TAG[] = "Text";

    inline static bool isFontAlreadyLoaded = false;

private:
    Coordinates m_cursorCoordinates;

    Coordinates m_oldCursorCoordinates;

    // line saved, useful when appending text to restore the last line
    std::unique_ptr<char[]> m_curLine;

    Frame m_currentFrame;

    Frame m_oldFrame;

    bool m_wrap;
    bool m_center;
    RectType m_reference;

    int const m_font;
    TFT_eSPI* m_tft;
    uint16_t const m_fgColour;
    uint16_t const m_bgColour;
    uint16_t m_charHeight;
};
}  // namespace view
