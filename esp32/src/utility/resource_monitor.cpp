#include "utility/resource_monitor.h"
#include <Arduino.h>

void ResourceMonitor::printRemainingStackSize() {
    unsigned int remaining_stack = uxTaskGetStackHighWaterMark(NULL);
    ESP_LOGD(TAG, "Remaining stack size: %u bytes\n", remaining_stack);
}

void ResourceMonitor::printRemainingHeapSizeInfo() {
    ESP_LOGD(TAG, "#####");
    ESP_LOGD(TAG, "Heap info");
    ESP_LOGD(TAG, "Sum of all available blocks in the heap: %u bytes\n",
             esp_get_free_heap_size());
    ESP_LOGD(TAG, "Largest block that can be allocated: %u bytes\n",
             heap_caps_get_largest_free_block(MALLOC_CAP_DEFAULT));
    ESP_LOGD(TAG, "#####");
}