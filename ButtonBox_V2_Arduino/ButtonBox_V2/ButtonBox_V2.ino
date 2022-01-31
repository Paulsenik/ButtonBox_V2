
// EXTERNAL MODS:
// https://github.com/BesoBerlin/Arduino-German-Keyboard

#include <Wire.h>
#include <EEPROM.h>
#include <Keyboard.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>


// Display
//
// Declaration for an SSD1306 display connected to I2C (SDA, SCL pins)
// The pins for I2C are defined by the Wire-library. 
// On an arduino UNO:                 A4(SDA), A5(SCL)
// On an arduino MEGA 2560:           20(SDA), 21(SCL)
// On an arduino LEONARDO / MIRCO:    2(SDA),  3(SCL), ...
#define SCREEN_WIDTH 128 // OLED display width, in pixels
#define SCREEN_HEIGHT 64 // OLED display height, in pixels
#define OLED_RESET     -1 // Reset pin # (or -1 if sharing Arduino reset pin)
#define SCREEN_ADDRESS 0x3C ///< See datasheet for Address; 0x3D for 128x64, 0x3C for 128x32
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);
bool visible = true;


// Variables
//
char serialLine[20] = "";
byte currentPage = 0;
int currentState = 0; //0=Main, 1=Settings, 2=Edit(Macro duration), 3=Upload
int currentSettingState = 0; //0=visible selected; 1=macro duration selected 2=upload
bool isKeyboardMode = true; // K=Keyboard, C=Custom
int uploadCounter = 0;
int uploadSize = 1024;


// Keyboard
//
const byte MAXMACROSIZE = 8; // in bytes
const int MAXPRESSDELAY = 1000;
int PRESSDELAY = 0; // for macro
int lastPressed[MAXMACROSIZE/2];
bool lastPressedMode = true; // Keybaord, Custom
//


// Buttons (Matrix)
//
const byte ROWPINS[] = {5,6,7,8}; // OUTPUT/5V
const byte COLUMNPINS[] = {9,10,11,12}; // INPUT/GND
const byte TOTALBUTTONS = 16;
//
bool pressed[TOTALBUTTONS];


// Encoder
#define DT_1 18
#define CLK_1 19
#define SW 4
int eState, eLastState;
long eLastRot;
const int MINDELAY = 75; // cooldown before rotating again
bool SW_value = 1;
//
boolean isPressing = false, hasClickedBefore = false, wasHold = false, wasDouble = false, wasSingle = false;
long timeAtPress = 0l;
const int holdPRESSDELAYMin = 400; // minimum press-duration in millis for longpressaction
const int doublePRESSDELAYMax = 300; // maximum press-duration in millis for doublepressaction
const int minimumDelay = 50;

void setup() {

  Serial.begin(9600);
  Serial.setTimeout(10);
  //while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  //}
  pinMode(LED_BUILTIN, OUTPUT);
  
  // Button_Matrix
  for(int i=0;i<sizeof(ROWPINS);i++)
    pinMode(ROWPINS[i],OUTPUT);
  for(int i=0;i<sizeof(COLUMNPINS);i++)
    pinMode(COLUMNPINS[i],INPUT);

  // Encoder
  pinMode(SW, INPUT_PULLUP);
  pinMode (CLK_1,INPUT);
  pinMode (DT_1,INPUT);
  eLastState = digitalRead(CLK_1);

  // EEPROM
  EEPROM.begin();

  // Display
  if(!display.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS)) {
    Serial.println(F("SSD1306 allocation failed"));
    for(;;); // Don't proceed, loop forever
  }
  repaint();

  sendBoardInfo();
}

void loop() {
  
  checkEncoder();
  checkButtons();
  serialByteTick();
  repaint();
}

