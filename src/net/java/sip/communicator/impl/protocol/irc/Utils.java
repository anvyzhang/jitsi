/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.util.*;

/**
 * Some IRC-related utility methods.
 *
 * @author Danny van Heumen
 */
public final class Utils
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    /**
     * Index indicating the end of the color code.
     */
    private static final int INDEX_END_COLOR_CODE = 3;

    /**
     * Private constructor since we do not need to construct anything.
     */
    private Utils()
    {
    }

    /**
     * Parse IRC text message and process possible control codes.
     *
     * TODO Support for color 99 (Transparent) TODO Support for wrapping around
     * after color 15?
     *
     * @param text the message
     * @return returns the processed message or null if text message was null,
     *         since there is nothing to modify there
     */
    public static String parse(final String text)
    {
        if (text == null)
        {
            return null;
        }

        FormattedTextBuilder builder = new FormattedTextBuilder();
        for (int i = 0; i < text.length(); i++)
        {
            char val = text.charAt(i);
            switch (val)
            {
            case '\u0002':
                if (builder.isActive(ControlChar.Bold.class))
                {
                    builder.cancel(ControlChar.Bold.class, true);
                }
                else
                {
                    builder.apply(new ControlChar.Bold());
                }
                break;
            case '\u0016':
                if (builder.isActive(ControlChar.Italics.class))
                {
                    builder.cancel(ControlChar.Italics.class, true);
                }
                else
                {
                    builder.apply(new ControlChar.Italics());
                }
                break;
            case '\u001F':
                if (builder.isActive(ControlChar.Underline.class))
                {
                    builder.cancel(ControlChar.Underline.class, true);
                }
                else
                {
                    builder.apply(new ControlChar.Underline());
                }
                break;
            case '\u0003':
                Color foreground = null;
                Color background = null;
                try
                {
                    // parse foreground color code
                    foreground = parseForegroundColor(text.substring(i + 1));
                    i += 2;

                    // parse background color code
                    background = parseBackgroundColor(text.substring(i + 1));
                    i += INDEX_END_COLOR_CODE;
                }
                catch (IllegalArgumentException e)
                {
                    LOGGER.debug(
                        "Invalid color code: " + text.substring(i + 1), e);
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    LOGGER.debug("Unknown color code referenced: "
                        + text.substring(i + 1));
                }
                if (foreground == null && background == null)
                {
                    builder.cancel(ControlChar.ColorFormat.class, false);
                }
                else
                {
                    builder.apply(new ControlChar.ColorFormat(foreground,
                        background));
                }
                break;
            case '\u000F':
                builder.cancelAll();
                break;
            default:
                // value is a normal character, just append
                builder.append(val);
                break;
            }
        }
        return builder.done();
    }

    /**
     * Parse background color code starting with the separator.
     *
     * @param text the text starting with the background color (separator)
     * @return returns the background color
     */
    private static Color parseBackgroundColor(final String text)
    {
        try
        {
            if (text.charAt(0) == ',')
            {
                // if available, also parse background color
                int color =
                    Integer.parseInt("" + text.charAt(1) + text.charAt(2));
                color = color % Color.values().length;
                return Color.values()[color];
            }
            throw new IllegalArgumentException(
               "no color separator present, hence no background color present");
        }
        catch (StringIndexOutOfBoundsException e)
        {
            // Abort parsing background color. Assume only
            // foreground color available.
            throw new IllegalArgumentException(
                "text stopped before the background color code was finished");
        }
        catch (NumberFormatException e)
        {
            // No background color defined, ignoring ...
            throw new IllegalArgumentException(
                "value isn't a background color code: "
                    + text.substring(1, INDEX_END_COLOR_CODE));
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            LOGGER.debug("Specified IRC color is not a known color code: "
                + e.getMessage());
            throw new IllegalArgumentException("background color value "
                + text.substring(1, INDEX_END_COLOR_CODE)
                + " is not a known color");
        }
    }

    /**
     * Parse foreground color and return corresponding Color instance.
     *
     * @param text the text to parse, starting with color code
     * @return returns Color instance
     */
    private static Color parseForegroundColor(final String text)
    {
        try
        {
            int color = Integer.parseInt("" + text.charAt(0) + text.charAt(1));
            color = color % Color.values().length;
            return Color.values()[color];
        }
        catch (StringIndexOutOfBoundsException e)
        {
            // Invalid control code, since text has ended.
            LOGGER.trace("ArrayIndexOutOfBounds during foreground "
                + "color control code parsing.");
            throw new IllegalArgumentException("missing foreground color code");
        }
        catch (NumberFormatException e)
        {
            // FIXME correctly print out color code (as a number or hex number)
            // Invalid text color value
            LOGGER.trace("Invalid foreground color code encountered.");
            throw new IllegalArgumentException(
                "invalid foreground color code: " + text.substring(0, 2));
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            LOGGER.debug("Specified IRC color is not a known color code: "
                + e.getMessage());
            throw new IllegalArgumentException("foreground color value "
                + text.substring(0, 2) + " is not a known color");
        }
    }

    /**
     * Format message as normal HTML-formatted message.
     *
     * @param message original IRC message
     * @return returns HTML-formatted normal message
     */
    public static String formatMessage(final String message)
    {
        return message;
    }

    /**
     * Format message as HTML-formatted notice.
     *
     * @param message original IRC message
     * @param user user nick name
     * @return returns HTML-formatted notice
     */
    public static String formatNotice(final String message, final String user)
    {
        return "<i>" + user + "</i>: " + message;
    }

    /**
     * Format message as HTML-formatted action.
     *
     * @param message original IRC message
     * @param user user nick name
     * @return returns HTML-formatted action
     */
    public static String formatAction(final String message, final String user)
    {
        return "<b>*" + user + "</b> " + message;
    }
}
