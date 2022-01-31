
#include "Keyboard.h"

void setup() {
  // open the serial port:
  Serial.begin(9600);
  // initialize control over the keyboard:
}

void loop() {
  if(Serial.available() > 1){
      Serial.println("pressing");
      Keyboard.begin();
      Keyboard.press(129);
      Keyboard.press('2');
      delay(100);
      Keyboard.releaseAll();
      Keyboard.end();
  }
}
