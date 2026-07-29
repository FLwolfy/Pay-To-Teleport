package com.flwolfy.paytp.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.ClickEvent.SuggestCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.network.chat.MutableComponent;

public final class PayTpTextBuilder {

  public static final ChatFormatting DEFAULT_TEXT_COLOR = ChatFormatting.YELLOW;
  public static final ChatFormatting DEFAULT_HIGHLIGHT_COLOR = ChatFormatting.GREEN;
  public static final ChatFormatting DEFAULT_WARN_COLOR = ChatFormatting.RED;
  public static final ChatFormatting DEFAULT_SHADE_COLOR = ChatFormatting.DARK_GRAY;

  private PayTpTextBuilder() {}

  // ========================================= //
  // ========== Component Formatting ========= //
  // ========================================= //

  /**
   * Formats a {@link Component} template with color highlighting for inserted arguments.
   * <p>
   * This method works similarly to {@link String#format(String, Object...)}, but supports
   * Minecraft's {@link Component} API and {@link ChatFormatting} colors. Each occurrence of
   * <code>%s</code> in the input component will be replaced with the corresponding argument from {@code args}, and
   * highlighted with a specified color.
   * </p>
   *
   * Example:
   * <pre>
   *   format(
   *       Component.literal("Hello, %s!"),
   *       ChatFormatting.WHITE,
   *       ChatFormatting.GOLD,
   *       "Steve"
   *   );
   *   // => "Hello, " (white) + "Steve" (gold) + "!" (white)
   * </pre>
   *
   * @param template        The base {@link Component} template containing "%s" placeholders.
   * @param textColor       The {@link ChatFormatting} color applied to normal component segments.
   * @param highlightColor  The {@link ChatFormatting} color applied to substituted argument segments.
   * @param args            The objects or {@link Component} instances to insert into the placeholders.
   * @return                A fully formatted {@link Component} object with colors applied.
   */
  public static Component format(Component template, ChatFormatting textColor, ChatFormatting highlightColor, Object... args) {
    String raw = template.getString();
    String[] parts = raw.split("%s", -1);
    MutableComponent result = Component.literal("").withStyle(textColor);

    for (int i = 0; i < parts.length; i++) {
      result.append(Component.literal(parts[i]).withStyle(textColor));
      if (i < args.length) {
        Object arg = args[i];
        if (arg instanceof Component textArg) {
          result.append(textArg.copy().withStyle(highlightColor));
        } else {
          result.append(Component.literal(String.valueOf(arg)).withStyle(highlightColor));
        }
      }
    }

    return result;
  }

  /**
   * A shorthand version of {@link #format(Component, ChatFormatting, ChatFormatting, Object...)} that uses
   * default colors for normal component segments and highlights.
   *
   * @param template  The {@link Component} template containing "%s" placeholders.
   * @param args      The objects or {@link Component} instances to insert into placeholders.
   * @return          A formatted {@link Component} using default colors.
   */
  public static Component format(Component template, Object... args) {
    return format(template, DEFAULT_TEXT_COLOR, DEFAULT_HIGHLIGHT_COLOR, args);
  }

  /**
   * Returns a new {@link Component} that executes a command when clicked and shows a hover tooltip,
   * while preserving the original component's style (color, bold, italic, etc.).
   *
   * <p>Example usage:
   * <pre>
   * Component message = Component.literal("Click me").withStyle(ChatFormatting.GREEN);
   * Component clickable = PayTpTextBuilder.commandText(
   *     message,
   *     Component.literal("Runs /hello"),
   *     "/hello"
   * );
   * player.sendSystemMessage(clickable);
   * </pre>
   *
   * @param text          The original {@link Component} to copy. Its style will be preserved.
   * @param hoverText     The {@link Component} to display when the player hovers over the clickable component.
   * @param clickCommand  The command string to execute when the player clicks the component (e.g., "/hello").
   * @return              A new {@link Component} object with the click and hover events applied.
   */
  public static Component commandText(Component text, Component hoverText, String clickCommand) {
    return text.copy().setStyle(
        text.getStyle()
            .withClickEvent(new RunCommand(clickCommand))
            .withHoverEvent(new ShowText(hoverText))
    );
  }

  /**
   * Returns a new {@link Component} that suggests a command in the player's chat box when clicked
   * and shows a hover tooltip, while preserving the original component's style (color, bold, italic, etc.).
   *
   * <p>Unlike RUN_COMMAND, this does not execute the command immediately. Instead,
   * the command is placed in the chat input field, and the player can review or edit it
   * before pressing Enter to execute it.
   *
   * <p>Example usage:
   * <pre>
   * Component message = Component.literal("Click me").withStyle(ChatFormatting.GREEN);
   * Component clickable = PayTpTextBuilder.suggestCommandText(
   *     message,
   *     Component.literal("Suggests /hello"),
   *     "/hello"
   * );
   * player.sendSystemMessage(clickable);
   * </pre>
   *
   * @param text          The original {@link Component} to copy. Its style will be preserved.
   * @param hoverText     The {@link Component} to display when the player hovers over the clickable component.
   * @param clickCommand  The command string to suggest in the chat input (e.g., "/hello").
   * @return              A new {@link Component} object with the click suggestion and hover events applied.
   */
  public static Component suggestCommandText(Component text, Component hoverText, String clickCommand) {
    return text.copy().setStyle(
        text.getStyle()
            .withClickEvent(new SuggestCommand(clickCommand))
            .withHoverEvent(new ShowText(hoverText))
    );
  }
}
