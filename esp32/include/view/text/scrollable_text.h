#pragma once

#include "view/image/image.h"
#include "view/text/text.h"
#include "view/text/text_area.h"

namespace view {
class ScrollableText : public Text, public View {
public:
    ScrollableText(RectType frame, View* superiorView, std::string const&);

    size_t setContent(std::string const& content) override;

    size_t appendContent(std::string const& content) override;

    void wrapTextVertically(bool wrap) override;

    bool isEmpty() override {
        auto const& pTextArea = m_textFrames[0];
        return !pTextArea || pTextArea->isEmpty();
    }

    void onEvent(SwipeClockwise const&) override;

    void onEvent(SwipeAntiClockwise const&) override;

    bool resize(Size const& newSize) override;

    bool move(Coordinates const& coordinates) override;

    void showArrows(bool showArrows);

    void clearFromScreen() override;

protected:
    void drawOnScreen() override;

private:
    void placeComponents();

    size_t handleExceedingText(std::string const& content, size_t beg);

private:
    inline static constexpr byte maxNumText = 3;

    inline static char const TAG[] = "ScrollableText";

private:
    // use unique_ptr to take ownership of the TextArea
    std::array<std::unique_ptr<TextArea>, maxNumText> m_textFrames;
    byte m_textFramesSz;
    byte m_idxCurFrame;
    bool m_wrapText;
    std::function<void(std::string const&)> m_onExceedingText;

    // indicators when the text exceeds the frame
    Image* m_leftArrow;
    Image* m_rightArrow;
    bool m_showArrows;
};
}  // namespace view