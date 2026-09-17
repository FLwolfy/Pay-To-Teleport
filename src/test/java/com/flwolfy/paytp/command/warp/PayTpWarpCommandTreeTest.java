package com.flwolfy.paytp.command.warp;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import net.minecraft.commands.SharedSuggestionProvider;
import org.junit.jupiter.api.Test;

class PayTpWarpCommandTreeTest {

  @Test
  void teleportLiteralIsRequiredBeforeTheWaypointName() throws Exception {
    CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
    dispatcher.register(literal("ptpwarp")
        .executes(context -> 0)
        .then(literal("teleport")
            .then(argument("input", greedyString()).executes(context -> 4))));

    assertEquals(4, dispatcher.execute("ptpwarp teleport 樱花谷", new Object()));
    assertThrows(
        CommandSyntaxException.class,
        () -> dispatcher.execute("ptpwarp 樱花谷", new Object())
    );
  }

  @Test
  void deleteUsesOneCanonicalInputWithOptionalForcedSuffix() throws Exception {
    CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
    dispatcher.register(literal("delete")
        .then(argument("input", greedyString()).executes(context ->
            PayTpWarpCommandInput.delete(getString(context, "input")).forced() ? 5 : 1
        )));

    assertEquals(1, dispatcher.execute("delete \"樱花谷\"", new Object()));
    assertEquals(5, dispatcher.execute("delete \"樱花谷\" forced", new Object()));
    assertEquals(1, dispatcher.getRoot().getChild("delete").getChildren().size());
  }

  @Test
  void typeFirstCreateHasNoLegacyCatchAllBranch() throws Exception {
    CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
    dispatcher.register(literal("create")
        .then(literal("private")
            .then(argument("name", greedyString()).executes(context -> 1)))
        .then(literal("public")
            .then(argument("name", greedyString()).executes(context -> 2)))
        .then(literal("server")
            .then(argument("name", greedyString()).executes(context -> 3))));

    assertEquals(3, dispatcher.execute("create server 樱花谷", new Object()));
    assertThrows(
        CommandSyntaxException.class,
        () -> dispatcher.execute("create 樱花谷 server", new Object())
    );
  }

  @Test
  void anEmptyNameArgumentListsFullQuotedChoices() {
    var suggestions = SharedSuggestionProvider.suggest(
        List.of("\"樱花谷\"", "\"主城\""),
        new SuggestionsBuilder("", 0)
    ).join().getList();

    assertEquals(List.of("\"主城\"", "\"樱花谷\""), suggestions.stream()
        .map(suggestion -> suggestion.getText())
        .toList());
  }
}
