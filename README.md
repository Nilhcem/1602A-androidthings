# 1602A LCD display driver for Android Things

A port of the LiquidCrystal Arduino library for Android Things.


## Photo

![photo][]

## Download

```groovy
dependencies {
    compile 'com.nilhcem.androidthings:driver-lcd1602:0.0.1'
}
```

## Usage

```java
Lcd1602 lcd = new Lcd1602(GPIO_LCD_RS, GPIO_LCD_EN, GPIO_LCD_D4, GPIO_LCD_D5, GPIO_LCD_D6, GPIO_LCD_D7);
lcd.begin(16, 2);

// load custom character to the LCD
int[] heart = {0b00000, 0b01010, 0b11111, 0b11111, 0b11111, 0b01110, 0b00100, 0b00000};
lcd.createChar(0, heart);

lcd.clear();
lcd.print("Hello,");
lcd.setCursor(0, 1);
lcd.print("Android Things!");
lcd.write(0); // write :heart: custom character

// Later on
lcd.close();
```

## Breadboard

![breadboard][]

## Schematic

![schema][]

[photo]: https://raw.githubusercontent.com/Nilhcem/1602A-androidthings/master/assets/photo.jpeg
[breadboard]: https://raw.githubusercontent.com/Nilhcem/1602A-androidthings/master/assets/breadboard.png
[schema]: https://raw.githubusercontent.com/Nilhcem/1602A-androidthings/master/assets/schema.png
