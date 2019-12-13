package com.uddernetworks.drivestore;

public enum Color {
    RESET(0),
    BOLD(1),
    UNDERLINE(4),
    INVERSE(7),

    // Normal Foreground Colors
    BLACK(30),
    RED(31),
    GREEN(32),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    CYAN(36),
    WHITE(37),

    // Normal Background Colors
    BACK_BLACK(40),
    BACK_RED(41),
    BACK_GREEN(42),
    BACK_YELLOW(43),
    BACK_BLUE(44),
    BACK_MAGENTA(45),
    BACK_CYAN(46),
    BACK_WHITE(47),

    // Strong Foreground Colors
    STRONG_BLACK(90),
    STRONG_RED(91),
    STRONG_GREEN(92),
    STRONG_YELLOW(93),
    STRONG_BLUE(94),
    STRONG_MAGENTA(95),
    STRONG_CYAN(96),
    STRONG_WHITE(97),

    // Strong Background Colors
    STRONG_BACK_BLACK(100),
    STRONG_BACK_RED(101),
    STRONG_BACK_GREEN(102),
    STRONG_BACK_YELLOW(103),
    STRONG_BACK_BLUE(104),
    STRONG_BACK_MAGENTA(105),
    STRONG_BACK_CYAN(106),
    STRONG_BACK_WHITE(107);

    private int id;

    Color(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "\u001B[" + this.id + "m";
    }
}