#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <freertos/queue.h> // thread-safe queue used to dispatch updates to the main thread
#include <atomic>
/*
 * General info + examples at https://docs.espressif.com/projects/arduino-esp32/en/latest/api/ble.html
 * Complete list of examples: https://github.com/espressif/arduino-esp32/tree/master/libraries/BLE/examples
 */

#define SERVICE_UUID "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
// characteristic writable by the client (Android app): App -> esp32
#define CHARACTERISTIC_UUID_RX "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
// characteristic writable by the server/pheriperal (esp32): esp32 -> App
#define CHARACTERISTIC_UUID_TX "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

#define QUEUE_SZ 10

BLEServer* pServer = NULL;
BLECharacteristic* pRxCharacteristic = NULL;
BLECharacteristic* pTxCharacteristic = NULL;

std::atomic<bool> deviceConnected(false);
QueueHandle_t msgQueue = xQueueCreate(QUEUE_SZ, sizeof(const String));

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      Serial.println("server connected");
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      Serial.println("server disconnected");
      deviceConnected = false;
    }
};

class MyCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    String rxValue = pCharacteristic->getValue();
    if (xQueueSend(msgQueue, &rxValue, portMAX_DELAY) != pdPASS) {
      Serial.println("QUEUE is full!!!!");
    }
  }
};


void setup() {
  Serial.begin(9600);
  
  Serial.println("Preparing the BLE Server");
  // Create the BLE Device
  BLEDevice::init("FM Server");

  // Create the BLE Server
  BLEServer* pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks);
  
  // Create the NUS BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);
  
  // Create the BLE Characteristics (all properties added)
  pTxCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID_TX,
          BLECharacteristic::PROPERTY_INDICATE
  );
  pTxCharacteristic->addDescriptor(new BLE2902());
  
  pRxCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID_RX,
    BLECharacteristic::PROPERTY_WRITE
  );

  pRxCharacteristic->setCallbacks(new MyCallbacks());

  // Start the service
  pService->start();

  advertise(pServer);
}

int i = 0;

void loop() {
  if (deviceConnected) {
    // read incoming messages
    // TODO: generalize to a specific protocol
    /*
    if (uxQueueMessagesWaiting(msgQueue) != 0) {
      handleMsg(msgQueue);
    } else {
      Serial.println("Sending msg: " + msg);      
      String msg = "Counter=" + String(i);

      pTxCharacteristic->setValue(msg);
      pTxCharacteristic->indicate();
      i+=1;
    }
    */
    if (uxQueueMessagesWaiting(msgQueue) != 0) {
      handleMsg(msgQueue);
    }
  }

  if (!deviceConnected) {
    Serial.println("Device not connected: advertising");
    advertise(pServer);
  }

  int d=0;
  if (deviceConnected) {
    d=500;
  } else {
    d=2000;
  }
  delay(d);
}

void handleMsg(QueueHandle_t msgQueue) {
  String msg;
  xQueueReceive(msgQueue, &msg, portMAX_DELAY);
  // TODO: define a protocol to handle incoming messages
  String incoming_call_message = "incoming_call";
  String disconnect_message = "disconnect";
  if (msg.length() > 0) {
    Serial.println("*********");
    Serial.print("Received Value: ");
    if (msg==incoming_call_message) {
      Serial.println("INCOMING CALL!!!!");
    } else if(msg==disconnect_message) {
      Serial.println("Closing connection");
      deviceConnected=false;
    } else {
      int sz = msg.length();
      for (int i = 0; i < sz; i++) {
          Serial.print(msg[i]);
      }
      Serial.print("\n");
    }
    Serial.println("*********");
  }
}

void advertise(BLEServer* pServer) {
  BLEAdvertising* pAdvertising = pServer->getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  pAdvertising->start();
}
