#pragma once

#include <condition_variable>
#include <iostream>
#include <mutex>
#include <queue>

/**
 * Thread safe implementation of a Queue
 */
template <typename T>
class BlockingQueue {
public:
    /**
     * Pushes an element to the queue
     * @param item to push
     */
    void push(T const& item) {
        std::unique_lock<std::mutex> lock(mtx);

        queue.push(item);

        // notify one thread that was waiting for the queue to be filled with at
        // least one element
        cv.notify_one();
    }

    /**
     * Pushes an element to the queue
     * @param item to push
     */
    void push(T&& item) {
        std::unique_lock<std::mutex> lock(mtx);

        queue.push(std::move(item));

        // notify one thread that was waiting for the queue to be filled with at
        // least one element
        cv.notify_one();
    }

    /**
     * Removes an element off the queue and returns it, waiting if the queue is
     * empty
     * @return the first item in the queue
     */
    T remove() {
        std::unique_lock<std::mutex> lock(mtx);

        cv.wait(lock, [this]() { return !queue.empty(); });

        T item(std::move(queue.front()));
        queue.pop();

        return std::move(item);
    }

    bool isEmpty() {
        std::unique_lock<std::mutex> lock(mtx);
        return queue.empty();
    }

private:
    std::queue<T> queue;

    std::mutex mtx;

    std::condition_variable cv;
};