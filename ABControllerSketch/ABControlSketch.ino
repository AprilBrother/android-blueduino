#include <SoftwareSerial.h>

SoftwareSerial mySerial(11, 12); // RX, TX

#define TOTAL_PINS 18

#define PIN_CAPABILITY_NONE      0x00
#define PIN_CAPABILITY_DIGITAL   0x01
#define PIN_CAPABILITY_ANALOG    0x02
#define PIN_CAPABILITY_PWM       0x04
#define PIN_CAPABILITY_SERVO     0x08
#define PIN_CAPABILITY_I2C       0x10

#define PIN_MODE_INPUT            0x00
#define PIN_MODE_OUTPUT           0x01
#define PIN_MODE_ANALOG           0x02
#define PIN_MODE_PWM              0x03

#define PIN_STATE_HIGH            0x01
#define PIN_STATE_LOW            0x00

#define WAIT_SECONDS          3

#define FIRST_ANALOG_PIN        5
#define IS_PIN_DIGITAL(p)       (((p) >= 0 && (p) < TOTAL_PINS))
#define IS_PIN_ANALOG(p)        ((p) >= 5 && (p) < 14)
#define IS_PIN_PWM(p)           ((p) >= 3 && (p) <= 7)
#define PIN_TO_ANALOG(p)        ((p) - FIRST_ANALOG_PIN)

byte pinSerial[] = {0,1,2,3,5,6,9,10,4,8,18,19,20,21,7,14,15,16};
int pinAnalog[] = {A7,A9,A10,A6,A8,A0,A1,A2,A3};
byte pin_mode[TOTAL_PINS];
byte pin_state[TOTAL_PINS];
byte pin_pwm[TOTAL_PINS];
byte pin_servo[TOTAL_PINS];

void setup()
{
  mySerial.begin(9600);
  
   for (int pin = 0; pin < TOTAL_PINS; pin++)
    {
        // Set pin to input with internal pull up
        pinMode(pinSerial[pin], OUTPUT);
        digitalWrite(pinSerial[pin], HIGH);
        
        // Save pin mode and state
        pin_mode[pin] = PIN_MODE_OUTPUT;
        pin_state[pin] = HIGH;
    }
}

byte reportDigitalInput()
{
    static byte pin = 0;
    byte report = 0;
    
    if (!IS_PIN_DIGITAL(pin))
    {
        pin++;
        if (pin >= TOTAL_PINS)
            pin = 0;
        return 0;
    }
    
    if (pin_mode[pin] == PIN_MODE_INPUT)
    {
        byte current_state = digitalRead(pinSerial[pin]);
        
        if (pin_state[pin] != current_state)
        {
            pin_state[pin] = current_state;
            byte buf[] = {'G', pin, pin_mode[pin], current_state};
            mySerial.write(buf, 4);
            
            report = 1;
        }
    }
    
    pin++;
    if (pin >= TOTAL_PINS)
        pin = 0;
    
    return report;
}


void reportPinCapability(byte pin)
{
    byte buf[] = {'P', pin, 0x00};
    byte pin_cap = 0;
    
    if (IS_PIN_DIGITAL(pin))
        pin_cap |= PIN_CAPABILITY_DIGITAL;
    
    if (IS_PIN_ANALOG(pin))
        pin_cap |= PIN_CAPABILITY_ANALOG;
    
    if (IS_PIN_PWM(pin))
        pin_cap |= PIN_CAPABILITY_PWM;
    
//    if (IS_PIN_SERVO(pin))
//        pin_cap |= PIN_CAPABILITY_SERVO;
    
    buf[2] = pin_cap;
    mySerial.write(buf, 3);
}
void reportPinDigitalData(byte pin)
{
    byte state = digitalRead(pin);
    byte mode = pin_mode[pin];
    byte buf[] = {'G', pin, mode, state};
    mySerial.write(buf, 4);
}

