#include "view/text/scrollable_text.h"
#include "view/bin_pngs/24/swipe_left.h"
#include "view/bin_pngs/24/swipe_right.h"

namespace view {
ScrollableText::ScrollableText(RectType frame,
                               View* superiorView,
                               std::string const&)
    : View::View(frame, superiorView, "ScrollableText"),
      m_textFramesSz{0},
      m_idxCurFrame{0},
      m_wrapText{true},
      m_showArrows{true},
      m_leftArrow{new Image(RectType{Coordinates{0, 0}, Size{24, 24}},
                            this,
                            {BIN_IMG(24, 24, swipe_left)})},
      m_rightArrow{new Image(RectType{Coordinates{0, 0}, Size{24, 24}},
                             this,
                             {BIN_IMG(24, 24, swipe_right)})} {
    for (auto& pTextArea : m_textFrames) {
        pTextArea = std::make_unique<TextArea>(
            RectType{Coordinates{0, 0}, Size{0, 0}}, nullptr);
        pTextArea->wrapTextVertically(false);
    }
    placeComponents();
}

void ScrollableText::placeComponents() {
    auto [x, y] = getCoordinates();
    ESP_LOGD(TAG, "Scrolltext coordinates: (%d,%d)", x, y);

    auto [w, h] = getSize();
    auto imgSz = m_leftArrow->getSize();
    int16_t yCenter = y + h / 2;
    byte distanceFromHorizontalMargin = 4;
    byte horizontalSpaceForArrows =
        (distanceFromHorizontalMargin + imgSz.m_width +
         distanceFromHorizontalMargin);

    m_leftArrow->moveRespectToTheCenter(
        Coordinates{x + horizontalSpaceForArrows / 2, yCenter});

    ESP_LOGD(TAG, "left arrow coordinates: (%d,%d)",
             m_leftArrow->getCoordinates().m_x,
             m_leftArrow->getCoordinates().m_y);

    m_rightArrow->moveRespectToTheCenter(
        Coordinates{x + w - horizontalSpaceForArrows / 2, yCenter});

    ESP_LOGD(TAG, "right arrow coordinates: (%d,%d)",
             m_rightArrow->getCoordinates().m_x,
             m_rightArrow->getCoordinates().m_y);

    for (auto& pTextArea : m_textFrames) {
        pTextArea->move(Coordinates{x + horizontalSpaceForArrows, y});
        pTextArea->resize(Size{w - 2 * horizontalSpaceForArrows, h});
        ESP_LOGD(TAG,
                 "Placement for the text area. Coordinates: (%d, %d); Size: "
                 "w=%d, h=%d",
                 pTextArea->getCoordinates().m_x,
                 pTextArea->getCoordinates().m_y, pTextArea->getSize().m_width,
                 pTextArea->getSize().m_height);
    }
}

bool ScrollableText::move(Coordinates const& coordinates) {
    View::move(coordinates);
    placeComponents();
    return true;
}

bool ScrollableText::resize(Size const& newSize) {
    View::resize(newSize);
    placeComponents();
    return true;
}

size_t ScrollableText::setContent(std::string const& content) {
    m_textFramesSz = 1;
    m_idxCurFrame = 0;
    auto const& pTextArea = m_textFrames[0];
    size_t fstIdxExceeding = pTextArea->setContent(content);
    ESP_LOGD(TAG,
             "Called ScrollableText::setContent for '%s' results in '%u'chars "
             "exceeding: the string '%s'",
             content.c_str(), content.size() - fstIdxExceeding,
             content.substr(fstIdxExceeding).c_str());
    return handleExceedingText(content, fstIdxExceeding);
}

size_t ScrollableText::appendContent(std::string const& content) {
    if (m_textFramesSz == 0) {
        return setContent(content);
    } else {
        auto const& pTextArea = m_textFrames[m_textFramesSz - 1];
        size_t fstIdxExceeding = pTextArea->appendContent(content);
        ESP_LOGD(TAG,
                 "Called ScrollableText::appendContent for '%s' results in "
                 "'%s' exceeding chars",
                 content.c_str(), content.substr(fstIdxExceeding).c_str());
        return handleExceedingText(content, fstIdxExceeding);
    }
}

size_t ScrollableText::handleExceedingText(std::string const& content,
                                           size_t beg) {
    std::string exceedingContent(content.substr(beg));
    if (exceedingContent.empty())
        return content.size();

    size_t addedChars{0};
    if (m_textFramesSz + 1 <= maxNumText) {
        ESP_LOGD(TAG, "There are exceeding characters yet to place:  '%s'",
                 exceedingContent.c_str());
        auto& pTextFrame = m_textFrames[m_textFramesSz++];
        addedChars = pTextFrame->setContent(exceedingContent);
    } else if (m_wrapText) {
        m_textFramesSz = 0;
        m_idxCurFrame = 0;
        ESP_LOGD(TAG, "No text area left for %s, wrap it",
                 exceedingContent.c_str());
        addedChars = m_textFrames[m_idxCurFrame]->setContent(exceedingContent);
    } else {
        ESP_LOGD(TAG, "No text area left for %s, truncate",
                 exceedingContent.c_str());
        addedChars = 0;
    }
    return beg + addedChars;
}

void ScrollableText::clearFromScreen() {
    if (m_textFramesSz == 0)
        return;
    m_textFrames[m_idxCurFrame]->clearFromScreen();
    m_leftArrow->clearFromScreen();
    m_rightArrow->clearFromScreen();
}

void ScrollableText::wrapTextVertically(bool wrap) {
    m_wrapText = wrap;
}

void ScrollableText::onEvent(SwipeClockwise const&) {
    if (m_idxCurFrame + 1 < m_textFramesSz) {
        m_textFrames[m_idxCurFrame++]->clearFromScreen();
        draw();
    }
}

void ScrollableText::onEvent(SwipeAntiClockwise const&) {
    if (m_idxCurFrame - 1 >= 0) {
        m_textFrames[m_idxCurFrame--]->clearFromScreen();
        draw();
    }
}

void ScrollableText::drawOnScreen() {
    if (m_textFramesSz == 0)
        return;

    ESP_LOGD(TAG, "current frame is at index: %u / %u", m_idxCurFrame,
             m_textFramesSz);
    ESP_LOGD(TAG, "Scrollbar coordinates: (%d,%d)", getCoordinates().m_x,
             getCoordinates().m_y);
    ESP_LOGD(TAG, "Scrollbar size: w = %u, h = %u", getSize().m_width,
             getSize().m_height);

    ESP_LOGD(TAG, "Arrow coordinates: (%d, %d), arrow size: w = %u, h = %u",
             m_leftArrow->getCoordinates().m_x,
             m_leftArrow->getCoordinates().m_y, m_leftArrow->getSize().m_width,
             m_leftArrow->getSize().m_height);
    if (m_idxCurFrame > 0 && m_showArrows) {
        m_leftArrow->makeVisible(true);
        ESP_LOGD(TAG, "Left arrow is visible");
    } else {
        m_leftArrow->clearFromScreen();
        m_leftArrow->makeVisible(false);
        ESP_LOGD(TAG, "Left arrow is hidden");
    }

    m_leftArrow->draw();

    ESP_LOGD(TAG, "Arrow coordinates: (%d, %d), arrow size: w = %u, h = %u",
             m_rightArrow->getCoordinates().m_x,
             m_rightArrow->getCoordinates().m_y,
             m_rightArrow->getSize().m_width, m_rightArrow->getSize().m_height);

    if (m_idxCurFrame + 1 < m_textFramesSz && m_showArrows) {
        m_rightArrow->makeVisible(true);
        ESP_LOGD(TAG, "Right arrow is visible");
    } else {
        m_rightArrow->clearFromScreen();
        m_rightArrow->makeVisible(false);
        ESP_LOGD(TAG, "Right arrow is hidden");
    }

    m_rightArrow->draw();

    auto const& pTextArea = m_textFrames[m_idxCurFrame];
    ESP_LOGD(TAG,
             "Text area coordinates: (%d, %d), text area size: w = %u, h = %u",
             pTextArea->getCoordinates().m_x, pTextArea->getCoordinates().m_y,
             pTextArea->getSize().m_width, pTextArea->getSize().m_height);
    pTextArea->draw();
}

void ScrollableText::showArrows(bool showArrows) {
    ESP_LOGD(TAG, "showing arrows? %d", showArrows);
    m_showArrows = showArrows;
}

}  // namespace view