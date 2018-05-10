package com.nilhcem.androidthings.driver.lcd1602a;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

/**
 * Inspired from {@code https://github.com/arduino/Arduino/tree/master/libraries/LiquidCrystal/src}
 */
public class Lcd1602 implements AutoCloseable {

    // commands
    private static final int LCD_CLEARDISPLAY = 0x01;
    private static final int LCD_RETURNHOME = 0x02;
    private static final int LCD_ENTRYMODESET = 0x04;
    private static final int LCD_DISPLAYCONTROL = 0x08;
    private static final int LCD_CURSORSHIFT = 0x10;
    private static final int LCD_FUNCTIONSET = 0x20;
    private static final int LCD_SETCGRAMADDR = 0x40;
    private static final int LCD_SETDDRAMADDR = 0x80;

    // flags for display entry mode
    private static final int LCD_ENTRYRIGHT = 0x00;
    private static final int LCD_ENTRYLEFT = 0x02;
    private static final int LCD_ENTRYSHIFTINCREMENT = 0x01;
    private static final int LCD_ENTRYSHIFTDECREMENT = 0x00;

    // flags for display on/off control
    private static final int LCD_DISPLAYON = 0x04;
    private static final int LCD_DISPLAYOFF = 0x00;
    private static final int LCD_CURSORON = 0x02;
    private static final int LCD_CURSOROFF = 0x00;
    private static final int LCD_BLINKON = 0x01;
    private static final int LCD_BLINKOFF = 0x00;

    // flags for display/cursor shift
    private static final int LCD_DISPLAYMOVE = 0x08;
    private static final int LCD_CURSORMOVE = 0x00;
    private static final int LCD_MOVERIGHT = 0x04;
    private static final int LCD_MOVELEFT = 0x00;

    // flags for function set
    private static final int LCD_8BITMODE = 0x10;
    private static final int LCD_4BITMODE = 0x00;
    private static final int LCD_2LINE = 0x08;
    private static final int LCD_1LINE = 0x00;
    private static final int LCD_5x10DOTS = 0x04;
    private static final int LCD_5x8DOTS = 0x00;

    private Gpio rsGpio; // LOW: command.  HIGH: character.
    private Gpio rwGpio; // LOW: write to LCD.  HIGH: read from LCD.
    private Gpio enableGpio; // activated by a HIGH pulse.
    private Gpio[] dataGpios = new Gpio[8];

    private int displayfunction;
    private int displaycontrol;
    private int displaymode;

    private int numlines;

    public Lcd1602(String rs, String rw, String enable, String d0, String d1, String d2, String d3, String d4, String d5, String d6, String d7) throws IOException {
        this(false, rs, rw, enable, d0, d1, d2, d3, d4, d5, d6, d7);
    }

    public Lcd1602(String rs, String enable, String d0, String d1, String d2, String d3, String d4, String d5, String d6, String d7) throws IOException {
        this(false, rs, null, enable, d0, d1, d2, d3, d4, d5, d6, d7);
    }

    public Lcd1602(String rs, String rw, String enable, String d0, String d1, String d2, String d3) throws IOException {
        this(true, rs, rw, enable, d0, d1, d2, d3, null, null, null, null);
    }

    public Lcd1602(String rs, String enable, String d0, String d1, String d2, String d3) throws IOException {
        this(true, rs, null, enable, d0, d1, d2, d3, null, null, null, null);
    }

    /**
     * <pre>
     * When the display powers up, it is configured as follows:
     * 1. Display clear
     * 2. Function set:
     *   DL = 1; 8-bit interface data
     *   N = 0; 1-line display
     *   F = 0; 5x8 dot character font
     * 3. Display on/off control:
     *   D = 0; Display off
     *   C = 0; Cursor off
     *   B = 0; Blinking off
     * 4. Entry mode set:
     *   I/D = 1; Increment by 1
     *   S = 0; No shift
     *
     * Note, however, that resetting the Arduino doesn't reset the LCD, so we
     * can't assume that it's in that state when a sketch starts (and the
     * Lcd1602a constructor is called).
     * </pre>
     */
    public Lcd1602(boolean fourbitmode, String rs, String rw, String enable, String d0, String d1, String d2, String d3, String d4, String d5, String d6, String d7) throws IOException {
        PeripheralManager manager = PeripheralManager.getInstance();

        rsGpio = manager.openGpio(rs);
        rsGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

        // we can save 1 pin by not using RW. Indicate by passing null instead of pin#
        if (rw != null) {
            rwGpio = manager.openGpio(rw);
            rwGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        }

        enableGpio = manager.openGpio(enable);
        enableGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

        String[] dataPins = new String[]{d0, d1, d2, d3, d4, d5, d6, d7};
        for (int i = 0; i < (fourbitmode ? 4 : 8); i++) {
            dataGpios[i] = manager.openGpio(dataPins[i]);
            dataGpios[i].setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        }

        if (fourbitmode) {
            displayfunction = LCD_4BITMODE | LCD_1LINE | LCD_5x8DOTS;
        } else {
            displayfunction = LCD_8BITMODE | LCD_1LINE | LCD_5x8DOTS;
        }

        begin(16, 1);
    }

