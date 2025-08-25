#include "utility/resource_monitor.h"
#include <Arduino.h>

void ResourceMonitor::printRemainingStackSize() {
    unsigned int remaining_stack = uxTaskGetStackHighWaterMark(NULL);
    Serial.printf("Remaining stack size: %u bytes\n", remaining_stack);
}
