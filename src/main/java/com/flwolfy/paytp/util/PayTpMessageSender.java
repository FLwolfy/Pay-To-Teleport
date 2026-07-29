package com.flwolfy.paytp.util;

import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.data.lang.PayTpLangManager;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class PayTpMessageSender {

  private static final PayTpLangManager LANG_LOADER = PayTpLangManager.getInstance();

  private PayTpMessageSender() {}

  // ========================================= //
  // ============= Message Sending =========== //
  // ========================================= //

  public static void msgRequesterNotEnough(ServerPlayer player) {
    Component msg = Component.empty()
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.teleport"),
            PayTpTextBuilder.DEFAULT_TEXT_COLOR,
            PayTpTextBuilder.DEFAULT_WARN_COLOR,
            LANG_LOADER.getText("paytp.failed")))
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.requester-not-enough")));

    player.sendSystemMessage(msg);
  }

  public static void msgTpSucceeded(
      ServerPlayer player,
      Component currencyItemText,
      int price
  ) {
    MutableComponent msg = Component.empty()
        .append(PayTpTextBuilder.format(
            LANG_LOADER.getText("paytp.teleport"),
            LANG_LOADER.getText("paytp.success")
        ));

    if (price > 0) {
      msg = msg
          .append(Component.literal("\n"))
          .append(PayTpTextBuilder.format(
              LANG_LOADER.getText("paytp.consume"),
              price,
              currencyItemText
          ));
    }

    player.sendSystemMessage(msg);
  }

  public static void msgTpBackSucceeded(
      ServerPlayer player,
      Component currencyItemText,
      int price
  ) {
    MutableComponent msg = Component.empty()
        .append(PayTpTextBuilder.format(
            LANG_LOADER.getText("paytp.tp-back"),
            LANG_LOADER.getText("paytp.success")
        ));

    if (price > 0) {
      msg = msg
          .append(Component.literal("\n"))
          .append(PayTpTextBuilder.format(
              LANG_LOADER.getText("paytp.consume"),
              price,
              currencyItemText
          ));
    }

    player.sendSystemMessage(msg);
  }

  public static void msgTpFailed(
      ServerPlayer player,
      Component currencyItemText,
      int price,
      int balance
  ) {
    Component msg = Component.empty()
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.teleport"),
            PayTpTextBuilder.DEFAULT_TEXT_COLOR,
            PayTpTextBuilder.DEFAULT_WARN_COLOR,
            LANG_LOADER.getText("paytp.failed")
        ))
        .append(Component.literal("\n"))
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.not-enough"),
            PayTpTextBuilder.DEFAULT_TEXT_COLOR,
            PayTpTextBuilder.DEFAULT_WARN_COLOR,
            currencyItemText,
            price,
            currencyItemText,
            balance
        ));

    player.sendSystemMessage(msg);
  }

  public static void msgPaymentError(ServerPlayer player) {
    Component msg = Component.empty()
        .append(PayTpTextBuilder.format(
            LANG_LOADER.getText("paytp.teleport"),
            PayTpTextBuilder.DEFAULT_TEXT_COLOR,
            PayTpTextBuilder.DEFAULT_WARN_COLOR,
            LANG_LOADER.getText("paytp.failed")
        ))
        .append(Component.literal("\n"))
        .append(LANG_LOADER.getText("paytp.payment-error")
            .copy()
            .withStyle(PayTpTextBuilder.DEFAULT_WARN_COLOR));

    player.sendSystemMessage(msg);
  }

  public static void msgCrossDimensionDisabled(ServerPlayer player) {
    Component msg = Component.empty()
        .append(PayTpTextBuilder.format(
            LANG_LOADER.getText("paytp.teleport"),
            PayTpTextBuilder.DEFAULT_TEXT_COLOR,
            PayTpTextBuilder.DEFAULT_WARN_COLOR,
            LANG_LOADER.getText("paytp.failed")
        ))
        .append(Component.literal("\n"))
        .append(PayTpTextBuilder.format(
            LANG_LOADER.getText("paytp.cross-dimension-disabled"),
            PayTpTextBuilder.DEFAULT_TEXT_COLOR,
            PayTpTextBuilder.DEFAULT_WARN_COLOR
        ));

    player.sendSystemMessage(msg);
  }

  public static void msgTpAccepted(ServerPlayer player, Component senderText) {
    player.sendSystemMessage(PayTpTextBuilder.format(
        LANG_LOADER.getText("paytp.request.accept"),
        senderText
    ));
  }

  public static void msgTpCanceled(ServerPlayer player, Component targetText) {
    player.sendSystemMessage(PayTpTextBuilder.format(
        LANG_LOADER.getText("paytp.request.cancel.sender"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        targetText
    ));
  }

  public static void msgCancelTp(ServerPlayer player, Component senderText) {
    player.sendSystemMessage(PayTpTextBuilder.format(
        LANG_LOADER.getText("paytp.request.cancel.receiver"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        senderText
    ));
  }

  public static void msgTpRequestSent(
      ServerPlayer player,
      Component targetText
  ) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.request"), targetText));
  }

  public static void msgTpRequestReceived(
      ServerPlayer player,
      Component senderText,
      String acceptCommandName,
      String denyCommandName,
      int expireTime,
      boolean here
  ) {
    MutableComponent msg = Component.empty();

    if (here) {
      msg.append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.receive"),
          senderText,
          LANG_LOADER.getText("paytp.receive.here").withStyle(PayTpTextBuilder.DEFAULT_WARN_COLOR),
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
        LANG_LOADER.getText("paytp.accept").withStyle(
            PayTpTextBuilder.DEFAULT_HIGHLIGHT_COLOR
        ),
        PayTpTextBuilder.format(
            LANG_LOADER.getText("paytp.hover.reply"),
            LANG_LOADER.getText("paytp.accept")
        ),
        "/" + acceptCommandName
    ));
    msg.append(PayTpTextBuilder.commandText(
        LANG_LOADER.getText("paytp.deny").withStyle(
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

    player.sendSystemMessage(msg);
  }

  public static void msgSelfTp(ServerPlayer player) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.self-tp")));
  }

  public static void msgNoTargetFound(ServerPlayer player) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-target")));
  }

  public static void msgNoAcceptRequest(ServerPlayer player) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-accept")));
  }

  public static void msgNoDenyRequest(ServerPlayer player) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-deny")));
  }

  public static void msgNoCancelRequest(ServerPlayer player) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-cancel")));
  }

  public static void msgNoBack(ServerPlayer player) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-back")));
  }

  public static void msgNoWarp(ServerPlayer player, String warpName) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-warp"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR
        , warpName
    ));
  }

  public static void msgTpHome(ServerPlayer player) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.tp-home")));
  }

  public static void msgHomeSet(ServerPlayer player) {
    Component msg = Component.empty()
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.set-home"),
            LANG_LOADER.getText("paytp.home")
        ));
    player.sendSystemMessage(msg);
  }

  public static void msgHomeNotSet(ServerPlayer player) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.no-home")));
  }

  public static void msgWarpCreated(ServerPlayer player, ServerPlayer createPlayer, String name) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.create-warp"),
        name,
        createPlayer.getName()
    ));
  }

  public static void msgWarpExist(ServerPlayer player, String name) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.warp-exist"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        name
    ));
  }

  public static void msgWarpCreateFailed(ServerPlayer player, String name) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.create-warp-failed"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        name
    ));
  }

  public static void msgWarpDeleted(ServerPlayer player, ServerPlayer deletePlayer, String name) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.delete-warp"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        name,
        deletePlayer.getName()
    ));
  }

  public static void msgWarpDeletedServer(ServerPlayer player, String name) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.delete-warp-server"),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        PayTpTextBuilder.DEFAULT_WARN_COLOR,
        name,
        LANG_LOADER.getText("paytp.server")
    ));
  }

  public static void msgEmptyWarp(ServerPlayer player) {
    player.sendSystemMessage(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.empty-warp")));
  }

  public static void msgWarpList(
      ServerPlayer player,
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

    MutableComponent msg = Component.empty();
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
          Component.literal(entry.getKey()).withStyle(PayTpTextBuilder.DEFAULT_HIGHLIGHT_COLOR),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.warp"), entry.getKey()),
          "/" + warpCommandName + " " + entry.getKey()
      ));
      msg.append(Component.literal(" "));
      msg.append(Component.literal(entry.getValue().toString()).withStyle(PayTpTextBuilder.DEFAULT_SHADE_COLOR));
    }

    msg.append(newline);
    msg.append(newline);

    MutableComponent pageButtons = Component.empty();

    if (page > 1) {
      pageButtons.append(PayTpTextBuilder.commandText(
          Component.literal("⏪").withStyle(PayTpTextBuilder.DEFAULT_TEXT_COLOR),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.page"), (page - 1)),
          "/" + warpListCommandName + " " + (page - 1)
      ));
    } else {
      pageButtons.append(Component.literal("⏪").withStyle(PayTpTextBuilder.DEFAULT_SHADE_COLOR));
    }

    pageButtons.append(Component.literal(" | ").withStyle(PayTpTextBuilder.DEFAULT_TEXT_COLOR));
    pageButtons.append(Component.literal("[" + page + "]").withStyle(PayTpTextBuilder.DEFAULT_HIGHLIGHT_COLOR));
    pageButtons.append(Component.literal(" / " + totalPages + " | ").withStyle(PayTpTextBuilder.DEFAULT_TEXT_COLOR));

    if (page < totalPages) {
      pageButtons.append(PayTpTextBuilder.commandText(
          Component.literal("⏩").withStyle(PayTpTextBuilder.DEFAULT_TEXT_COLOR),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.page"), (page + 1)),
          "/" + warpListCommandName + " " + (page + 1)
      ));
    } else {
      pageButtons.append(Component.literal("⏩").withStyle(PayTpTextBuilder.DEFAULT_SHADE_COLOR));
    }

    msg.append(pageButtons);
    msg.append(newline);
    msg.append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.help.divider")));

    player.sendSystemMessage(msg);
  }

  public static void msgHelp(
      ServerPlayer player,
      String tpCommandName,
      boolean allowCrossDim,
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
    // Reuse components
    // -------------------
    String newline = "\n";
    String indentCmd = " ".repeat(4);
    String indentDesc = " ".repeat(8);

    // -------------------
    // Header
    // -------------------
    MutableComponent title = LANG_LOADER.getText("paytp.help.title");
    MutableComponent divider = LANG_LOADER.getText("paytp.help.divider");

    // -------------------
    // Msg Holder
    // -------------------
    MutableComponent[] msgHolder = new MutableComponent[]{ Component.empty()
        .append("\n")
        .append(divider).append("\n")
        .append(title).append("\n")
        .append(divider).append("\n")
        .append(LANG_LOADER.getText("paytp.help.intro").append("\n\n"))
    };

    // -------------------
    // Component combinations
    // -------------------
    BiConsumer<String, String> appendCmdText = (key, cmd) -> {
      if (!cmd.isEmpty()) {
        msgHolder[0] = msgHolder[0].append(Component.literal(indentCmd)
            .append(LANG_LOADER.getText(key)).append("\n")
            .append(Component.literal(indentDesc + "- ")
                .append(LANG_LOADER.getText(key + ".desc")).append("\n")));
      }
    };

    BiConsumer<String, Runnable> appendSectionIfNotEmpty = (sectionKey, appendCmds) -> {
      MutableComponent temp = Component.empty();
      MutableComponent oldMsg = msgHolder[0];
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
      appendCmdText.accept(
          allowCrossDim ? "paytp.help.tp.coord.cross-dim" : "paytp.help.tp.coord",
          tpCommandName
      );
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
    msgHolder[0].append(newline).append(divider);

    // -------------------
    // Component formatting
    // -------------------
    List<Component> formattedTexts = new ArrayList<>();

    BiFunction<String, String, Void> suggestIfNotEmpty = (cmd, placeholder) -> {
      if (!cmd.isEmpty()) {
        formattedTexts.add(PayTpTextBuilder.suggestCommandText(
            Component.literal("/" + cmd),
            PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + cmd),
            placeholder
        ));
      }
      return null;
    };

    // Teleport
    String coordinatePlaceholder = "/" + tpCommandName
        + (allowCrossDim ? " (dim)" : "")
        + " <x> <y> <z>";
    suggestIfNotEmpty.apply(tpCommandName, coordinatePlaceholder);
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
    msgHolder[0] = Component.empty().append(PayTpTextBuilder.format(msgHolder[0], formattedTexts.toArray()));
    player.sendSystemMessage(msgHolder[0]);
  }

}
