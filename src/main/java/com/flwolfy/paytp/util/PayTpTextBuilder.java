package com.flwolfy.paytp.util;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PayTpTextBuilder {

  public static final Formatting DEFAULT_TEXT_COLOR = Formatting.GOLD;
  public static final Formatting DEFAULT_HIGHLIGHT_COLOR = Formatting.GREEN;
  public static final Formatting DEFAULT_WARN_COLOR = Formatting.RED;

  private PayTpTextBuilder() {}

  // ========================================= //
  // ============= Text Formatting =========== //
  // ========================================= //

  /**
   * Formats a {@link Text} template with color highlighting for inserted arguments.
   * <p>
   * This method works similarly to {@link String#format(String, Object...)}, but supports
   * Minecraft's {@link Text} components and color formatting. Each occurrence of <code>%s</code>
   * in the input text will be replaced with the corresponding argument from {@code args}, and
   * highlighted with a specified color.
   * </p>
   *
   * Example:
   * <pre>
   *   format(Text.literal("Hello, %s!"), Formatting.WHITE, Formatting.GOLD, "Steve");
   *   // => "Hello, " (white) + "Steve" (gold) + "!" (white)
   * </pre>
   *
   * @param template        The base {@link Text} template containing "%s" placeholders.
   * @param textColor       The {@link Formatting} color applied to normal text segments.
   * @param highlightColor  The {@link Formatting} color applied to substituted argument segments.
   * @param args            The objects or {@link Text} instances to insert into the placeholders.
   * @return                A fully formatted {@link Text} object with colors applied.
   */
  public static Text format(Text template, Formatting textColor, Formatting highlightColor, Object... args) {
    String raw = template.getString();
    String[] parts = raw.split("%s", -1);
    MutableText result = Text.literal("").formatted(textColor);

    for (int i = 0; i < parts.length; i++) {
      result.append(Text.literal(parts[i]).formatted(textColor));
      if (i < args.length) {
        Object arg = args[i];
        if (arg instanceof Text textArg) {
          result.append(textArg.copy().formatted(highlightColor));
        } else {
          result.append(Text.literal(String.valueOf(arg)).formatted(highlightColor));
        }
      }
    }

    return result;
  }

  /**
   * A shorthand version of {@link #format(Text, Formatting, Formatting, Object...)} that uses
   * default colors for normal text and highlights.
   *
   * @param template  The {@link Text} template containing "%s" placeholders.
   * @param args      The objects or {@link Text} instances to insert into placeholders.
   * @return          A formatted {@link Text} using default colors.
   */
  public static Text format(Text template, Object... args) {
    return format(template, DEFAULT_TEXT_COLOR, DEFAULT_HIGHLIGHT_COLOR, args);
  }

  /**
   * Returns a new {@link Text} that executes a command when clicked and shows a hover tooltip,
   * while preserving the original text's formatting (color, bold, italic, etc.).
   *
   * <p>Example usage:
   * <pre>
   * Text msg = Text.literal("Click me").formatted(Formatting.GREEN);
   * Text clickable = PayTpTextBuilder.commandText(msg, Text.literal("Runs /hello"), "/hello");
   * player.sendMessage(clickable);
   * </pre>
   *
   * @param text          The original {@link Text} to copy. Its formatting will be preserved.
   * @param hoverText     The {@link Text} to display when the player hovers over the clickable text.
   * @param clickCommand  The command string to execute when the player clicks the text (e.g., "/hello").
   * @return              A new {@link Text} object with the click and hover events applied.
   */
  public static Text commandText(Text text, Text hoverText, String clickCommand) {
    return text.copy().setStyle(
        text.getStyle()
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
    );
  }

}

