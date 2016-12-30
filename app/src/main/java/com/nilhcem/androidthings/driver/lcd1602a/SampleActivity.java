package com.nilhcem.androidthings.driver.lcd1602a;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class SampleActivity extends Activity {

    private static final String TAG = SampleActivity.class.getSimpleName();

    private static final String GPIO_LCD_RS = "BCM26";
    private static final String GPIO_LCD_EN = "BCM19";

    private static final String GPIO_LCD_D4 = "BCM21";
    private static final String GPIO_LCD_D5 = "BCM20";
    private static final String GPIO_LCD_D6 = "BCM16";
    private static final String GPIO_LCD_D7 = "BCM12";

    private Lcd1602 lcd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            lcd = new Lcd1602(GPIO_LCD_RS, GPIO_LCD_EN, GPIO_LCD_D4, GPIO_LCD_D5, GPIO_LCD_D6, GPIO_LCD_D7);
            lcd.begin(16, 2);

            // load characters to the LCD
            int[] heart = {0b00000, 0b01010, 0b11111, 0b11111, 0b11111, 0b01110, 0b00100, 0b00000};
            int[] smiley = {0b00000, 0b00000, 0b01010, 0b00000, 0b00000, 0b10001, 0b01110, 0b00000};
            lcd.createChar(0, heart);
            lcd.createChar(1, smiley);

            lcd.clear();
            lcd.print("Hello ");
            lcd.write(1); // :)
            lcd.write(',');
            lcd.setCursor(0, 1);
            lcd.print("Android Things!");
            lcd.write(0); // <3
        } catch (IOException e) {
            Log.e(TAG, "Error initializing LCD", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (lcd != null) {
            try {
                lcd.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing", e);
            } finally {
                lcd = null;
            }
        }
    }
}