void repaint(){  
  display.clearDisplay();
  u8g2_prepare();
  // end init

  // visibility
  if(!visible){
    display.display();
    return;
  }

  if(currentState == 0){//Main
    
    // Mode
    display.drawLine(18,0,18,18,SSD1306_WHITE);
    display.drawLine(0,18,18,18,SSD1306_WHITE);
    display.setCursor(1, 1);     // Start at top-left corner
    display.setTextSize(2);
    if(isKeyboardMode){
      display.write("K");
    }else{
      display.write("C");
    }

    // String buffer for different int
    char cbuffer[6];
    
    // Page
    itoa(currentPage+1,cbuffer,10); // add 1 (to display first page as "1" not "0")
    display.setCursor(0, 48);
    display.write(cbuffer);

    
    //lastpressed
    if(lastPressedMode){ // Keyboard
      for(int i=0;i<MAXMACROSIZE/2;i++) {
        if(lastPressed[i] != 0 && lastPressed[i] != -1){
          // itoa(lastPressed[i],cbuffer,10); //DEC-Value of Macro
          display.setCursor(32,0+16*i);
          display.write(getKeyName(lastPressed[i]));
        }
      }
    }else{//custom
      if(lastPressed[0] != -1){
        display.setCursor(32,0);
        itoa(lastPressed[0],cbuffer,10);
        display.setCursor(32, 0);
        display.write(cbuffer);
      }
    }
    
  }else if(currentState == 1){//Settings
    display.setCursor(0, 0);     // Start at top-left corner
    display.setTextSize(2);
    display.write("Settings");

    display.setCursor(0, 16+16*currentSettingState);
    display.write(16);
    display.setCursor(0, 16);
    display.write(" Visible");
    display.setCursor(0, 32);
    display.write(" Macrodlay");
    display.setCursor(0, 48);
    display.write(" Upload");
    
  }else if(currentState == 2){//Edit
    display.setCursor(0, 0);     // Start at top-left corner
    display.setTextSize(2);
    display.write("Delay:");
    display.setCursor(28, 32);
    char cbuffer[7]; 
    itoa(PRESSDELAY,cbuffer,10);
    display.write(cbuffer);
    display.setCursor(32+8*6, 32);
    display.write("ms");
    
    float finish = ((float)PRESSDELAY)/MAXPRESSDELAY;

    display.drawRoundRect(0, 55, 128, 5,
      2, SSD1306_WHITE);
    display.fillCircle(5+118*finish, 57, 5, SSD1306_WHITE);

  }else if(currentState == 3){//upload
    display.setCursor(0, 0);
    display.setTextSize(2);
    display.write("Progress:");
    display.setCursor(8, 32);
    char cbuffer[7]; 
    itoa(uploadSize,cbuffer,10);
    display.write(cbuffer);
    display.setCursor(60, 32);
    display.write("/");
    itoa(uploadCounter,cbuffer,10);
    display.setCursor(76, 32);
    display.write(cbuffer);

    float finish = 0;

    if(uploadSize != 0)
      finish = ((float)uploadCounter)/uploadSize;

    display.fillRoundRect(0, 30, 128*finish, 18,
      4, SSD1306_INVERSE);
    display.drawRoundRect(0, 30, 128, 18,
      4, SSD1306_WHITE);
  }

  // push at end
  display.display();
}

void u8g2_prepare() {
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE); // Draw white text
  display.setCursor(0, 0);     // Start at top-left corner
  display.cp437(true);         // Use full 256 char 'Code Page 437' font
}


// Rotary Encoder
void checkEncoder(){
  encoder_checkRotValue();

  int tempValue = digitalRead(SW);
  if(SW_value != tempValue){
    SW_value = tempValue;
    encoder_buttonValueChange();
  }
  
  buttonTick();
}

void encoder_checkRotValue(){
   eState = digitalRead(CLK_1);
    if (eState != eLastState){
      if(eLastRot + MINDELAY < millis()){
        if (digitalRead(DT_1) != eState){
          if(Serial)
            Serial.println(-1);
          encoder_ValueChange(false);
        }else{
          if(Serial)
            Serial.println(1);
          encoder_ValueChange(true);
        }
        eLastRot = millis();
      }
    }
    eLastState = eState;
}

void encoder_buttonValueChange(){

  // prevent ghosting of button
  if (timeAtPress + minimumDelay > millis())
      return;
  
      
  if(!SW_value){ // pressing
    if(!visible){
      visible = true;
      return;
    }
    
    pressing();
  }else{
    releasing();
  }
}

