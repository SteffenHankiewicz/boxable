package be.quodlibet.boxable;

import java.awt.Color;

/**
 * How {@code <code>} spans are tinted: their own text colour on a faint panel, the way inline code
 * reads in a Markdown document. Both are settable so a document can match its own house style;
 * setting either to {@code null} switches that part off, leaving the surrounding text colour and a
 * plain background.
 */
public final class CodeStyle {

    private static Color textColor = new Color(0xbf, 0x77, 0x04);
    private static Color backgroundColor = new Color(0xf6, 0xf7, 0xf8);

    private CodeStyle() {
    }

    public static Color getTextColor() {
        return textColor;
    }

    public static void setTextColor(Color color) {
        textColor = color;
    }

    public static Color getBackgroundColor() {
        return backgroundColor;
    }

    public static void setBackgroundColor(Color color) {
        backgroundColor = color;
    }
}
