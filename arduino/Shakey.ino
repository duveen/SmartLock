#include <RFduinoBLE.h>
#include "EEPROM.h"
#include "MPU9250.h"

// Sensor INDEX
#define SOLENOID 10

// EEPROM INDEX
#define PASS_INDEX 4
#define SENS_INDEX 5
#define INTERVAL_INDEX 6

// 민감도 값 설정
#define SENSITIVITY1 30
#define SENSITIVITY2 60
#define SENSITIVITY3 90
#define SENSITIVITY4 120
#define SENSITIVITY5 150

boolean is_connect = false;
boolean is_running = false;

int interval = 500;

short pass = 0; // 등록상태 확인
int sens = 3; // 민감도 값 설정
char* secret_key = "1512233015"; // 시크릿 키

int sensValue = 60;

MPU9250 mpu;

void setup() {
  Serial.begin(9600);
  Serial.println("Shakey On");

  if (pass == 0) {
    interval = 500;
  } else {
    interval = 3000;
  }

  // BLE Data Setup
  RFduinoBLE.deviceName = "Shakey(DE1A)";
  RFduinoBLE.advertisementData = "smu";
  RFduinoBLE.advertisementInterval = interval;

  // MPU Connecting TEST
  // Wire.begin();
  Wire.beginOnPins(4, 6);
  Serial.print("MPU9250 Connecting... \t\t");
  if (!mpu.isConnecting()) {
    Serial.println("Error");
    while (1);
  }
  Serial.println("OK");

  // MPU INIT
  Serial.print("MPU9250 Initializing... \t");
  mpu.MPU9250SelfTest(mpu.SelfTest);
  mpu.calibrateMPU9250(mpu.gyroBias, mpu.accelBias);
  mpu.initMPU9250();
  Serial.println("OK");

  // AK Connecting TEST
  Serial.print("AK8963 Connecting... \t\t");
  if (!mpu.isAKConnecting()) {
    Serial.println("Error");
    while (1);
  }
  Serial.println("OK");

  // AK INIT
  Serial.print("AK8963 Initializing... \t\t");
  mpu.initAK8963(mpu.magCalibration);
  Serial.println("OK");

  // MPU Sleep Mode Check
  Serial.print("MPU Sleeping... \t\t");
  mpu.setSleepMode(true);
  Serial.println(mpu.isSleepMode() ? "Sleep" : "WakeUp");

  // EEPROM PASS Value Read
  Serial.print("Register Check... \t\t");
  pass = EEPROM.read(PASS_INDEX);
  if (pass == 255) {
    EEPROM.write(PASS_INDEX, 0);
    pass = 0;
  }
  Serial.println(pass == 0 ? "No Register" : "Register");

  // EEPROM SENS Value Read
  Serial.print("Read Sens Value from EEPROM... \t");
  sens = EEPROM.read(SENS_INDEX);
  if (sens == 255) {
    EEPROM.write(SENS_INDEX, 3);
    sens = 3;
  }
  sensValue = getSens(sens);

  Serial.print("SENS: ");
  Serial.println(sens);

  // Solenoid PIN CHECK
  pinMode(SOLENOID, OUTPUT);

  // BLE START
  Serial.print("BLE Start & Waiting... \t\t");
  RFduinoBLE.begin();
  Serial.println("OK");
}

void loop() {
  RFduino_ULPDelay(100);

  if (is_connect && pass == 1) {
    int16_t accel[3];

    mpu.readGyroData(accel);

    double ax = (double) accel[0] * 250 / 32768;
    double ay = (double) accel[1] * 250 / 32768;
    double az = (double) accel[2] * 250 / 32768;

    if (ax < -sensValue || ax > sensValue) {
      if (ay < -sensValue || ay > sensValue) {
        if (az < -sensValue || az > sensValue) {
          if (!is_running) {
            Serial.print("Unlock requesting...\t\t");
            RFduinoBLE.send("S0", 2);
            is_running = true;
            Serial.println("OK");
          }
        }
      }
    }
  }
}

void measureShakeLoop() {
  while (1) {
    if (!is_connect) return;
  }
}

void RFduinoBLE_onConnect() {
  Serial.println("RFduino connected... \t\tOK");
  is_connect = true;

  Serial.print("MPU Wake Up... \t\t\t");
  mpu.setSleepMode(false);
  Serial.println(mpu.isSleepMode() ? "Sleep" : "WakeUp");
}

void RFduinoBLE_onDisconnect() {
  Serial.println("RFduino disconnected... \tOK");
  is_connect = false;
  is_running = false;

  Serial.print("MPU Sleeping... \t\t");
  mpu.setSleepMode(false);
  Serial.println(mpu.isSleepMode() ? "Sleep" : "WakeUp");

  EEPROM.write(PASS_INDEX, pass);
  EEPROM.write(SENS_INDEX, sens);
}

void RFduinoBLE_onReceive(char *data, int len) {
  String receiveData = String(data);
  receiveData = receiveData.substring(0, len);
  Serial.print("Message Received...\t\t");
  Serial.println(receiveData);

  String command = receiveData.substring(0, 2);

  if (pass == 0) { // 스마트폰 등록 이전
    if (command.equals("K0")) {
      char options[12];
      strcpy(options, "R0");
      strcat(options, secret_key);

      Serial.print("Message Sending...\t\t");
      Serial.println(options);
      RFduinoBLE.send(options, 12);
    } else if (command.equals("K1")) {
      pass = 1;
    }
  } else {  // 스마트폰 등록 이후
    if (command.equals("K0")) {
      RFduinoBLE.send("R1", 2);
    } else if (command.equals("R0")) {
      String receiveKey = receiveData.substring(2, receiveData.length());
      if (String(secret_key).equals(receiveKey)) {
        openShakey();
        delay(1000);
        closeShakey();
      } else {
        Serial.println("시크릿 키 불일치");
      }
      is_running = false;
    } else if (command.equals("R1")) {
      String receiveKey = receiveData.substring(2, receiveData.length());
      if (String(secret_key).equals(receiveKey)) {
        pass = 0;
        sens = 3;
        RFduinoBLE.send("A0", 2);
      } else {
        Serial.println("시크릿 키 불일치");
      }
    } else if (command.equals("R2")) {
      char options[3];
      strcpy(options, "R2");
      options[2] = charToInt(sens);
      RFduinoBLE.send(options, 3);
    } else if (command.equals("S0")) {
      changeSens(receiveData);
    }
  }
}

void openShakey() {
  Serial.print("Shakey...\t\t\t");
  digitalWrite(SOLENOID, HIGH);
  Serial.println("OPEN");
}

void closeShakey() {
  Serial.print("Shakey...\t\t\t");
  digitalWrite(SOLENOID, LOW);
  Serial.println("CLOSE");
}

void changeSens(String message) {
  Serial.print("Change Sens Value...\t\t");

  String value = message.substring(2, 3);
  sens = value.toInt();
  sensValue = getSens(sens);

  Serial.println("OK");
}

int charToInt(int value) {
  return value + 48;
}

int getSens(short sens_index) {
  if (sens_index == 1) {
    return SENSITIVITY1;
  } else if (sens_index == 2) {
    return SENSITIVITY2;
  } else if (sens_index == 3) {
    return SENSITIVITY3;
  } else if (sens_index == 4) {
    return SENSITIVITY4;
  } else {
    return SENSITIVITY5;
  }
}