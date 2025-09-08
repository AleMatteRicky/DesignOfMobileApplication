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
          std::vector<BinaryImageInfo> const& binImages,
          bool isVisible = true)
        : View::View(frame, superiorView, "Image", isVisible),
          m_binImages(binImages) {}

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

    void onEvent(Click const&) override { m_onClickCb(); }

    void onEvent(ble::ConnectionState const& event) override {
        m_onConnectionState(event);
    }

    void setOnClick(std::function<void(void)> const& callback) {
        m_onClickCb = callback;
    }

    void setOnUpdateMessage(std::function<void(ble::UpdateMessage const&,
                                               int&)> const& onUpdateMessage) {
        m_onUpdateMessage = onUpdateMessage;
    }

    void setOnConnectionState(
        std::function<void(ble::ConnectionState)> callback) {
        m_onConnectionState = callback;
    }

protected:
    void setIdxCurImage(byte idx) { m_idxCurImage = idx; }

    void drawOnScreen() override;

private:
    inline static char const TAG[] = "Image";

private:
    byte m_idxCurImage = 0;
    std::vector<BinaryImageInfo> m_binImages;
    std::string m_name;

    std::function<void(void)> m_onClickCb = []() {};
    std::function<void(ble::ConnectionState)> m_onConnectionState =
        [](ble::ConnectionState) {};
    std::function<void(ble::UpdateMessage const&, int&)> m_onUpdateMessage =
        [](ble::UpdateMessage const&, int&) {};
};
}  // namespace view