void pressing(){      
  wasSingle = false;
    isPressing = true;

    if (hasClickedBefore && !wasHold) {
      if (!wasDouble) {
        if (timeAtPress + doublePRESSDELAYMax > millis()) {
        encoder_pressDouble();
        wasDouble = true;
        }
      } else {
        wasDouble = false;
      }
    }

    wasHold = false;
    hasClickedBefore = false;
    timeAtPress = millis();
}

void releasing(){
  isPressing = false;

    if (!hasClickedBefore && !wasHold) {
      hasClickedBefore = true;
    }

    timeAtPress = millis();
}

void buttonTick() {
  if (!hasClickedBefore && isPressing && !wasHold && !wasDouble) { // 1.
      if (timeAtPress + holdPRESSDELAYMin < millis()) {
        encoder_pressHold();
        wasHold = true;
      }
    }

    if (hasClickedBefore && !isPressing && !wasDouble && !wasHold) {
      if (timeAtPress + doublePRESSDELAYMax < millis()) {
        encoder_pressSingle();
        hasClickedBefore = false;
      }
    }
}

void encoder_ValueChange(bool forward){  
  
  if(!visible){
    visible = true;
    return;
  }
  
  if(currentState == 0){//Main
    if(forward){
      currentPage++;
    }else{
      if(currentPage != 0)
        currentPage--;
    }
  }else if(currentState == 1){//Settings
    if(forward){
      if(currentSettingState == 0)
        currentSettingState = 1;
      else if(currentSettingState == 1)
        currentSettingState = 2;
    }else{
      if(currentSettingState == 2)
        currentSettingState = 1;
      else if(currentSettingState == 1)
        currentSettingState = 0;
    }
  }else if(currentState == 2){//Edit
    if(forward){
      if(PRESSDELAY < MAXPRESSDELAY)
        PRESSDELAY+=50;
    }else{
      if(PRESSDELAY > 0)
        PRESSDELAY-=50;
    }
  }
  repaint();
}

void encoder_pressHold(){
  if(Serial)
    Serial.println("HOLD");
  
  if(currentState == 0){//Main
    visible = false;
  }
  repaint();
}

void encoder_pressSingle(){
  if(Serial)
    Serial.println("SINGLE");
  
  if(currentState == 0){//Main
    if(isKeyboardMode){
      // TODO remove all keystrokes
      isKeyboardMode = false;
    }else{
      isKeyboardMode = true;
    }
    repaint();
  }else if(currentState == 1){//Settings
    if(currentSettingState == 0){ // visible
      visible = false;
    }else if(currentSettingState == 1){ // marcro duration
      currentState = 2;
    }else if(currentSettingState == 2){ // upload visual
      currentState = 3;
    }
  }else if(currentState == 2){//Edit
      currentState = 1;
  }else if(currentState == 3){//Upload
    currentState = 1;
  }
  repaint();
}

void encoder_pressDouble(){
  if(Serial)
    Serial.println("DOUBLE");
  
  if(currentState == 0){//Main
    currentState = 1;
  }else if(currentState == 1){//Settings
    currentState = 0;
  }else if(currentState == 2){//Edit
    currentState = 1;
  }else if(currentState == 3){//Upload
    currentState = 1;
  }
  repaint();
}


void checkButtons(){
  
  for(int i=0;i<sizeof(ROWPINS);i++){
    digitalWrite(ROWPINS[i], HIGH);
    for(int j=0;j<sizeof(COLUMNPINS);j++){
      
      int value = digitalRead(COLUMNPINS[j]);
      byte index = i*sizeof(COLUMNPINS)+j;

      if(value == HIGH){
        if(!pressed[index]){
          
          if(isKeyboardMode){      
            keyAction(index);
          }else{
            customAction(index);
          }           
          pressed[index] = true;
        }
      }else{        
        if(pressed[index]){
          if(isKeyboardMode){
            Keyboard.begin();
            Keyboard.releaseAll();
            Keyboard.end();
            Serial.println("rls"); /// releasing all Keys!!
          }else{
            customAction(index);
          }
          pressed[index] = false;
        }
      }
    }      
    digitalWrite(ROWPINS[i], LOW);
  }
}

