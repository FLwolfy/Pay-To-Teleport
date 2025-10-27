package com.flwolfy.paytp.util;

import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.data.lang.PayTpLangManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class PayTpMessageSender {

  private static final PayTpLangManager LANG_LOADER = PayTpLangManager.getInstance();

  private PayTpMessageSender() {}

  // ========================================= //
  // ============= Message Sending =========== //
  // ========================================= //

  public static void msgRequesterNotEnough(ServerPlayerEntity player) {
    Text msg = Text.empty()
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.teleport"),
            PayTpTextBuilder.DEFAULT_TEXT_COLOR,
            PayTpTextBuilder.DEFAULT_WARN_COLOR,
            LANG_LOADER.getText("paytp.failed")))
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.requester-not-enough")));

    player.sendMessage(msg, false);
  }

  public static void msgTpSucceeded(
      ServerPlayerEntity player,
      Text currencyItemText,
      int price
  ) {
    MutableText msg = Text.empty()
        .append(PayTpTextBuilder.format(
            LANG_LOADER.getText("paytp.teleport"),
            LANG_LOADER.getText("paytp.success")
        ));

    if (price > 0) {
      msg = msg
          .append(Text.literal("\n"))
          .append(PayTpTextBuilder.format(
              LANG_LOADER.getText("paytp.consume"),
              price,
              currencyItemText
          ));
    }

    player.sendMessage(msg, false);
  }

  public static void msgTpBackSucceeded(
      ServerPlayerEntity player,
      Text currencyItemText,
      int price
  ) {
    MutableText msg = Text.empty()
        .append(PayTpTextBuilder.format(
            LANG_LOADER.getText("paytp.tp-back"),
            LANG_LOADER.getText("paytp.success")
        ));

    if (price > 0) {
      msg = msg
          .append(Text.literal("\n"))
          .append(PayTpTextBuilder.format(
              LANG_LOADER.getText("paytp.consume"),
              price,
              currencyItemText
          ));
    }

    player.sendMessage(msg, false);
  }

  public static void msgTpFailed(
      ServerPlayerEntity player,
      Text currencyItemText,
      int price,
      int balance
  ) {
    Text msg = Text.empty()
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.teleport"),
            PayTpTextBuilder.DEFAULT_TEXT_COLOR,
            PayTpTextBuilder.DEFAULT_WARN_COLOR,
            LANG_LOADER.getText("paytp.failed")
        ))
        .append(Text.literal("\n"))
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.not-enough"),
            PayTpTextBuilder.DEFAULT_TEXT_COLOR,
            PayTpTextBuilder.DEFAULT_WARN_COLOR,
            currencyItemText,
            price,
            currencyItemText,
            balance
        ));

    player.sendMessage(msg, false);
  }

  public static void msgTpAccepted(ServerPlayerEntity player, Text senderText) {
    player.sendMessage(PayTpTextBuilder.format(
        LANG_LOADER.getText("paytp.request.accept"),
        senderText
    ), false);
  }

  public static void msgTpCanceled(ServerPlayerEntity player, Text targetText) {
    player.sendMessage(PayTpTextBuilder.format(
        LANG_LOADER.getText("paytp.request.cancel.sender"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        targetText
    ), false);
  }

  public static void msgCancelTp(ServerPlayerEntity player, Text senderText) {
    player.sendMessage(PayTpTextBuilder.format(
        LANG_LOADER.getText("paytp.request.cancel.receiver"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        senderText
    ), false);
  }

  public static void msgTpRequestSent(
      ServerPlayerEntity player,
      Text targetText
  ) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.request"), targetText), false);
  }

  public static void msgTpRequestReceived(
      ServerPlayerEntity player,
      Text senderText,
      String acceptCommandName,
      String denyCommandName,
      int expireTime,
      boolean here
  ) {
    MutableText msg = Text.empty();

    if (here) {
      msg.append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.receive"),
          senderText,
          LANG_LOADER.getText("paytp.receive.here").formatted(PayTpTextBuilder.DEFAULT_WARN_COLOR),
          expireTime
      ));
    } else {
      msg.append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.receive"),
          senderText,
          LANG_LOADER.getText("paytp.receive.to"),
          expireTime
      ));
    }

    msg.append(PayTpTextBuilder.commandText(
        LANG_LOADER.getText("paytp.accept").formatted(
            PayTpTextBuilder.DEFAULT_HIGHLIGHT_COLOR
        ),
        PayTpTextBuilder.format(
            LANG_LOADER.getText("paytp.hover.reply"),
            LANG_LOADER.getText("paytp.accept")
        ),
        "/" + acceptCommandName
    ));
    msg.append(PayTpTextBuilder.commandText(
        LANG_LOADER.getText("paytp.deny").formatted(
            PayTpTextBuilder.DEFAULT_WARN_COLOR
        ),
        PayTpTextBuilder.format(
            LANG_LOADER.getText("paytp.hover.reply"),
            PayTpTextBuilder.DEFAULT_TEXT_COLOR,
            PayTpTextBuilder.DEFAULT_WARN_COLOR,
            LANG_LOADER.getText("paytp.deny")
        ),
        "/" + denyCommandName
    ));

    player.sendMessage(msg, false);
  }

  public static void msgSelfTp(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.self-tp")), false);
  }

  public static void msgNoTargetFound(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-target")), false);
  }

  public static void msgNoAcceptRequest(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-accept")), false);
  }

  public static void msgNoDenyRequest(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-deny")), false);
  }

  public static void msgNoCancelRequest(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-cancel")), false);
  }

  public static void msgNoBack(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-back")), false);
  }

  public static void msgNoWarp(ServerPlayerEntity player, String warpName) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-warp"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR
        , warpName
    ), false);
  }

  public static void msgTpHome(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.tp-home")), false);
  }

  public static void msgHomeSet(ServerPlayerEntity player) {
    Text msg = Text.empty()
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.set-home"),
            LANG_LOADER.getText("paytp.home")
        ));
    player.sendMessage(msg, false);
  }

  public static void msgHomeNotSet(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-home")), false);
  }

  public static void msgWarpCreated(ServerPlayerEntity player, ServerPlayerEntity createPlayer, String name) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.create-warp"),
        name,
        createPlayer.getName()
    ), false);
  }

  public static void msgWarpExist(ServerPlayerEntity player, String name) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.warp-exist"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        name
    ), false);
  }

  public static void msgWarpCreateFailed(ServerPlayerEntity player, String name) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.create-warp-failed"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        name
    ), false);
  }

  public static void msgWarpDeleted(ServerPlayerEntity player, ServerPlayerEntity deletePlayer, String name) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.delete-warp"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        name,
        deletePlayer.getName()
    ), false);
  }

  public static void msgWarpDeletedServer(ServerPlayerEntity player, String name) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.delete-warp-server"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        name,
        LANG_LOADER.getText("paytp.server")
    ), false);
  }

  public static void msgEmptyWarp(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.empty-warp")), false);
  }

  public static void msgWarpList(
      ServerPlayerEntity player,
      Map<String, PayTpData> warpList,
      String warpCommandName,
      String warpListCommandName,
      int page
  ) {
    final int PAGE_SIZE = 8;

    String newline = "\n";
    List<Map.Entry<String, PayTpData>> entries = new ArrayList<>(warpList.entrySet());

    int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
    page = Math.max(1, Math.min(page, totalPages));

    MutableText msg = Text.empty();
    msg.append(newline);
    msg.append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.help.divider")));
    msg.append(newline);
    msg.append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.warp-list")));

    int start = (page - 1) * PAGE_SIZE;
    int end = Math.min(start + PAGE_SIZE, entries.size());

    for (int i = start; i < end; i++) {
      Map.Entry<String, PayTpData> entry = entries.get(i);
      msg.append(newline);
      msg.append(PayTpTextBuilder.commandText(
          Text.literal(entry.getKey()).formatted(PayTpTextBuilder.DEFAULT_HIGHLIGHT_COLOR),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.warp"), entry.getKey()),
          "/" + warpCommandName + " " + entry.getKey()
      ));
      msg.append(Text.literal(" "));
      msg.append(Text.literal(entry.getValue().toString()).formatted(PayTpTextBuilder.DEFAULT_SHADE_COLOR));
    }

    msg.append(newline);
    msg.append(newline);

    MutableText pageButtons = Text.empty();

    if (page > 1) {
      pageButtons.append(PayTpTextBuilder.commandText(
          Text.literal("⏪").formatted(PayTpTextBuilder.DEFAULT_TEXT_COLOR),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.page"), (page - 1)),
          "/" + warpListCommandName + " " + (page - 1)
      ));
    } else {
      pageButtons.append(Text.literal("⏪").formatted(PayTpTextBuilder.DEFAULT_SHADE_COLOR));
    }

    pageButtons.append(Text.literal(" | ").formatted(PayTpTextBuilder.DEFAULT_TEXT_COLOR));
    pageButtons.append(Text.literal("[" + page + "]").formatted(PayTpTextBuilder.DEFAULT_HIGHLIGHT_COLOR));
    pageButtons.append(Text.literal(" / " + totalPages + " | ").formatted(PayTpTextBuilder.DEFAULT_TEXT_COLOR));

    if (page < totalPages) {
      pageButtons.append(PayTpTextBuilder.commandText(
          Text.literal("⏩").formatted(PayTpTextBuilder.DEFAULT_TEXT_COLOR),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.page"), (page + 1)),
          "/" + warpListCommandName + " " + (page + 1)
      ));
    } else {
      pageButtons.append(Text.literal("⏩").formatted(PayTpTextBuilder.DEFAULT_SHADE_COLOR));
    }

    msg.append(pageButtons);
    msg.append(newline);
    msg.append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.help.divider")));

    player.sendMessage(msg, false);
  }

  public static void msgHelp(
      ServerPlayerEntity player,
      String tpCommandName,
      String backCommandName,
      String tpPlayerCommandName,
      String tpPlayerHereCommandName,
      String acceptCommandName,
      String denyCommandName,
      String cancelCommandName,
      String homeCommandName,
      String setHomeCommandName,
      String warpCommandName,
      String warpCreateCommandName,
      String warpDeleteCommandName,
      String warpListCommandName
  ) {
    // -------------------
    // Reuse texts
    // -------------------
    String newline = "\n";
    String indentCmd = " ".repeat(4);
    String indentDesc = " ".repeat(8);

    // -------------------
    // Header
    // -------------------
    MutableText title = LANG_LOADER.getText("paytp.help.title");
    MutableText divider = LANG_LOADER.getText("paytp.help.divider");

    // -------------------
    // Msg Holder
    // -------------------
    MutableText[] msgHolder = new MutableText[]{ Text.empty()
        .append("\n")
        .append(divider).append("\n")
        .append(title).append("\n")
        .append(divider).append("\n")
        .append(LANG_LOADER.getText("paytp.help.intro").append("\n\n"))
    };

    // -------------------
    // Text combinations
    // -------------------
    BiConsumer<String, String> appendCmdText = (key, cmd) -> {
      if (!cmd.isEmpty()) {
        msgHolder[0] = msgHolder[0].append(Text.literal(indentCmd)
            .append(LANG_LOADER.getText(key)).append("\n")
            .append(Text.literal(indentDesc + "- ")
                .append(LANG_LOADER.getText(key + ".desc")).append("\n")));
      }
    };

    BiConsumer<String, Runnable> appendSectionIfNotEmpty = (sectionKey, appendCmds) -> {
      MutableText temp = Text.empty();
      MutableText oldMsg = msgHolder[0];
      msgHolder[0] = temp;
      appendCmds.run();
      if (!msgHolder[0].getString().isEmpty()) {
        oldMsg = oldMsg.append(newline)
            .append(LANG_LOADER.getText(sectionKey)).append(newline)
            .append(msgHolder[0]);
      }
      msgHolder[0] = oldMsg;
    };

    // Teleport
    appendSectionIfNotEmpty.accept("paytp.help.section.tp", () -> {
      appendCmdText.accept("paytp.help.tp.coord", tpCommandName);
      appendCmdText.accept("paytp.help.tp.back", backCommandName);
    });

    // Request
    appendSectionIfNotEmpty.accept("paytp.help.section.req", () -> {
      appendCmdText.accept("paytp.help.req.to", tpPlayerCommandName);
      appendCmdText.accept("paytp.help.req.here", tpPlayerHereCommandName);
      appendCmdText.accept("paytp.help.req.accept", acceptCommandName);
      appendCmdText.accept("paytp.help.req.deny", denyCommandName);
      appendCmdText.accept("paytp.help.req.cancel", cancelCommandName);
    });

    // Home
    appendSectionIfNotEmpty.accept("paytp.help.section.home", () -> {
      appendCmdText.accept("paytp.help.home.goto", homeCommandName);
      appendCmdText.accept("paytp.help.home.set", setHomeCommandName);
    });

    // Warp
    appendSectionIfNotEmpty.accept("paytp.help.section.warp", () -> {
      appendCmdText.accept("paytp.help.warp.goto", warpCommandName);
      appendCmdText.accept("paytp.help.warp.create", warpCreateCommandName);
      appendCmdText.accept("paytp.help.warp.delete", warpDeleteCommandName);
      appendCmdText.accept("paytp.help.warp.list", warpListCommandName);
    });

    // -------------------
    // Footer
    // -------------------
    msgHolder[0].append(newline).append(LANG_LOADER.getText("paytp.help.note")).append(newline)
        .append(divider);

    // -------------------
    // Text formatting
    // -------------------
    List<Text> formattedTexts = new ArrayList<>();

    BiFunction<String, String, Void> suggestIfNotEmpty = (cmd, placeholder) -> {
      if (!cmd.isEmpty()) {
        formattedTexts.add(PayTpTextBuilder.suggestCommandText(
            Text.literal("/" + cmd),
            PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + cmd),
            placeholder
        ));
      }
      return null;
    };

    // Teleport
    suggestIfNotEmpty.apply(tpCommandName, "/" + tpCommandName + " (dim) <x> <y> <z>");
    suggestIfNotEmpty.apply(backCommandName, "/" + backCommandName);

    // Request
    suggestIfNotEmpty.apply(tpPlayerCommandName, "/" + tpPlayerCommandName + " <player>");
    suggestIfNotEmpty.apply(tpPlayerHereCommandName, "/" + tpPlayerHereCommandName + " <player>");
    suggestIfNotEmpty.apply(acceptCommandName, "/" + acceptCommandName + " (player)");
    suggestIfNotEmpty.apply(denyCommandName, "/" + denyCommandName + " (player)");
    suggestIfNotEmpty.apply(cancelCommandName, "/" + cancelCommandName + " (player)");

    // Home
    suggestIfNotEmpty.apply(homeCommandName, "/" + homeCommandName);
    suggestIfNotEmpty.apply(setHomeCommandName, "/" + setHomeCommandName);

    // Warp
    suggestIfNotEmpty.apply(warpCommandName, "/" + warpCommandName + " <name>");
    suggestIfNotEmpty.apply(warpCreateCommandName, "/" + warpCreateCommandName + " <name>");
    suggestIfNotEmpty.apply(warpDeleteCommandName, "/" + warpDeleteCommandName + " <name>");
    suggestIfNotEmpty.apply(warpListCommandName, "/" + warpListCommandName + " (page)");

    // -------------------
    // Msg Send
    // -------------------
    msgHolder[0] = Text.empty().append(PayTpTextBuilder.format(msgHolder[0], formattedTexts.toArray()));
    player.sendMessage(msgHolder[0], false);
  }

}
