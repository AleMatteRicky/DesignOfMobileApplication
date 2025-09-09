#pragma once

class ResourceMonitor {
public:
    static void printRemainingStackSize();
    static void printRemainingHeapSizeInfo();

private:
    inline static char const TAG[] = "ResourceMonitor";
};