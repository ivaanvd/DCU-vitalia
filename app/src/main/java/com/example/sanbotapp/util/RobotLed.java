package com.example.sanbotapp.util;

/**
 * Constantes de LED independientes del SDK, para que el código común compile
 * en todos los flavors. Los valores coinciden con com.sanbot/com.qihancloud
 * opensdk.function.beans.LED.
 */
public final class RobotLed {

    public static final byte PART_ALL = 0;

    public static final byte MODE_CLOSE = 1;
    public static final byte MODE_WHITE = 2;
    public static final byte MODE_RED = 3;
    public static final byte MODE_GREEN = 4;
    public static final byte MODE_PINK = 5;
    public static final byte MODE_PURPLE = 6;
    public static final byte MODE_BLUE = 7;
    public static final byte MODE_YELLOW = 8;

    private RobotLed() {}
}
