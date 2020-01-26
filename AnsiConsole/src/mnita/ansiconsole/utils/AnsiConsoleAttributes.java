package mnita.ansiconsole.utils;

import static mnita.ansiconsole.utils.AnsiCommands.COMMAND_COLOR_INTENSITY_DELTA;

import mnita.ansiconsole.preferences.AnsiConsolePreferenceUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public class AnsiConsoleAttributes {
    public static final int UNDERLINE_NONE = -1; // nothing in SWT, a bit of an abuse

    private static final boolean USE_WINDOWS_MAPPING = AnsiConsolePreferenceUtils.useWindowsMapping();

    public Integer currentBgColor;
    public Integer currentFgColor;
    public int     underline;
    public boolean bold;
    public boolean italic;
    public boolean invert;
    public boolean conceal;
    public boolean strike;
    public boolean framed;

    public AnsiConsoleAttributes() {
        reset();
    }

    public void reset() {
        currentBgColor = null;
        currentFgColor = null;
        underline = UNDERLINE_NONE;
        bold = false;
        italic = false;
        invert = false;
        conceal = false;
        strike = false;
        framed = false;
    }

    public static AnsiConsoleAttributes from(AnsiConsoleAttributes other) {
        AnsiConsoleAttributes result = new AnsiConsoleAttributes();
        if (other != null) {
            result.currentBgColor = other.currentBgColor;
            result.currentFgColor = other.currentFgColor;
            result.underline = other.underline;
            result.bold = other.bold;
            result.italic = other.italic;
            result.invert = other.invert;
            result.conceal = other.conceal;
            result.strike = other.strike;
            result.framed = other.framed;
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (currentBgColor != null) result.append("Bg" + currentBgColor);
        if (currentFgColor != null) result.append("Fg" + currentFgColor);
        if (underline != UNDERLINE_NONE) result.append("_");
        if(bold) result.append("\ud835\uddef"); // 𝗯
        if(italic) result.append("\ud835\udc56"); // 𝑖
        if(invert) result.append("\u00bf"); // ¿
        if(conceal) result.append("\u2702"); // ✂
        if(strike) result.append("\u2014"); // —
        if(framed) result.append("\u2610"); // ☐
        return result.toString();
    }

    public static Color hiliteRgbColor(Color c) {
        if (c == null)
            return ColorCache.get(new RGB(0xff, 0xff, 0xff));
        int red = c.getRed() * 2;
        int green = c.getGreen() * 2;
        int blue = c.getBlue() * 2;

        if (red > 0xff)   red = 0xff;
        if (green > 0xff) green = 0xff;
        if (blue > 0xff)  blue = 0xff;

        return ColorCache.get(new RGB(red, green, blue)); // here
    }

    // This function maps from the current attributes as "described" by escape sequences to real,
    // Eclipse console specific attributes (resolving color palette, default colors, etc.)
    public static void updateRangeStyle(StyleRange range, AnsiConsoleAttributes attribute) {
        AnsiConsoleAttributes tempAttrib = AnsiConsoleAttributes.from(attribute);

        boolean hilite = false;

        if (USE_WINDOWS_MAPPING) {
            if (tempAttrib.bold) {
                tempAttrib.bold = false; // not supported, rendered as intense, already done that
                hilite = true;
            }
            if (tempAttrib.italic) {
                tempAttrib.italic = false;
                tempAttrib.invert = true;
            }
            tempAttrib.underline = UNDERLINE_NONE; // not supported on Windows
            tempAttrib.strike = false; // not supported on Windows
            tempAttrib.framed = false; // not supported on Windows
        }

        // Prepare the foreground color
        if (hilite) {
            if (tempAttrib.currentFgColor == null) {
                range.foreground = AnsiConsolePreferenceUtils.getDebugConsoleFgColor();
                range.foreground = hiliteRgbColor(range.foreground);
            } else {
                if (tempAttrib.currentFgColor < COMMAND_COLOR_INTENSITY_DELTA)
                    range.foreground = ColorCache.get(AnsiConsoleColorPalette.getColor(tempAttrib.currentFgColor + COMMAND_COLOR_INTENSITY_DELTA));
                else
                    range.foreground = ColorCache.get(AnsiConsoleColorPalette.getColor(tempAttrib.currentFgColor));
            }
        } else {
            if (tempAttrib.currentFgColor != null)
                range.foreground = ColorCache.get(AnsiConsoleColorPalette.getColor(tempAttrib.currentFgColor));
        }

        // Prepare the background color
        if (tempAttrib.currentBgColor != null)
            range.background = ColorCache.get(AnsiConsoleColorPalette.getColor(tempAttrib.currentBgColor));

        // These two still mess with the foreground/background colors
        // We need to solve them before we use them for strike/underline/frame colors
        if (tempAttrib.invert) {
            if (range.foreground == null)
                range.foreground = AnsiConsolePreferenceUtils.getDebugConsoleFgColor();
            if (range.background == null)
                range.background = AnsiConsolePreferenceUtils.getDebugConsoleBgColor();
            Color tmp = range.background;
            range.background = range.foreground;
            range.foreground = tmp;
        }

        if (tempAttrib.conceal) {
            if (range.background == null)
                range.background = AnsiConsolePreferenceUtils.getDebugConsoleBgColor();
            range.foreground = range.background;
        }

        range.font = null;
        range.fontStyle = SWT.NORMAL;
        // Prepare the rest of the attributes
        if (tempAttrib.bold)
            range.fontStyle |= SWT.BOLD;

        if (tempAttrib.italic)
            range.fontStyle |= SWT.ITALIC;

        if (tempAttrib.underline != UNDERLINE_NONE) {
            range.underline = true;
            range.underlineColor = range.foreground;
            range.underlineStyle = tempAttrib.underline;
        }
        else
            range.underline = false;

        range.strikeout = tempAttrib.strike;
        range.strikeoutColor = range.foreground;

        if (tempAttrib.framed) {
            range.borderStyle = SWT.BORDER_SOLID;
            range.borderColor = range.foreground;
        }
        else
            range.borderStyle = SWT.NONE;
    }
}
