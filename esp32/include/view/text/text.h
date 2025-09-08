#pragma once

#include "view/view.h"

namespace view {
class Text {
public:
    /**
     * Sets the content of this Text equal to the characters in @p{content}
     * accordingly to the wrap policy specified by the method
     * \see{view::Text::wrapTextVertically}.
     * @return the number of characters in 'content' correctly set to
     * this Text.
     */
    virtual size_t setContent(std::string const& content) = 0;

    /**
     * Appends the characters in @p{content} accordingly to the wrap policy
     * specified by the methdo \see{view::Text::wrapTextVertically}.
     * @return the number of characters in 'content' correctly appened in
     * this Text.
     */
    virtual size_t appendContent(std::string const& content) = 0;

    /**
     * Sets the policy when the text exceeds the vertical limit imposed by this
     * Text. If true, the exceeding characters are wrapped vertically, so the
     * exceeding characters are inserted from the beginning of this Text. If
     * false, the execeeding characters are discarded.
     * @param wrap the policy to adopt when this Text is full.
     */
    virtual void wrapTextVertically(bool wrap) = 0;

    virtual bool isEmpty() = 0;
};

}  // namespace view