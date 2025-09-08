#pragma once

#include <chrono>
#include <functional>
#include <set>
#include <string>
#include <thread>
#include <unordered_map>
#include "notifications/notification_manager.h"
#include "view/main_event_queue.h"

namespace view {

struct Timeout {
    static inline std::string const name = "Timeout";
};

using TimerDispatcher = DistributedNotificationManager<Timeout>;

class DelayedFunction : public Observer<Timeout> {
public:
    DelayedFunction(std::function<void(void)> function)
        : m_function(function) {}
    void onEvent(Timeout const& t) { m_function(); }

    char const* getName() override { return "DelayedFunction"; }

private:
    std::function<void(void)> m_function;
};

class Timer {
public:
    Timer()
        : m_name(Timeout::name + std::to_string(id++)),
          m_delayFunction(nullptr),
          m_isValid{true} {}

    ~Timer() { cancel(); }

    void delay(int timeMs, std::function<void(void)> function) {
        // cancel any existing thread
        cancel();

        auto timerDispatcher = TimerDispatcher::getInstance();
        m_isValid = true;

        m_delayFunction = std::make_unique<DelayedFunction>(function);
        timerDispatcher->addObserver(m_name.c_str(), m_delayFunction.get());

        m_thread =
            std::thread([name = m_name, timeMs, timerDispatcher, &mtx = m_mtx,
                         &cv = m_cv, &isValid = m_isValid]() {
                std::unique_lock<std::mutex> lock(mtx);
                // awake before the timeMs only if the thread needs to be
                // killed
                cv.wait_for(lock, std::chrono::milliseconds(timeMs),
                            [&isValid] { return !isValid; });
                if (isValid)
                    timerDispatcher->notify(name.c_str(), Timeout());
            });
    }

    void cancel() {
        // create a new scope to lock the mutex only for setting the flag to
        // false
        {
            std::unique_lock<std::mutex> lock(m_mtx);
            m_isValid = false;
        }
        m_cv.notify_one();
        auto timerDispatcher = TimerDispatcher::getInstance();
        timerDispatcher->remove(m_name.c_str(), *m_delayFunction);
        m_delayFunction = nullptr;

        // wait for the signal to arrive and the thread to complete
        if (m_thread.joinable())
            m_thread.join();
    }

private:
    static constexpr byte maxLenStr = 32;

private:
    std::unique_ptr<DelayedFunction> m_delayFunction;
    std::thread m_thread;
    std::string const m_name;
    bool m_isValid;

    std::mutex m_mtx;
    std::condition_variable m_cv;

private:
    // id to identify each timer otherwise the first one to expire
    // would notify all the others that are yet to expire
    static inline int id = 0;
};
}  // namespace view