    @Override
    public void close() throws Exception {
        if (rsGpio != null) {
            try {
                rsGpio.close();
            } finally {
                rsGpio = null;
            }
        }

        if (rwGpio != null) {
            try {
                rwGpio.close();
            } finally {
                rwGpio = null;
            }
        }

        if (enableGpio != null) {
            try {
                enableGpio.close();
            } finally {
                enableGpio = null;
            }
        }

        for (int i = 0; i < dataGpios.length; i++) {
            if (dataGpios[i] != null) {
                try {
                    dataGpios[i].close();
                } finally {
                    dataGpios[i] = null;
                }
            }
        }
    }

    public void begin(int cols, int lines) throws IOException {
        begin(cols, lines, LCD_5x8DOTS);
    }

    public void begin(int cols, int lines, int dotsize) throws IOException {
        if (lines > 1) {
            displayfunction |= LCD_2LINE;
        }
        numlines = lines;

        // for some 1 line displays you can select a 10 pixel high font
        if ((dotsize != 0) && (lines == 1)) {
            displayfunction |= LCD_5x10DOTS;
        }

        // SEE PAGE 45/46 FOR INITIALIZATION SPECIFICATION!
        // according to datasheet, we need at least 40ms after power rises above 2.7V
        // before sending commands. Arduino can turn on way before 4.5V so we'll wait 50
        delayMicroseconds(50000);
        // Now we pull both RS and R/W low to begin commands
        rsGpio.setValue(false);
        enableGpio.setValue(false);
        if (rwGpio != null) {
            rwGpio.setValue(false);
        }

        //put the LCD into 4 bit or 8 bit mode
        if ((displayfunction & LCD_8BITMODE) == 0) {
            // this is according to the hitachi HD44780 datasheet
            // figure 24, pg 46

            // we start in 8bit mode, try to set 4 bit mode
            write4bits(0x03);
            delayMicroseconds(4500); // wait min 4.1ms

            // second try
            write4bits(0x03);
            delayMicroseconds(4500); // wait min 4.1ms

            // third go!
            write4bits(0x03);
            delayMicroseconds(150);

            // finally, set to 4-bit interface
            write4bits(0x02);
        } else {
            // this is according to the hitachi HD44780 datasheet
            // page 45 figure 23

            // Send function set command sequence
            command(LCD_FUNCTIONSET | displayfunction);
            delayMicroseconds(4500);  // wait more than 4.1ms

            // second try
            command(LCD_FUNCTIONSET | displayfunction);
            delayMicroseconds(150);

            // third go
            command(LCD_FUNCTIONSET | displayfunction);
        }

        // finally, set # lines, font size, etc.
        command(LCD_FUNCTIONSET | displayfunction);

        // turn the display on with no cursor or blinking default
        displaycontrol = LCD_DISPLAYON | LCD_CURSOROFF | LCD_BLINKOFF;
        display();

        // clear it off
        clear();

        // Initialize to default text direction (for romance languages)
        displaymode = LCD_ENTRYLEFT | LCD_ENTRYSHIFTDECREMENT;
        // set the entry mode
        command(LCD_ENTRYMODESET | displaymode);
    }

    /**********
     * high level commands, for the user!
     */
    public void clear() throws IOException {
        command(LCD_CLEARDISPLAY);  // clear display, set cursor position to zero
        delayMicroseconds(2000);  // this command takes a long time!
    }

    public void home() throws IOException {
        command(LCD_RETURNHOME);  // set cursor position to zero
        delayMicroseconds(2000);  // this command takes a long time!
    }

    public void setCursor(int col, int row) throws IOException {
        int[] rowOffsets = {0x00, 0x40, 0x14, 0x54};
        if (row >= numlines) {
            row = numlines - 1;    // we count rows starting w/0
        }

        command(LCD_SETDDRAMADDR | (col + rowOffsets[row]));
    }

