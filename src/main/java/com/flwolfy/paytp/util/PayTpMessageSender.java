package com.flwolfy.paytp.util;

import com.flwolfy.paytp.data.lang.PayTpLangManager;

import java.util.ArrayList;
import java.util.List;
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
    Text msg = Text.empty()
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.teleport"),
            LANG_LOADER.getText("paytp.success")
        ))
        .append(Text.literal("\n"))
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.consume"),
            price,
            currencyItemText
        ));

    player.sendMessage(msg, false);
  }

  public static void msgTpBackSucceeded(
      ServerPlayerEntity player,
      Text currencyItemText,
      int price
  ) {
    Text msg = Text.empty()
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.tp-back"),
            LANG_LOADER.getText("paytp.success")
        ))
        .append(Text.literal("\n"))
        .append(PayTpTextBuilder.format(LANG_LOADER.getText("paytp.consume"),
            price,
            currencyItemText
        ));

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
      String setHomeCommandName
  ) {
    // -------------------
    // Reuse texts
    // -------------------
    String newline = "\n";
    String indentCmd = " ".repeat(4);
    String indentDesc = " ".repeat(8);

    // -------------------
    // Headers
    // -------------------
    MutableText title = LANG_LOADER.getText("paytp.help.title");
    MutableText divider = LANG_LOADER.getText("paytp.help.divider");

    // -------------------
    // Text combinations
    // -------------------
    MutableText msg = Text.empty()
        // Header
        .append(newline)
        .append(divider).append(newline)
        .append(title).append(newline)
        .append(divider).append(newline)
        .append(LANG_LOADER.getText("paytp.help.intro").append(newline).append(newline));

    // [Teleport]
    msg.append(LANG_LOADER.getText("paytp.help.section.tp").append(newline));
    if (!tpCommandName.isEmpty()) {
      msg.append(Text.literal(indentCmd)
          .append(LANG_LOADER.getText("paytp.help.tp.coord")).append(newline)
          .append(Text.literal(indentDesc + "- ")
              .append(LANG_LOADER.getText("paytp.help.tp.coord.desc")).append(newline)));
    }
    if (!backCommandName.isEmpty()) {
      msg.append(Text.literal(indentCmd)
          .append(LANG_LOADER.getText("paytp.help.tp.back")).append(newline)
          .append(Text.literal(indentDesc + "- ")
              .append(LANG_LOADER.getText("paytp.help.tp.back.desc")).append(newline)));
    }

    // [Request]
    msg.append(newline).append(LANG_LOADER.getText("paytp.help.section.req")).append(newline);
    if (!tpPlayerCommandName.isEmpty()) {
      msg.append(Text.literal(indentCmd)
          .append(LANG_LOADER.getText("paytp.help.req.to")).append(newline)
          .append(Text.literal(indentDesc + "- ")
              .append(LANG_LOADER.getText("paytp.help.req.to.desc")).append(newline)));
    }
    if (!tpPlayerHereCommandName.isEmpty()) {
      msg.append(Text.literal(indentCmd)
          .append(LANG_LOADER.getText("paytp.help.req.here")).append(newline)
          .append(Text.literal(indentDesc + "- ")
              .append(LANG_LOADER.getText("paytp.help.req.here.desc")).append(newline)));
    }
    if (!acceptCommandName.isEmpty()) {
      msg.append(Text.literal(indentCmd)
          .append(LANG_LOADER.getText("paytp.help.req.accept")).append(newline)
          .append(Text.literal(indentDesc + "- ")
              .append(LANG_LOADER.getText("paytp.help.req.accept.desc")).append(newline)));
    }
    if (!denyCommandName.isEmpty()) {
      msg.append(Text.literal(indentCmd)
          .append(LANG_LOADER.getText("paytp.help.req.deny")).append(newline)
          .append(Text.literal(indentDesc + "- ")
              .append(LANG_LOADER.getText("paytp.help.req.deny.desc")).append(newline)));
    }
    if (!cancelCommandName.isEmpty()) {
      msg.append(Text.literal(indentCmd)
          .append(LANG_LOADER.getText("paytp.help.req.cancel")).append(newline)
          .append(Text.literal(indentDesc + "- ")
              .append(LANG_LOADER.getText("paytp.help.req.cancel.desc")).append(newline)));
    }

    // [Home]
    msg.append(newline).append(LANG_LOADER.getText("paytp.help.section.home")).append(newline);
    if (!homeCommandName.isEmpty()) {
      msg.append(Text.literal(indentCmd)
          .append(LANG_LOADER.getText("paytp.help.home.goto")).append(newline)
          .append(Text.literal(indentDesc + "- ")
              .append(LANG_LOADER.getText("paytp.help.home.goto.desc")).append(newline)));
    }
    if (!setHomeCommandName.isEmpty()) {
      msg.append(Text.literal(indentCmd)
          .append(LANG_LOADER.getText("paytp.help.home.set")).append(newline)
          .append(Text.literal(indentDesc + "- ")
              .append(LANG_LOADER.getText("paytp.help.home.set.desc")).append(newline)));
    }

    // Footer
    msg.append(newline).append(LANG_LOADER.getText("paytp.help.note")).append(newline)
        .append(divider);

    // -------------------
    // Text formatting
    // -------------------
    List<Text> formattedTexts = new ArrayList<>();

    // ==== /ptp =====
    if (!tpCommandName.isEmpty()) {
      formattedTexts.add(PayTpTextBuilder.suggestCommandText(
          Text.literal("/" + tpCommandName),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + tpCommandName),
          "/" + tpCommandName + " " + player.getServerWorld().getRegistryKey().getValue().toString() + " ~ ~ ~"
      ));
    }

    // ==== /ptpback =====
    if (!backCommandName.isEmpty()) {
      formattedTexts.add(PayTpTextBuilder.suggestCommandText(
          Text.literal("/" + backCommandName),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + backCommandName),
          "/" + backCommandName
      ));
    }

    // ==== /ptpto =====
    if (!tpPlayerCommandName.isEmpty()) {
      formattedTexts.add(PayTpTextBuilder.suggestCommandText(
          Text.literal("/" + tpPlayerCommandName),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + tpPlayerCommandName),
          "/" + tpPlayerCommandName + " " + player.getName().getString()
      ));
    }

    // ==== /ptphere =====
    if (!tpPlayerHereCommandName.isEmpty()) {
      formattedTexts.add(PayTpTextBuilder.suggestCommandText(
          Text.literal("/" + tpPlayerHereCommandName),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + tpPlayerHereCommandName),
          "/" + tpPlayerHereCommandName + " " + player.getName().getString()
      ));
    }

    // ==== /ptpaccpet =====
    if (!acceptCommandName.isEmpty()) {
      formattedTexts.add(PayTpTextBuilder.suggestCommandText(
          Text.literal("/" + acceptCommandName),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + acceptCommandName),
          "/" + acceptCommandName
      ));
    }

    // ==== /ptpdeny =====
    if (!denyCommandName.isEmpty()) {
      formattedTexts.add(PayTpTextBuilder.suggestCommandText(
          Text.literal("/" + denyCommandName),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + denyCommandName),
          "/" + denyCommandName
      ));
    }

    // ==== /ptpcancel =====
    if (!cancelCommandName.isEmpty()) {
      formattedTexts.add(PayTpTextBuilder.suggestCommandText(
          Text.literal("/" + cancelCommandName),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + cancelCommandName),
          "/" + cancelCommandName
      ));
    }

    // ==== /ptphome =====
    if (!homeCommandName.isEmpty()) {
      formattedTexts.add(PayTpTextBuilder.suggestCommandText(
          Text.literal("/" + homeCommandName),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + homeCommandName),
          "/" + homeCommandName
      ));
    }

    // ==== /ptphome set =====
    if (!setHomeCommandName.isEmpty()) {
      formattedTexts.add(PayTpTextBuilder.suggestCommandText(
          Text.literal("/" + setHomeCommandName),
          PayTpTextBuilder.format(LANG_LOADER.getText("paytp.hover.command"), "/" + setHomeCommandName),
          "/" + setHomeCommandName
      ));
    }

    msg = Text.empty().append(PayTpTextBuilder.format(msg, formattedTexts.toArray()));
    player.sendMessage(msg, false);
  }


}
