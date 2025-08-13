#include "BLEDevice.h"
#include "BLEServer.h"
#include "BLEUtils.h"
#include "BLE2902.h"
#include "BLESecurity.h"
#include <esp_gap_ble_api.h>
#include <esp_gatts_api.h>
#include <esp_bt_defs.h>

// UUIDs for our service and characteristics
#define SERVICE_UUID        "12345678-1234-1234-1234-123456789abc"
#define CHARACTERISTIC_UUID_RX "12345678-1234-1234-1234-123456789abd"
#define CHARACTERISTIC_UUID_TX "12345678-1234-1234-1234-123456789abe"

// BLE objects
BLEServer* pServer = NULL;
BLECharacteristic* pTxCharacteristic = NULL;
BLECharacteristic* pRxCharacteristic = NULL;

// Device name
String device_name = "ESP32-BLE-PasskeyDemo";

// Connection and pairing status
bool device_connected = false;
bool old_device_connected = false;
bool pairing_in_progress = false;
String received_message = "";

// Security callback class
class MySecurity : public BLESecurityCallbacks {
private:
  bool new_pairing_started = false;
  
public:
  // Called when passkey needs to be displayed
  uint32_t onPassKeyRequest() {
    Serial.println("PassKeyRequest (shouldn't happen in display mode)");
    return 123456;
  }

  // Called to display passkey
  void onPassKeyNotify(uint32_t pass_key) {
    new_pairing_started = true;
    pairing_in_progress = true;
    Serial.println("========================================");
    Serial.println("🔐 NEW BLE PAIRING IN PROGRESS");
    Serial.println("========================================");
    Serial.printf("PASSKEY: %06lu\n", pass_key);
    Serial.println("========================================");
    Serial.println("Enter this 6-digit passkey on your smartphone:");
    Serial.printf(">>> %06lu <<<\n", pass_key);
    Serial.println("========================================");
    Serial.println("Waiting for passkey confirmation...");
  }

  // Called for numeric comparison (not used in our setup)
  bool onConfirmPIN(uint32_t pin) {
    Serial.println("Confirm PIN: " + String(pin));
    return true;
  }

  // Called when security request is made
  bool onSecurityRequest() {
    Serial.println("Security request received - checking bonding status");
    return true;
  }

  // Called when authentication is complete
  void onAuthenticationComplete(esp_ble_auth_cmpl_t cmpl) {
    pairing_in_progress = false;
    
    if (cmpl.success) {
      if (new_pairing_started) {
        // This was a new pairing process
        Serial.println("========================================");
        Serial.println("✓ NEW BLE PAIRING SUCCESSFUL!");
        Serial.println("Device is now bonded and connected");
        Serial.println("Bonding keys have been stored");
        Serial.println("========================================");
        new_pairing_started = false;
      } else {
        // This was reconnection with existing bonding
        Serial.println("========================================");
        Serial.println("✓ BONDED DEVICE RECONNECTED");
        Serial.println("Using existing bonding keys");
        Serial.println("Encrypted connection established");
        Serial.println("========================================");
      }
      Serial.println("You can now send/receive messages");
    } else {
      Serial.println("========================================");
      if (new_pairing_started) {
        Serial.println("✗ NEW BLE PAIRING FAILED!");
        new_pairing_started = false;
      } else {
        Serial.println("✗ BONDING VERIFICATION FAILED!");
        Serial.println("You may need to unpair and pair again");
      }
      Serial.printf("Failure reason: %d\n", cmpl.fail_reason);
      Serial.println("========================================");
    }
  }
};

// Server callback class
class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    device_connected = true;
    Serial.println("Device connecting...");
  }

  void onDisconnect(BLEServer* pServer) {
    device_connected = false;
    pairing_in_progress = false;
    Serial.println("Device disconnected");
    // Restart advertising
    BLEDevice::startAdvertising();
    Serial.println("Ready for new connection...");
  }
};

// Characteristic callback class for receiving data
class MyCallbacks: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    String rxValue = pCharacteristic->getValue();
    
    if (rxValue.length() > 0) {
      received_message = "";
      for (int i = 0; i < rxValue.length(); i++) {
        received_message += rxValue[i];
      }
      Serial.println("Received: " + received_message);
      
      // Echo the message back
      String response = "ESP32 received: " + received_message;
      pTxCharacteristic->setValue(response.c_str());
      pTxCharacteristic->notify();
    }
  }
};