// only for Buttons
// triggers macro for a set index & page
void keyAction(byte index){
  int totalIndex = currentPage * TOTALBUTTONS + index;
  int lastIndex = 0;

  Serial.println(currentPage);
  
  for(int i=0;i<EEPROM.length();i++){
    
    if(lastIndex < totalIndex){
      i += EEPROM[i];
      lastIndex++;
      
    }else{ //location found

      digitalWrite(LED_BUILTIN, HIGH);
      if(Serial){
        Serial.print("<M>: "); // start Macro
        Serial.println(lastIndex); // start Macro
      }
      Keyboard.begin();
      
      int value;
      byte macroSize = EEPROM[i];
      if(macroSize > MAXMACROSIZE)
        return;
      
      for(int j=0;j < MAXMACROSIZE/2;j++) {
        lastPressed[j] = -1;
      }
      byte lpindex = 0;
      
      if(macroSize != 0){
        for(byte j=0;j<macroSize;j+=2){
          int address = i+j+1;
          if(address < EEPROM.length()-1){
            EEPROM.get(address,value);

            if(value != 0){ // 0~NULL  -1~Zero_or_SPACE
              if(Serial){
                Serial.print("<V>: "); // Value
                char cbuffer[5]; 
                itoa(value,cbuffer,10);
                Serial.println(cbuffer);
              }

              Keyboard.press(toPressValue(value));
              lastPressed[lpindex++] = value;
              if(PRESSDELAY != 0)
                delay(PRESSDELAY);
            }
          }
        }
      }
  
      lastPressedMode = true;

      // Only Macros with 1 KEY can be pressed down for as long as the button is pressed
      if(macroSize != 2){
        Keyboard.releaseAll();
        Keyboard.end();
      }
      
      if(Serial)
        Serial.println("</M>"); // End Macro
      digitalWrite(LED_BUILTIN, LOW);

      repaint();
      break;
    }
  }
}

// custom button Output via Serial
void customAction(byte index){
  int totalIndex = currentPage * TOTALBUTTONS + index;

  if(Serial){
    Serial.print("BB[");// BB = ButtonBox
    if(pressed[index]){
      Serial.print("-");
      Serial.print(totalIndex);
    }else{
      Serial.print(totalIndex);
    }
    Serial.println("]");
  }
  lastPressed[0] = totalIndex;
  
  lastPressedMode = false;
  
  repaint();
}

void checkSerialInput(){
  /*
  for(int i=0;i<strlen(serialLine);i++)
    Serial.println(serialLine[i], DEC);
  */
  
  if(strcmp(serialLine, "") != 0){ // valid SerialInput
    /*
    if(Serial){
      Serial.print("in=");
      Serial.println(serialLine);
    }
    */
    if(strcmp(serialLine, "g[info]") == 0){ // get all
      sendBoardInfo();
    }else if(strcmp(serialLine, "g[all]") == 0){ // get all
      sendEEPROMData();
      
    }else if(strcmp(serialLine, "r[ino]") == 0){ // restart Arduino
      restartArduino();
      
    }else if(strcmp(serialLine, "r[all]") == 0){ // reset all
      for(int i=0;i< EEPROM.length();i++)
        EEPROM.update(i,0);
        
      if(Serial)
        Serial.println("reset");
        
    }else if(strlen(serialLine) >= 5 && serialLine[0]=='u' && serialLine[1]=='s' && serialLine[2]=='[' && serialLine[strlen(serialLine)-1]==']'){ // try to format protocol-input to set EEPROM
      // uploadSize
      
      char tempS[7] = "";
      int j=0;

      for(int i=3;i<strlen(serialLine);i++){
          if(serialLine[i]==']'){
            //uploadSize = atoi(tempS); // potetially replace
            uploadCounter = 0;
            break;
          }else{ 
            tempS[j++]=serialLine[i]; // put InputChar at location i & increment it
            tempS[j]='\0';
          }
      }
    }else if(strlen(serialLine) >= 6 && serialLine[0]=='b' && serialLine[1]=='[' && serialLine[strlen(serialLine)-1]==']'){ // try to format protocol-input to set EEPROM
      
      // get address and key
      char tempS[7] = "";
      bool hasFirstArg = false;
      int loc, value;
      
      int j=0;
      
      for(int i=2;i<strlen(serialLine);i++){
        if(!hasFirstArg){ // 1. Arg      
          if(serialLine[i]==','){
            loc = atoi(tempS);
            tempS[0] = '\0';
            hasFirstArg = true;
            
            if(loc >= EEPROM.length())
            return;

          }else{            
            tempS[j++]=serialLine[i]; // put InputChar at location i & increment it
            tempS[j]='\0';
          }
        }else{ // 2. Arg
          if(serialLine[i]==']'){
            value = atoi(tempS);
            uploadCounter = loc;
            EEPROM.put(loc,value);
            /*
            if(Serial){
              Serial.print("set");
              Serial.print(loc);
              Serial.print(" ");
              Serial.println(value);
            }
            */
            break;
          }else{
            int j=strlen(tempS);
            tempS[j++]=serialLine[i]; // put InputChar at location i & increment i
            tempS[j]='\0';
          }
        }
      }      
    }    
  }
}

