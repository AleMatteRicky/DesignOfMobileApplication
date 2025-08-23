#pragma once

#include <string>

namespace ble {

std::string const service_uuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
// UUID of the characteristic writable by the client (Android app) used to
// receive updates coming from the application: App -> esp32
std::string const characteristic_uuid_rx =
    "6e400002-b5a3-f393-e0a9-e50e24dcca9e";

// UUID characteristic writable by the server/pheriperal (esp32) used to send
// updates to the App: esp32 -> App
std::string const characteristic_uuid_tx =
    "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
}