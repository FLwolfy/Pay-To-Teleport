package com.flwolfy.paytp.command.warp;

import com.flwolfy.paytp.PayTpMod;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.List;

/**
 * A warp-name argument that accepts unquoted Unicode text up to the next space.
 * Names containing spaces may still be surrounded with quotes.
 */
public final class PayTpWarpNameArgument implements ArgumentType<String> {

  private static final SimpleCommandExceptionType EMPTY_NAME = new SimpleCommandExceptionType(new LiteralMessage("Expected waypoint name"));
  private static final Collection<String> EXAMPLES = List.of("spawn", "主城", "\"主城 广场\"");

  private PayTpWarpNameArgument() {}

  public static void register() {
    ArgumentTypeRegistry.registerArgumentType(
        Identifier.fromNamespaceAndPath(PayTpMod.MOD_ID, "warp_name"),
        PayTpWarpNameArgument.class,
        SingletonArgumentInfo.contextFree(PayTpWarpNameArgument::warpName)
    );
  }

  public static PayTpWarpNameArgument warpName() {
    return new PayTpWarpNameArgument();
  }

  @Override
  public String parse(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead()) {
      throw EMPTY_NAME.createWithContext(reader);
    }
    if (StringReader.isQuotedStringStart(reader.peek())) {
      String result = reader.readQuotedString();
      if (result.isEmpty()) {
        throw EMPTY_NAME.createWithContext(reader);
      }
      return result;
    }

    int start = reader.getCursor();
    while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
      reader.skip();
    }
    if (reader.getCursor() == start) {
      throw EMPTY_NAME.createWithContext(reader);
    }
    return reader.getString().substring(start, reader.getCursor());
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