void(* resetFunc) (void) = 0;

void restartArduino(){
  resetFunc();
  delay(500);
}

void sendBoardInfo(){
  if(!Serial)
    return;
  // init Complete
  Serial.println("ButtonBox[V2.0.0]");

  // send EEPROM-size
  Serial.print("s[");
  Serial.print(EEPROM.length());
  Serial.println("]");
}

// sends Arduino EEPROM data
void sendEEPROMData(){
  if(!Serial)
    return;
  // send EEPROM-size
  Serial.print("s[");
  Serial.print(EEPROM.length());
  Serial.println("]");
    
  for(int i=0;i< EEPROM.length();i++){
    int value = 0;
    // EEPROM.get(i,value); // read both bytes
    value = EEPROM.read(i); // read byte only on i
    Serial.print("b[");
    Serial.print(i);
    Serial.print(",");
    Serial.print(value);
    Serial.println("]");
  }
}

void serialByteTick(){
  if(Serial.available() ){
    char inChar = (char)Serial.read();
        
    if(inChar != 10 && inChar != 13){
      int i=strlen(serialLine);
      if(i+1 >= sizeof(serialLine))
        return;
      serialLine[i++]=inChar; // put InputChar at location i & increment i
      serialLine[i]='\0';
    }
    if(inChar == 93){ // ']'
      checkSerialInput();
      // Serial.println(serialLine);
      serialLine[0] = '\0'; // clear
    }
  }
}

char a[4];
char* getKeyName(int decValue){
  switch(decValue){
    case 128:
    case 132:
      return "CTRL";
    case 129:
    case 133:
      return "SHIFT";
    case 130:
    case 134:
      return "ALT";
    case 131:
    case 135:
      return "WIN";

    case 218:
      return "UP";
    case 217:
      return "DOWN";
    case 216:
      return "LEFT";
    case 215:
      return "RIGHT";

    case 32:
      return "SPACE";
    case 178:
      return "BACKSPACE";
    case 179:
      return "TAB";
    case 176:
      return "ENTER";//RETURN
    case 177:
      return "ESC";
    case 209:
      return "INSERT";
    case 212:
      return "DELETE";
    case 211:
      return "PAGE-UP";
    case 214:
      return "PAGE-DWN";
    case 210:
      return "HOME";
    case 213:
      return "END";
    case 193:
      return "CAPSLOCK";
    case 145:
      return "SCRLLOCK";
    case 144:
      return "NUM-LOCK";

    case 194:
      return "F1";
    case 195:
      return "F2";
    case 196:
      return "F3";
    case 197:
      return "F4";
    case 198:
      return "F5";
    case 199:
      return "F6";
    case 200:
      return "F7";
    case 201:
      return "F8";
    case 202:
      return "F9";
    case 203:
      return "F10";
    case 204:
      return "F11";
    case 205:
      return "F12";
  }
  a[0] = decValue;
  a[1] = '\0';
  return a;
}

// converts stored Keyvalues to printable values for KEYBOARD
int toPressValue(int input){
  switch(input){
    case 1000: // reserved for space
      return 32; // only testpurpopse
    default:
      return input;
  }
}
