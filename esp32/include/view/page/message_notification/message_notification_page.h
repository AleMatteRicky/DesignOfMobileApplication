#pragma once

#include "ArduinoJson.h"
#include "view/page/page.h"
#include "view/text/scrollable_text.h"

namespace view {
class MessageNotificationPage : public Page {
public:
    class Factory {
    public:
        static std::unique_ptr<MessageNotificationPage> create();
    };

    void onEvent(Click const&) override;

    void onEvent(SwipeClockwise const&) override;

    void onEvent(SwipeAntiClockwise const&) override;

    void onEvent(ble::MessageNotification const&) override;

    PageType getType() override { return MESSAGES; }

protected:
    void drawOnScreen() override;

private:
    void updateScreenWithNewMessages();

    void setUpTitle(std::vector<std::string> const&);

    void setUpMessages(std::vector<std::string> const&);

    void setMessageFullScreen(std::unique_ptr<ScrollableText> const& pMsg);

    std::unique_ptr<ScrollableText> allocateNewMessage();

    void removeFocusFromMesage(std::unique_ptr<ScrollableText> const& pMsg);

    std::string extractContent(std::string const& msg) {
        JsonDocument doc;
        DeserializationError error = deserializeJson(doc, msg);
        if (error) {
            ESP_LOGD(TAG, "Error '%s' when deserializing '%s'\n", error.c_str(),
                     msg.c_str());
            return "";
        }

        std::string source = doc["source"];
        std::string sender = doc["sender"];
        std::string content = doc["content"];

        byte indendLevel = 5;
        std::string spaces(indendLevel, ' ');
        // return spaces + source + " from '" + sender + "' : " + content;
        //  alternative: "source: " + source + "\nsender: '" + sender +
        //  "'\ncontent : " + content;
        return "source: " + source + "\nsender: '" + sender +
               "'\ncontent : " + content;
    }

private:
    inline static char const TAG[] = "MessageNotificationPage";

    static constexpr byte maxNumMessages = 5;

private:
    MessageNotificationPage();
    std::array<std::unique_ptr<ScrollableText>, maxNumMessages> m_messages;
    byte m_messagesSz;
    byte m_idxFocusedMessage;
    bool m_isFocused;
    TextArea* m_title;
};

};  // namespace view