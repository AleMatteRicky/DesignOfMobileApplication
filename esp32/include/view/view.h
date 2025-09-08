#pragma once

#include <Arduino.h>
#include "input/input_manager.h"
#include "view/coordinates.h"
#include "view/rectangular_type.h"
#include "view/remote_responder.h"
#include "view/responder.h"
#include "view/screen/screen.h"
#include "view/size.h"
#include "view/tft.h"

namespace view {

/**
 * Class representing an objcect part of the UI.
 * Views are arranged in a tree, defining the so called View-hierarchy
 */
class View : public Responder, public RemoteResponder {
public:
    View(RectType frame,
         View* superiorView,
         std::string const& tag,
         bool isVisible = true)
        : m_frame(frame),
          m_parentView(nullptr),
          m_tag(tag),
          m_isVisible(isVisible) {
        if (superiorView != nullptr) {
            superiorView->appendSubView(std::unique_ptr<View>(this));
        }
    }

    virtual ~View();

    void draw() {
        if (m_isVisible) {
            drawOnScreen();
        }
    }

    virtual void clearFromScreen() {
        auto tft = tft::Tft::getTFT_eSPI();
        auto coordinates = getCoordinates();
        auto size = getSize();
        tft->fillRect(coordinates.m_x, coordinates.m_y, size.m_width,
                      size.m_height, TFT_BLACK);
    }

    /**
     * Moves the view, already present in the UI tree, to the tree rooted in
     * this View, if the provided view is not already present, otherwise skip
     * the insertion. The view will be inserted as the right most node among the
     * children of this View. The function automatically sets the View's parent
     * reference to the correct one.
     * @param subView reference to the view to insert.
     */
    void moveView(View& subView) {
        if (std::find_if(m_subViews.begin(), m_subViews.end(),
                         [&subView](auto const& pSubView) {
                             return &subView == pSubView.get();
                         }) != m_subViews.end()) {
            ESP_LOGD(TAG,
                "Calling appendSubView on %p with existing subView %p => "
                "exiting\n",
                this, &subView);
            return;
        }

        assert(subView.m_parentView);
        std::unique_ptr<View> reference =
            std::move(subView.m_parentView->detach(subView));
        reference->m_parentView = this;
        m_subViews.push_back(std::move(reference));
    }

    View& getSubViewAtIndex(byte idx) {
        if (idx >= m_subViews.size()) {
            ESP_LOGD(
                TAG,
                "Object %s at %p is calling the getSubViewAtIndex WITHOUT "
                "enough "
                "subViews. Number of subviews: %ld, requested subView: %ld\n",
                m_tag.c_str(), this, m_subViews.size(), idx);
        }
        assert(idx < m_subViews.size());
        assert(m_subViews[idx]);
        return *m_subViews[idx];
    }

    Coordinates getCoordinates() const { return m_frame.m_coordinates; }

    Size getSize() const { return m_frame.m_size; }

    virtual bool move(Coordinates const& coordinates) {
        m_frame.m_coordinates = coordinates;
        return true;
    }

    /**
     * Resizes this view.
     * @param newSize the new size for this view
     * @return true iff the operation succeeded
     */
    virtual bool resize(Size const& newSize) {
        m_frame.m_size = newSize;
        return true;
    }

    bool isVisible() { return m_isVisible; }

    Coordinates getCenter() const {
        auto coordinates = m_frame.m_coordinates;
        auto size = m_frame.m_size;
        return {coordinates.m_x + size.m_width / 2,
                coordinates.m_y + size.m_height / 2};
    }

    void moveRespectToTheCenter(Coordinates center) {
        m_frame.m_coordinates.m_x = center.m_x - m_frame.m_size.m_width / 2;

        m_frame.m_coordinates.m_y = center.m_y - m_frame.m_size.m_height / 2;
    }

    RectType getFrame() { return m_frame; }

    void makeVisible(bool isVisible) { m_isVisible = isVisible; }

    /**
     * Applies the given function recursively, visiting the tree of views
     * through depth-first search
     * @param f function to apply to each View
     */
    void applyRecursively(std::function<void(View&)> f);

    void onEvent(Press const& ev) override {
        if (m_parentView)
            m_parentView->onEvent(ev);
    }

    void onEvent(Click const& ev) override {
        if (m_parentView)
            m_parentView->onEvent(ev);
    }

    void onEvent(SwipeAntiClockwise const& ev) override {
        if (m_parentView)
            m_parentView->onEvent(ev);
    }

    void onEvent(SwipeClockwise const& ev) override {
        if (m_parentView)
            m_parentView->onEvent(ev);
    }

    void onEvent(ble::ConnectionState const&) override {}

    void onEvent(ble::BondingState const&) override {}

    void onEvent(ble::UpdateMessage const&) override {}

    void onEvent(ble::CallNotification const&) override {}

    void onEvent(ble::MessageNotification const&) override {}

    /**
     * Print the tree of views rooted in this.
     * The subtrees are printed starting from the left, from top to bottom.
     * For example, if the tree is
     *            this
     *      view1       view2
     *  view3
     *
     * the tree will be printed as
     *      this
     *          view1
     *              view3
     *          view2
     */
    void printTree() const;

    /**
     * checks whether the view is part of the tree rooted in this
     * @param view to look up in the tree
     * @return true iff view is part of the tree rooted in this
     */
    bool isAttached(View const& view) const {
        if (this == &view)
            return true;
        for (byte i = 0; i < m_subViews.size(); i++) {
            bool isAttached = m_subViews[i]->isAttached(view);
            if (isAttached)
                return true;
        }
        return false;
    }

protected:
    virtual void drawOnScreen() = 0;

    size_t getNumSubViews() { return m_subViews.size(); }

    /**
     * Adds the view as the rightmost node to the tree rooted in this View.
     * This function assumes the view has not already in the tree, if it was the
     * case use the function 'moveView' instead. Notice that the View's
     * constructor automatically adds the View node to its parent.
     * @param subView to be inserted.
     */
    void appendSubView(std::unique_ptr<View> subView) {
        assert(!subView->m_parentView);
        subView->m_parentView = this;
        m_subViews.push_back(std::move(subView));
    }

    /**
     * Detaches the provided view from the tree rooted in this View
     */
    std::unique_ptr<View> detach(View const& view) {
        ESP_LOGD(TAG, "Detaching view at %p\n", &view);
        std::unique_ptr<View> reference;
        int16_t idx = -1;
        for (byte i = 0; i < m_subViews.size() && idx == -1; i++) {
            if (m_subViews[i].get() == &view) {
                reference = std::move(m_subViews[i]);
                idx = i;
            }
        }

        ESP_LOGD(TAG, "The view to detach is at %d\n", idx);
        assert(idx != -1);
        m_subViews.erase(m_subViews.begin() + idx);
        return reference;
    }

    /**
     * Detaches all subViews rooted in this View
     */
    void detachAll() { m_subViews.clear(); }

private:
    inline static char const TAG[] = "View";

private:
    View* m_parentView;

    std::vector<std::unique_ptr<View>> m_subViews;

    // coordinates are in absolute value
    RectType m_frame;

    std::string m_tag;

    bool m_isVisible;
};

}  // namespace view