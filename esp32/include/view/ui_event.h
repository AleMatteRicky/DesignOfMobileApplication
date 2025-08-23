#pragma once

#include <functional>
#include <string>

namespace view {

enum class UIEventTag { RemoteProcedure };

/**
 * Abstract class representing events related to the UI
 */
class UIEvent {
public:
    UIEvent(UIEventTag tag) : m_tag(tag) {}

    virtual ~UIEvent() {}

    UIEventTag getTag() { return m_tag; }

private:
    UIEventTag m_tag;
};

class RemoteProcedure : public UIEvent {
public:
    RemoteProcedure(std::function<void()> callback)
        : UIEvent::UIEvent(UIEventTag::RemoteProcedure), m_callback(callback) {}

    void call() { m_callback(); }

private:
    std::function<void()> m_callback;
};
}