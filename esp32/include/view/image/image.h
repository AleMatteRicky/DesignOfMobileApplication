#pragma once

#include <mutex>
#include <vector>

#include <PNGdec.h>
#include <TFT_eSPI.h>
#include "view/image/bin_image_info.h"
#include "view/screen/screen.h"
#include "view/view.h"

namespace view {

class Image : public View {
public:
    Image(RectType frame,
          View* superiorView,
          std::vector<BinaryImageInfo> binImages,
          std::string name = "")
        : View::View(frame, superiorView, "Image"), m_binImages(binImages) {}

    Image(RectType frame,
          View* superiorView,
          std::vector<BinaryImageInfo> binImages,
          std::function<void(ble::UpdateMessage const&, int&)> onEvent)
        : View::View(frame, superiorView, "Image"), m_binImages(binImages), m_onEvent(onEvent) {}
    
    void draw() override;

    int pngDraw(PNGDRAW* pDraw);

    std::unique_ptr<BinaryImageInfo> toBinary();

    bool resize(Size const& newSize) override {
        for (byte i = 0; i < m_binImages.size(); i++) {
            if (m_binImages[i].m_width == newSize.m_width &&
                m_binImages[i].m_height == newSize.m_height) {
                m_idxCurImage = i;
                View::resize(newSize);
                return true;
            }
        }
        return false;
    }

    void onEvent(Click const&) { m_onClickCb(); }

    void setOnClick(std::function<void(void)> callback) {
        m_onClickCb = callback;
    }

protected:
    void setIdxCurImage(byte idx) { m_idxCurImage = idx; }

private:
    byte m_idxCurImage = 0;
    std::vector<BinaryImageInfo> m_binImages;
    std::string m_name;

    std::function<void(void)> m_onClickCb;

    std::function<void(ble::UpdateMessage const&, int&)> m_onEvent;
};
}  // namespace view