void reportPinPWMData(byte pin)
{
    byte value = pin_pwm[pin];
    byte mode = pin_mode[pin];
    byte buf[] = {'G', pin, mode, value};
    mySerial.write(buf, 4);
}

byte reportPinAnalogData()
{
    static byte pin = 0;
    byte report = 0;
    
    if (pin_mode[pin] == PIN_MODE_ANALOG)
    {
        delay(WAIT_SECONDS);
        uint16_t value = analogRead(pinAnalog[PIN_TO_ANALOG(pin)]);
        byte value_lo = value;
        byte value_hi = value>>8;
        
        byte mode = pin_mode[pin];
        mode = (value_hi << 4) | mode;
        
        byte buf[] = {'G', pin, mode, value_lo};
        mySerial.write(buf, 4);
    }
    
    pin++;
    if (pin >= TOTAL_PINS)
        pin = 0;
    
    return report;
}

void loop()
{
  while(mySerial.available() > 0)
  {
    byte cmd;
    cmd = mySerial.read();
    switch (cmd)
    {
      case 'C':
      {
        byte buf[2];
        buf[0] = 'C';
        buf[1] = TOTAL_PINS;
        mySerial.write(buf, 2);
      }
      break;
      case 'A':
      {
         for (int pin = 0; pin < TOTAL_PINS; pin++)
            {
                    reportPinCapability(pin);
                    if ( (pin_mode[pin] == PIN_MODE_INPUT) || (pin_mode[pin] == PIN_MODE_OUTPUT) )
                    {
                        reportPinDigitalData(pin);
                    }
                    else if (pin_mode[pin] == PIN_MODE_PWM)
                    {
                        reportPinPWMData(pin);
                    }
                    else if (pin_mode[pin] == PIN_MODE_ANALOG)
                    {
                         reportPinDigitalData(pin);
                    }
             }
      }
      break;
      case 'T':
      {
        delay(WAIT_SECONDS);
        int pin = mySerial.read();
        delay(WAIT_SECONDS);
        int state = mySerial.read();
        
        if (state == PIN_STATE_HIGH) {
          digitalWrite(pinSerial[pin], HIGH);
          pin_state[pin] = HIGH;
        } else {
          digitalWrite(pinSerial[pin], LOW);
          pin_state[pin] = LOW;
        }
      }
      break;
      case 'N':
      {
        delay(WAIT_SECONDS);
        byte pin = mySerial.read();
        delay(WAIT_SECONDS);
        byte value = mySerial.read();
        analogWrite(pinSerial[pin], value);
        pin_pwm[pin] = value;
      }
      break;
      case 'S':
      {
        delay(WAIT_SECONDS);
        byte pin = mySerial.read();
        delay(WAIT_SECONDS);
        byte mode = mySerial.read();
       
        if (mode != pin_mode[pin])
        {
          pin_mode[pin] = mode;
          if (mode == PIN_MODE_OUTPUT)
          {
            pinMode(pinSerial[pin], OUTPUT);
            digitalWrite(pinSerial[pin], HIGH);
            pin_state[pin] = HIGH;
            reportPinDigitalData(pin);
          }
          else if (mode == PIN_MODE_INPUT)
          {
             pinMode(pinSerial[pin], INPUT);
             reportPinDigitalData(pin);
          } 
          else if (mode == PIN_MODE_ANALOG && IS_PIN_ANALOG(pin))
          {
            pinMode(pinSerial[pin], INPUT);
            reportPinDigitalData(pin);
          }
          else if (mode == PIN_MODE_PWM && IS_PIN_PWM(pin))
          {
            pinMode(pinSerial[pin], OUTPUT);
            analogWrite(pinSerial[pin], 0);
            pin_pwm[pin] = 0;
            reportPinPWMData(pin);
          }
        }
      }
      break;
    }
    
  }
  
  reportPinAnalogData();
  
  byte input_data_pending = reportDigitalInput();
  if (input_data_pending) 
  {
    return;
  }

  
}



