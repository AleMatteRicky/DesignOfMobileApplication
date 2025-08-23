#pragma once

#include <chrono>
#include <functional>
#include <set>
#include <string>
#include <thread>
#include <unordered_map>
#include "view/main_event_queue.h"

namespace view {

class CallDelayer {
private:
    class DelayedTask {
    public:
        DelayedTask() : m_id(globalId++) {}

        ~DelayedTask() {
            if (m_thread.joinable())
                m_thread.join();
        }

        void start(std::function<void()>&& f) {
            if (m_thread.joinable())
                m_thread.join();
            m_thread = std::thread(f);
        }

        int getId() { return m_id; }

        DelayedTask& operator=(DelayedTask&& other) {
            if (m_thread.joinable())
                m_thread.join();
            m_id = other.m_id;
            m_thread = std::move(other.m_thread);
            return *this;
        }

    private:
        static inline int globalId = 0;
        int m_id;
        std::thread m_thread;
    };

public:
    static void delay(int milliseconds, std::function<void()> callback) {
        std::unique_lock<std::mutex> lock(mtx);
        DelayedTask task;
        int taskId = task.getId();
        instance->m_mapIdTotask[taskId] = std::move(task);

        instance->m_mapIdTotask[taskId].start(
            [milliseconds, cb = std::move(callback), id = taskId]() {
                std::this_thread::sleep_for(
                    std::chrono::milliseconds(milliseconds));

                auto mainQueue = MainEventQueue::getInstance();
                mainQueue->push(std::make_unique<RemoteProcedure>(cb));

                remove(id);
            });
    }

    static CallDelayer* getInstance() {
        if (instance)
            return instance;
        instance = new CallDelayer();
        return instance;
    }

    static void remove(int id) {
        std::unique_lock<std::mutex> lock(mtx);
        instance->m_mapIdTotask.erase(id);
    }

private:
    CallDelayer();
    CallDelayer(CallDelayer const&);

private:
    std::unordered_map<int, DelayedTask> m_mapIdTotask;
    static inline CallDelayer* instance = nullptr;
    static inline std::mutex mtx;
};

}  // namespace view