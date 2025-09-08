#pragma once

#include <atomic>
#include <unordered_map>
#include <variant>
#include "esp_log.h"
#include "utility/blocking_queue.h"
#include "view/main_event_queue.h"

template <typename T>
struct ObserverBase {
    virtual void onEvent(T const&) = 0;
    virtual ~ObserverBase() = default;
};

template <typename... Args>
struct Observer : ObserverBase<Args>... {
    using ObserverBase<Args>::onEvent...;  // bring all 'onEvent()' into scope
};

template <typename... Args>
using ObserverType = Observer<Args...>;

template <typename... Args>
using NotificationType = std::variant<Args...>;

/**
 * Generic class that implements the observer design pattern to allow objects to
 * communicate without having an explicit reference (dependency) between them.
 */
template <typename... Args>
class NotificationManager {
public:
    /**
     * Adds an observer for the specified event if not already added
     * @param eventName name of the event for which the observer listens to
     * @param observer to add
     */
    virtual void addObserver(char const* eventName,
                             Observer<Args...>* observer) = 0;

    /**
     * Notifies all observers (if any) for the specified event
     * @param eventName name of the event for which a new notification is
     * available
     * @param notification notification to be notified for all active observers
     */
    virtual void notify(char const* eventName,
                        NotificationType<Args...> notification) = 0;

    /**
     * Removes the observer for the provided event if it is registered for it
     * @param eventName name of the event
     * @param observer observer to remove
     */
    virtual void remove(char const* eventName,
                        Observer<Args...> const& observer) = 0;

    /**
     * Removes the observer from all events registered in this
     * NotificationManager
     * @param observer to remove
     */
    virtual void removeFromAllEvents(Observer<Args...> const& observer) = 0;

    /**
     * Returns true iff the observer has been registered for the event
     * @param eventName name of the event
     * @param observer observer to check for registration
     * @return true iff the specified observer is registered for the event
     */
    virtual bool isValidObserver(char const* eventName,
                                 Observer<Args...> const& observer) = 0;
};

/**
 * Singleton template class implementing the NotificationManager interface.
 * Note that this class is not thread safe. For a thread safe alternative look
 * at the class DistributedNotificationManager
 */
template <typename... Args>
class NotificationManagerImpl : public NotificationManager<Args...> {
public:
    void addObserver(char const* eventName,
                     Observer<Args...>* observer) override {
        ESP_LOGD(TAG, "Observer at '%s' has been added for the event %s\n",
                 observer->getName(), eventName);

        m_mapEventToObservers[eventName].push_back(observer);
    }

    void remove(char const* eventName,
                Observer<Args...> const& observer) override {
        if (m_mapEventToObservers.find(eventName) ==
            m_mapEventToObservers.end())
            return;

        ESP_LOGD(
            TAG,
            "Function 'remove' has been called for the observer at %p and the "
            "event '%s'\n",
            &observer, eventName);

        auto& observers = m_mapEventToObservers[eventName];

        observers.erase(
            std::remove_if(observers.begin(), observers.end(),
                           [&eventName, &observer](auto const& o) {
                               bool found = o == &observer;
                               if (found) {
                                   ESP_LOGD(
                                       TAG,
                                       "observer at %p has been successfully "
                                       "removed for the event %s\n",
                                       &observer, eventName);
                               }
                                           return o == &observer;
                                       }),

                        observers.end());
    }

    void removeFromAllEvents(ObserverType<Args...> const& observer) override {
        std::vector<std::string> allEvents;
        allEvents.reserve(m_mapEventToObservers.size());

        for (auto el : m_mapEventToObservers) {
            allEvents.push_back(el.first);
        }

        for (auto const& en : allEvents) {
            remove(en, observer);
        }
    }

    bool isValidObserver(char const* eventName,
                         Observer<Args...> const& observer) override {
        auto const& observers = m_mapEventToObservers[eventName];

        auto const& it =
            std::find_if(observers.begin(), observers.end(),
                         [&observer](auto const& o) { return o == &observer; });

        return it != observers.end();
    }

    void notify(char const* eventName,
                NotificationType<Args...> notification) override {
        auto const& observersToNotify = m_mapEventToObservers[eventName];

        for (auto const& o : observersToNotify) {
            std::visit([&o](auto&& n) { o->onEvent(n); }, notification);
        }
    }

private:
    inline static char const TAG[] = "NotificationManagerImpl";

private:
    std::unordered_map<std::string, std::vector<Observer<Args...>*>>
        m_mapEventToObservers;
};

/**
 * Thread safe implementation of the NotificationManager.
 */
template <typename... Args>
class DistributedNotificationManager : public NotificationManager<Args...> {
public:
    void addObserver(char const* eventName,
                     Observer<Args...>* const observer) override {
        std::lock_guard<std::recursive_mutex> lock(mutex);
        m_notificationManger.addObserver(eventName, observer);
    }

    void remove(char const* eventName,
                Observer<Args...> const& observer) override {
        std::lock_guard<std::recursive_mutex> lock(mutex);
        m_notificationManger.remove(eventName, observer);
    }

    void removeFromAllEvents(Observer<Args...> const& observer) override {
        std::lock_guard<std::recursive_mutex> lock(mutex);
        m_notificationManger.removeFromAllEvents(observer);
    }

    bool isValidObserver(char const* eventName,
                         Observer<Args...> const& observer) override {
        std::lock_guard<std::recursive_mutex> lock(mutex);
        return m_notificationManger.isValidObserver(eventName, observer);
    }

    void notify(char const* eventName,
                NotificationType<Args...> notification) override {
        auto mainQueue = view::MainEventQueue::getInstance();
        mainQueue->push(std::make_unique<view::RemoteProcedure>(
            [this, eventName, notification] {
                std::lock_guard<std::recursive_mutex> lock(mutex);
                m_notificationManger.notify(eventName, notification);
            }));
    }

    static DistributedNotificationManager<Args...>* getInstance() {
        if (instance == nullptr) {
            std::lock_guard<std::recursive_mutex> lock(mutex);
            // check again, otherwise a thread could overwrite the work done by
            // the previous thread
            if (instance == nullptr) {
                instance = new DistributedNotificationManager<Args...>();
            }
        }
        return instance;
    }

private:
    DistributedNotificationManager<Args...>() {}
    DistributedNotificationManager<Args...>(
        DistributedNotificationManager<Args...> const&) {}

private:
    static inline std::atomic<DistributedNotificationManager<Args...>*>
        instance{nullptr};

    static inline std::recursive_mutex mutex;

    static inline char const TAG[] = "DistributedNotificationManager";

private:
    NotificationManagerImpl<Args...> m_notificationManger;
};