void setup() {
  Serial.begin(9600);
  Serial.println("ESP32 BLE Passkey Entry Demo Starting...");

  // Create the BLE Device
  BLEDevice::init(device_name.c_str());

  // Set security parameters BEFORE creating server
  BLEDevice::setEncryptionLevel(ESP_BLE_SEC_ENCRYPT_MITM);
  BLEDevice::setSecurityCallbacks(new MySecurity());
  
  // Set BLE security
  BLESecurity *pSecurity = new BLESecurity();
  pSecurity->setAuthenticationMode(ESP_LE_AUTH_REQ_SC_MITM_BOND);
  pSecurity->setCapability(ESP_IO_CAP_OUT); // Display only
  pSecurity->setInitEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);
  pSecurity->setRespEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create BLE Characteristics
  // TX Characteristic (ESP32 to smartphone)
  pTxCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_TX,
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  pTxCharacteristic->addDescriptor(new BLE2902());

  // RX Characteristic (smartphone to ESP32)
  pRxCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_RX,
                      BLECharacteristic::PROPERTY_WRITE
                    );
  pRxCharacteristic->setCallbacks(new MyCallbacks());

  // Start the service
  pService->start();

  // Configure advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);

  // Start advertising
  BLEDevice::startAdvertising();
  
  Serial.println("BLE initialized successfully");
  Serial.println("Device Name: " + device_name);
  Serial.println("Service UUID: " + String(SERVICE_UUID));
  Serial.println("Security: MITM + Bonding + Passkey Display");
  Serial.println("ESP32 is now advertising and ready for pairing");
  Serial.println("Look for '" + device_name + "' in your smartphone's Bluetooth settings");
  Serial.println("========================================");

  printDeviceInfo();
  listBondedDevices();
  clearBondingInfo();
}

void loop() {
  // Handle connection state changes
  if (device_connected != old_device_connected) {
    if (device_connected) {
      Serial.println("BLE device connected!");
    }
    old_device_connected = device_connected;
  }

  // Send data from Serial Monitor to BLE device
  if (device_connected && Serial.available()) {
    String message = Serial.readString();
    message.trim();
    
    if (message.length() > 0) {
      pTxCharacteristic->setValue(message.c_str());
      pTxCharacteristic->notify();
      Serial.println("Sent: " + message);
    }
  }

  // Handle disconnection
  if (!device_connected && old_device_connected) {
    delay(500); // Give the bluetooth stack time to get ready
    pServer->startAdvertising(); // Restart advertising
    Serial.println("Restarting advertising...");
    old_device_connected = device_connected;
  }

  delay(100);
}

// Function to get device MAC address
String getDeviceMAC() {
  uint8_t baseMac[6] = {0};
  //esp_read_mac(baseMac, ESP_MAC_BT);
  char macStr[18] = {0};
  sprintf(macStr, "%02X:%02X:%02X:%02X:%02X:%02X", 
          baseMac[0], baseMac[1], baseMac[2], 
          baseMac[3], baseMac[4], baseMac[5]);
  return String(macStr);
}

// Print device information
void printDeviceInfo() {
  Serial.println("========================================");
  Serial.println("ESP32 BLE Device Information:");
  Serial.println("Device Name: " + device_name);
  Serial.println("MAC Address: " + getDeviceMAC());
  Serial.println("Service UUID: " + String(SERVICE_UUID));
  Serial.println("TX Characteristic: " + String(CHARACTERISTIC_UUID_TX));
  Serial.println("RX Characteristic: " + String(CHARACTERISTIC_UUID_RX));
  Serial.println("IO Capability: Display Only");
  Serial.println("Authentication: MITM + Bonding + Secure Connections");
  Serial.println("Encryption: Required");
  Serial.println("========================================");
}

// Utility function to send a message via BLE
void sendBLEMessage(String message) {
  if (device_connected && pTxCharacteristic) {
    pTxCharacteristic->setValue(message.c_str());
    pTxCharacteristic->notify();
    Serial.println("Sent via BLE: " + message);
  } else {
    Serial.println("Cannot send - no device connected");
  }
}

// Function to clear all bonding information (useful for testing)
void clearBondingInfo() {
  Serial.println("Clearing all bonding information...");
  
  // Get the number of bonded devices
  int dev_num = esp_ble_get_bond_device_num();
  
  if (dev_num > 0) {
    esp_ble_bond_dev_t *dev_list = (esp_ble_bond_dev_t *)malloc(sizeof(esp_ble_bond_dev_t) * dev_num);
    esp_ble_get_bond_device_list(&dev_num, dev_list);
    
    // Remove each bonded device
    for (int i = 0; i < dev_num; i++) {
      esp_ble_remove_bond_device(dev_list[i].bd_addr);
      Serial.printf("Removed bonded device %d\n", i+1);
    }
    
    free(dev_list);
    Serial.println("All bonding information cleared");
  } else {
    Serial.println("No bonded devices found");
  }
  
  Serial.println("Ready for fresh pairing");
}

// Function to list bonded devices
void listBondedDevices() {
  int dev_num = esp_ble_get_bond_device_num();
  Serial.printf("Number of bonded devices: %d\n", dev_num);
  
  if (dev_num > 0) {
    esp_ble_bond_dev_t *dev_list = (esp_ble_bond_dev_t *)malloc(sizeof(esp_ble_bond_dev_t) * dev_num);
    esp_ble_get_bond_device_list(&dev_num, dev_list);
    
    for (int i = 0; i < dev_num; i++) {
      Serial.printf("Bonded device %d: %02X:%02X:%02X:%02X:%02X:%02X\n", 
        i+1,
        dev_list[i].bd_addr[0], dev_list[i].bd_addr[1], dev_list[i].bd_addr[2],
        dev_list[i].bd_addr[3], dev_list[i].bd_addr[4], dev_list[i].bd_addr[5]);
    }
    
    free(dev_list);
  }
}