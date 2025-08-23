#pragma once

#include "ble/events/remote_events.h"
#include "view/image/image.h"

namespace view {
class ConnectionStateImage : public Image {
    using CallbackType = std::function<void(void)>;

public:
    ConnectionStateImage(RectType frame,
                         View* superior,
                         BinaryImageInfo connectionImg,
                         BinaryImageInfo disconnectionImg)
        : Image::Image(
              frame,
              superior,
              std::vector<BinaryImageInfo>{connectionImg, disconnectionImg}) {}

    void onEvent(ble::ConnectionState const& connectionState) override {
        m_isConnected = connectionState.CONNECTED ? true : false;
        setIdxCurImage(m_isConnected);
    }

private:
    bool m_isConnected = false;
};

}  // namespace view