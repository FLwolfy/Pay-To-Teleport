package com.flwolfy.paytp.util;

import com.flwolfy.paytp.config.PayTpLang;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class PayTpMessageHandler {

  private static final PayTpLang LANG_LOADER = PayTpLang.getInstance();

  /**
   * Change the message language to the specified language if supported.
   */
  public static void changeLanguage(String lang) {
    LANG_LOADER.setLanguage(lang);
  }

  // ========================================= //
  // ============= Message Sending =========== //
  // ========================================= //

  public static void msgRequesterNotEnough(ServerPlayerEntity player) {
    Text msg = Text.empty()
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.teleport"),
            PayTpTextFormatter.DEFAULT_TEXT_COLOR,
            PayTpTextFormatter.DEFAULT_WARN_COLOR,
            LANG_LOADER.getText("paytp.failed")))
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.requester-not-enough")));

    player.sendMessage(msg, false);
  }

  public static void msgTpSucceeded(
      ServerPlayerEntity player,
      String currencyName,
      int price
  ) {
    Text msg = Text.empty()
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.teleport"),
            LANG_LOADER.getText("paytp.success")
        ))
        .append(Text.literal("\n"))
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.consume"),
            price,
            currencyName
        ));

    player.sendMessage(msg, false);
  }

  public static void msgTpFailed(
      ServerPlayerEntity player,
      String currencyName,
      int price,
      int balance
  ) {
    Text msg = Text.empty()
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.teleport"), PayTpTextFormatter.DEFAULT_TEXT_COLOR, PayTpTextFormatter.DEFAULT_WARN_COLOR,
            LANG_LOADER.getText("paytp.failed")
        ))
        .append(Text.literal("\n"))
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.not-enough"), PayTpTextFormatter.DEFAULT_TEXT_COLOR, PayTpTextFormatter.DEFAULT_WARN_COLOR,
            currencyName,
            price,
            currencyName,
            balance
        ));

    player.sendMessage(msg, false);
  }

  public static void msgTpCanceled(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.cancel")), false);
  }

  public static void msgTpRequestSent(
      ServerPlayerEntity player,
      String targetName
  ) {
    player.sendMessage(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.request"), targetName), false);
  }

  public static void msgTpRequestReceived(
      ServerPlayerEntity player,
      String senderName,
      String acceptCommandName,
      String denyCommandName,
      int expireTime
  ) {
    MutableText msg = (MutableText) PayTpTextFormatter.format(LANG_LOADER.getText("paytp.receive"),
        senderName,
        expireTime
    );

    msg.append(LANG_LOADER.getText("paytp.accept").setStyle(
        Style.EMPTY.withColor(PayTpTextFormatter.DEFAULT_HIGHLIGHT_COLOR)
            .withClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/" + acceptCommandName
            ))
            .withHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                PayTpTextFormatter.format(LANG_LOADER.getText("paytp.hover"),
                    LANG_LOADER.getText("paytp.accept")
                )
            ))
    ));
    msg.append(LANG_LOADER.getText("paytp.deny").setStyle(
        Style.EMPTY.withColor(PayTpTextFormatter.DEFAULT_WARN_COLOR)
            .withClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/" + denyCommandName
            ))
            .withHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                PayTpTextFormatter.format(LANG_LOADER.getText("paytp.hover"),
                    PayTpTextFormatter.DEFAULT_TEXT_COLOR,
                    PayTpTextFormatter.DEFAULT_WARN_COLOR,
                    LANG_LOADER.getText("paytp.deny")
                )
            ))
    ));

    player.sendMessage(msg, false);
  }

  public static void msgNoTargetFound(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.no-target")), false);
  }

  public static void msgNoAcceptRequest(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.no-accept")), false);
  }

  public static void msgNoCancelRequest(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.no-cancel")), false);
  }
}
