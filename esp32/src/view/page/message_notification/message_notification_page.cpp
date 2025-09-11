#include "view/page/message_notification/message_notification_page.h"
#include "ble/remote_dispatcher.h"
#include "controller/central_controller.h"

namespace view {
std::unique_ptr<MessageNotificationPage>
MessageNotificationPage::Factory::create() {
    std::unique_ptr<MessageNotificationPage> messagesPage =
        std::unique_ptr<MessageNotificationPage>(new MessageNotificationPage());

    auto remoteDispatcher = ble::RemoteDispatcher::getInstance();

    remoteDispatcher->addObserver(ble::MessageNotification::name,
                                  messagesPage.get());

    auto inputManager = InputManager::getInstance();

    inputManager->addObserver(Click::name, messagesPage.get());
    inputManager->addObserver(SwipeClockwise::name, messagesPage.get());
    inputManager->addObserver(SwipeAntiClockwise::name, messagesPage.get());

    return messagesPage;
}

MessageNotificationPage::MessageNotificationPage()
    : Page::Page(RectType{Coordinates{0, 0}, Size{SCREEN_WIDTH, SCREEN_HEIGHT}},
                 nullptr),
      m_messagesSz{0},
      m_idxFocusedMessage{0},
      m_isFocused{false},
      m_title{new TextArea(RectType{Coordinates{0, 0}, Size{200, 20}}, this)} {
    updateScreenWithNewMessages();
}

void MessageNotificationPage::updateScreenWithNewMessages() {
    auto controller = controller::CentralController::getInstance();

    std::vector<std::string> newMessages =
        controller->getMessages(maxNumMessages);

    ESP_LOGD(TAG, "There are %u new messages: ", newMessages.size());

    setUpTitle(newMessages);

    setUpMessages(newMessages);
}

void MessageNotificationPage::setUpTitle(
    std::vector<std::string> const& newMessages) {
    std::string title;

    byte numMessages = std::min(
        static_cast<byte>(m_messagesSz + newMessages.size()), maxNumMessages);

    if (numMessages == 0) {
        title = "No messages yet";
    } else {
        title = "Message " + std::to_string(m_idxFocusedMessage + 1) + " / " +
                std::to_string(numMessages);
    }

    m_title->clearFromScreen();

    m_title->setContent(title);

    auto [titleX, titleY] = m_title->getCoordinates();
    auto [w, h] = m_title->sizeContent();

    ESP_LOGD(TAG, "Title at (%d, %d), size is (%d, %d)", titleX, titleY, w, h);

    int16_t horizontalSpaceFromTheBorder = (SCREEN_WIDTH - w) / 2;
    m_title->move(Coordinates{horizontalSpaceFromTheBorder, 40});

    m_title->resize(Size{SCREEN_WIDTH - horizontalSpaceFromTheBorder, h});
    m_title->makeVisible(true);
}

void MessageNotificationPage::removeFocusFromMessage(
    std::unique_ptr<ScrollableText> const& pMsg) {
    auto [titleX, titleY] = m_title->getCoordinates();
    auto [titleWidth, titleHeight] = m_title->getSize();
    int16_t msgY = titleY + titleHeight + 20;
    pMsg->move(Coordinates{0, msgY});
    pMsg->resize(Size{SCREEN_WIDTH, SCREEN_HEIGHT - msgY});
    ESP_LOGD(TAG, "message at (%lu, %lu) with size w=%u, h=%u",
             pMsg->getCoordinates().m_x, pMsg->getCoordinates().m_y,
             pMsg->getSize().m_width, pMsg->getSize().m_height);
    pMsg->showArrows(false);
}

std::unique_ptr<ScrollableText> MessageNotificationPage::allocateNewMessage() {
    ESP_LOGD(TAG, "allocating a new message");
    auto [titleX, titleY] = m_title->getCoordinates();
    int16_t msgY = titleY + m_title->getSize().m_height + 20;
    auto pMsg = std::make_unique<ScrollableText>(
        RectType{Coordinates{0, 0}, Size{0, 0}}, nullptr, "");
    pMsg->wrapTextVertically(false);
    removeFocusFromMessage(pMsg);
    return pMsg;
}

void MessageNotificationPage::setUpMessages(
    std::vector<std::string> const& newMessages) {
    byte numNewMessages = newMessages.size();
    // no new message, nothing to change
    if (numNewMessages == 0)
        return;

    assert(numNewMessages <= maxNumMessages);

    // shift by numNewMessages to have space for the new messages
    for (int16_t i = m_messagesSz - 1; i >= 0; i--) {
        if (i == m_idxFocusedMessage)
            m_messages[i]->clearFromScreen();
        if (i + numNewMessages < m_messages.size()) {
            ESP_LOGD(TAG, "Shifing the %u message", i + numNewMessages);
            std::swap(m_messages[i], m_messages[i + numNewMessages]);
        }
    }

    for (byte i = 0; i < numNewMessages; i++) {
        auto& pMsg = m_messages[i];
        if (!pMsg)
            pMsg = allocateNewMessage();

        ESP_LOGD(TAG, "Set message content to the new incoming one: '%s'",
                 newMessages[i].c_str());
        pMsg->setContent(extractContent(newMessages[i]));
    }

    // set back to the most recent message
    m_idxFocusedMessage = 0;
    // update the messages'size
    m_messagesSz = std::min(maxNumMessages,
                            static_cast<byte>(numNewMessages + m_messagesSz));
}

void MessageNotificationPage::setMessageFullScreen(
    std::unique_ptr<ScrollableText> const& pMsg) {
    ESP_LOGD(TAG, "Setting the message full screen");
    // hides the title
    m_title->clearFromScreen();
    m_title->makeVisible(false);

    pMsg->clearFromScreen();

    auto titleY = m_title->getCoordinates().m_y;
    pMsg->move(Coordinates{0, titleY});
    pMsg->resize(Size{SCREEN_WIDTH, SCREEN_HEIGHT - titleY});
    pMsg->showArrows(true);
}

/**
 * The first time is clicked, the message takes the entire frame, the second
 * time the page with all messages returns back visible.
 */
void MessageNotificationPage::onEvent(Click const&) {
    auto& pMsg = m_messages[m_idxFocusedMessage];

    if (!pMsg) {
        ESP_LOGD(TAG, "No messages, click has no effectl");
        return;
    }

    if (m_isFocused) {
        ESP_LOGD(TAG, "Focusing changing from true -> false");
        pMsg->clearFromScreen();
        removeFocusFromMessage(pMsg);
        updateScreenWithNewMessages();
    } else {
        ESP_LOGD(TAG, "Focusing changing from false -> true");
        setMessageFullScreen(pMsg);
    }

    m_isFocused = !m_isFocused;
    draw();
}

void MessageNotificationPage::onEvent(SwipeClockwise const& event) {
    if (m_isFocused) {
        ESP_LOGD(TAG, "Message is focused, deliver to it the clockwise swipe");
        m_messages[m_idxFocusedMessage]->onEvent(event);
        return;
    }

    if (m_idxFocusedMessage + 1 < m_messagesSz) {
        ESP_LOGD(TAG, "Message is not focused, swiping");
        m_messages[m_idxFocusedMessage]->clearFromScreen();
        m_idxFocusedMessage += 1;
        setUpTitle(std::vector<std::string>());
        draw();
    }
}

void MessageNotificationPage::onEvent(SwipeAntiClockwise const& event) {
    if (m_isFocused) {
        m_messages[m_idxFocusedMessage]->onEvent(event);
        return;
    }

    if (m_idxFocusedMessage - 1 >= 0) {
        // clear previous message
        m_messages[m_idxFocusedMessage]->clearFromScreen();
        m_idxFocusedMessage -= 1;
        setUpTitle(std::vector<std::string>());
        draw();
    }
}

void MessageNotificationPage::onEvent(ble::MessageNotification const& event) {
    if (m_isFocused) {
        ESP_LOGD(TAG, "A message is focused, skip the incoming event");
        return;
    }
    ESP_LOGD(TAG,
             "No message is focused, update the representation using the new "
             "messages");
    updateScreenWithNewMessages();
    draw();
}

void MessageNotificationPage::drawOnScreen() {
    m_title->draw();
    auto const& pMsg = m_messages[m_idxFocusedMessage];
    if (pMsg) {
        ESP_LOGD(TAG, "Printint the message");
        ESP_LOGD(TAG, "The message is empty? %d", pMsg->isEmpty());
        pMsg->draw();
    }
}

}  // namespace view
