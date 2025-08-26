#include "view/text/text.h"

#include "view/screen/screen.h"

namespace view {

void Text::setContent(const std::string& content) {
    // clean previously saved frames
    m_frames.clear();
    m_frames.push_back(TextFrame{""});
    appendContent(content);
    m_currentTextFrame = 0;
}

void Text::appendContent(std::string const& content) {
    auto size = getSize();
    size_t maxCharPerFrame =
        (size.m_width * size.m_height) / (m_charWidth * m_charHeight);

    int frameIdx = m_frames.size() - 1;
    for (int i = 0; i < content.size(); i++) {
        TextFrame& frame = m_frames[frameIdx];
        // there is still space in this frame, add the characters here
        if (frame.m_content.size() < maxCharPerFrame) {
            // pad the position in case of a newline
            if (content[i] == '\n') {
                frame.m_content.push_back(' ');
                uint16_t maxNumCharPerColumn = size.m_width / m_charWidth;
                uint16_t curCol =
                    (frame.m_content.size() - 1) % maxNumCharPerColumn;
                uint16_t numPositionsToPad = maxNumCharPerColumn - curCol - 1;
                for (int j = 0;
                     j < std::min((size_t)numPositionsToPad,
                                  maxCharPerFrame - frame.m_content.size());
                     j++) {
                    frame.m_content.push_back(' ');
                }

            } else {
                frame.m_content.push_back(content[i]);
            }
        } else {
            Serial.println("new frame created");
            // the current frame is full, create a new one and add the current
            // character
            TextFrame newFrame;
            // if the current character is a new line, there is no need to add
            // it as a new frame automatically creates a new line
            if (content[i] != '\n')
                newFrame.m_content.push_back(content[i]);
            m_frames.push_back(newFrame);
            frameIdx += 1;
        }
    }
}

void Text::drawOnScreen() {
#if 0
// TODO: ask Ale for the intended behaviour, but I think the user needs to be
// automatically redirected to the last frame to avoid confusion from text screen
// appearing at a rate impossible for an human to catch
    for (int frameIdx = currentTextFrame; frameIdx < frames.size();
         frameIdx++) {
        TextFrame const& frame = frames[frameIdx];
        std::string const& frameContent = frame.content;
        int row = frame.firstRowWhenDrawingTheFrame;
        int col = frame.firstColumnWhenDrawingTheFrame;
        int numRows = frameContent.size() / maxNumCharPerCol;
        while (row <= numRows) {
            std::string line;
            for (int i = 0;
                 i < std::min(maxNumCharPerCol - col, (int)frameContent.size());
                 i++) {
                line += frameContent[i];
            }
            m_tft->drawString(line.c_str(), col, row * m_heightPerChar);
            row += 1;
            col = 0;
        }
        // new frame, reset the cursor's coordinates
        row = 0;
        col = 0;
    }
#endif
    m_currentTextFrame = m_frames.size() - 1;
    drawFrame(m_currentTextFrame);
}

void Text::onEvent(SwipeClockwise const&) {
    if (m_currentTextFrame + 1 < m_frames.size()) {
        m_currentTextFrame += 1;
        drawFrame(m_currentTextFrame);
    }
}

void Text::onEvent(SwipeAntiClockwise const&) {
    if (m_currentTextFrame - 1 >= 0) {
        m_currentTextFrame -= 1;
        drawFrame(m_currentTextFrame);
    }
}

void Text::drawFrame(byte frameIdx) {
    Serial.printf(
        "There are %ld frames, but I am going to draw only the one at %ld\n",
        m_frames.size(), frameIdx);
    TextFrame const& frame = m_frames[frameIdx];
    std::string const& content = frame.m_content;
    clearFromScreen();
    m_tft->setTextColor(m_fgColour, m_bgColour);
    auto frameSz = getSize();
    uint32_t maxNumCharPerCol = frameSz.m_width / m_charWidth;
    int maxCharPerFrame =
        (frameSz.m_width * frameSz.m_height) / (m_charWidth * m_charHeight);
    assert(content.size() < maxCharPerFrame);
    uint32_t numRows = content.size() / maxNumCharPerCol;
    for (uint32_t r = 0; r <= numRows; r++) {
        std::string line;
        uint32_t numCharsForTheCurCol =
            std::min(maxNumCharPerCol, content.size() - r * maxNumCharPerCol);
        for (uint32_t c = 0; c < numCharsForTheCurCol; c++) {
            line.push_back(content[r * maxNumCharPerCol + c]);
        }
        Serial.printf("Printing the line %s\n", line.c_str());
        auto coordinates = getCoordinates();
        m_tft->drawString(line.c_str(), coordinates.m_x,
                          coordinates.m_y + r * m_charHeight);
    }
}

}  // namespace view