    // Turn the display on/off (quickly)
    public void noDisplay() throws IOException {
        displaycontrol &= ~LCD_DISPLAYON;
        command(LCD_DISPLAYCONTROL | displaycontrol);
    }

    public void display() throws IOException {
        displaycontrol |= LCD_DISPLAYON;
        command(LCD_DISPLAYCONTROL | displaycontrol);
    }

    // Turns the underline cursor on/off
    public void noCursor() throws IOException {
        displaycontrol &= ~LCD_CURSORON;
        command(LCD_DISPLAYCONTROL | displaycontrol);
    }

    public void cursor() throws IOException {
        displaycontrol |= LCD_CURSORON;
        command(LCD_DISPLAYCONTROL | displaycontrol);
    }

    // Turn on and off the blinking cursor
    public void noBlink() throws IOException {
        displaycontrol &= ~LCD_BLINKON;
        command(LCD_DISPLAYCONTROL | displaycontrol);
    }

    public void blink() throws IOException {
        displaycontrol |= LCD_BLINKON;
        command(LCD_DISPLAYCONTROL | displaycontrol);
    }

    // These commands scroll the display without changing the RAM
    public void scrollDisplayLeft() throws IOException {
        command(LCD_CURSORSHIFT | LCD_DISPLAYMOVE | LCD_MOVELEFT);
    }

    public void scrollDisplayRight() throws IOException {
        command(LCD_CURSORSHIFT | LCD_DISPLAYMOVE | LCD_MOVERIGHT);
    }

    // This is for text that flows Left to Right
    public void leftToRight() throws IOException {
        displaymode |= LCD_ENTRYLEFT;
        command(LCD_ENTRYMODESET | displaymode);
    }

    // This is for text that flows Right to Left
    public void rightToLeft() throws IOException {
        displaymode &= ~LCD_ENTRYLEFT;
        command(LCD_ENTRYMODESET | displaymode);
    }

    // This will 'right justify' text from the cursor
    public void autoscroll() throws IOException {
        displaymode |= LCD_ENTRYSHIFTINCREMENT;
        command(LCD_ENTRYMODESET | displaymode);
    }

    // This will 'left justify' text from the cursor
    public void noAutoscroll() throws IOException {
        displaymode &= ~LCD_ENTRYSHIFTINCREMENT;
        command(LCD_ENTRYMODESET | displaymode);
    }

    // Allows us to fill the first 8 CGRAM locations with custom characters
    public void createChar(int location, int[] charmap) throws IOException {
        location &= 0x7; // we only have 8 locations 0-7
        command(LCD_SETCGRAMADDR | (location << 3));
        for (int i = 0; i < 8; i++) {
            write(charmap[i]);
        }
    }

    /***********
     * mid level commands, for sending data/cmds
     */
    public void command(int value) throws IOException {
        send(value, false);
    }

    public void write(int value) throws IOException {
        send(value, true);
    }

    public void print(String message) throws IOException {
        for (int i = 0; i < message.length(); i++) {
            write(message.charAt(i));
        }
    }

    /************
     * low level data pushing commands
     **********/

    // write either command or data, with automatic 4/8-bit selection
    private void send(int value, boolean rsValue) throws IOException {
        rsGpio.setValue(rsValue);

        // if there is a RW pin indicated, set it low to Write
        if (rwGpio != null) {
            rwGpio.setValue(false);
        }

        if ((displayfunction & LCD_8BITMODE) == 0) {
            write4bits(value >> 4);
            write4bits(value);
        } else {
            write8bits(value);
        }
    }

    private void pulseEnable() throws IOException {
        enableGpio.setValue(false);
        delayMicroseconds(1);
        enableGpio.setValue(true);
        delayMicroseconds(1);    // enable pulse must be >450ns
        enableGpio.setValue(false);
        delayMicroseconds(100);   // commands need > 37us to settle
    }

    private void write4bits(int value) throws IOException {
        for (int i = 0; i < 4; i++) {
            dataGpios[i].setValue(((value >> i) & 0x01) != 0);
        }

        pulseEnable();
    }

    private void write8bits(int value) throws IOException {
        for (int i = 0; i < 8; i++) {
            dataGpios[i].setValue(((value >> i) & 0x01) != 0);
        }

        pulseEnable();
    }

    private void delayMicroseconds(long microseconds) {
        try {
            Thread.sleep(Math.max(1, Math.round(0.001d * microseconds)));
        } catch (InterruptedException e) {
            Log.e(Lcd1602.class.getSimpleName(), "Sleep error", e);
        }
    }
}
