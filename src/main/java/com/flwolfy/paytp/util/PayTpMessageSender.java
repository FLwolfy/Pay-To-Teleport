package com.flwolfy.paytp.util;

import com.flwolfy.paytp.config.PayTpLangManager;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class PayTpMessageSender {

  private static final PayTpLangManager LANG_LOADER = PayTpLangManager.getInstance();

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
      Text currencyItemText,
      int price
  ) {
    Text msg = Text.empty()
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.teleport"),
            LANG_LOADER.getText("paytp.success")
        ))
        .append(Text.literal("\n"))
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.consume"),
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
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.teleport"),
            PayTpTextFormatter.DEFAULT_TEXT_COLOR,
            PayTpTextFormatter.DEFAULT_WARN_COLOR,
            LANG_LOADER.getText("paytp.failed")
        ))
        .append(Text.literal("\n"))
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.not-enough"),
            PayTpTextFormatter.DEFAULT_TEXT_COLOR,
            PayTpTextFormatter.DEFAULT_WARN_COLOR,
            currencyItemText,
            price,
            currencyItemText,
            balance
        ));

    player.sendMessage(msg, false);
  }

  public static void msgTpCanceled(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.cancel")), false);
  }

  public static void msgTpRequestSent(
      ServerPlayerEntity player,
      Text targetText
  ) {
    player.sendMessage(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.request"), targetText), false);
  }

  public static void msgTpRequestReceived(
      ServerPlayerEntity player,
      Text senderText,
      String acceptCommandName,
      String denyCommandName,
      int expireTime
  ) {
    MutableText msg = (MutableText) PayTpTextFormatter.format(LANG_LOADER.getText("paytp.receive"),
        senderText,
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

  public static void msgTpHome(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.tp-home")), false);
  }

  public static void msgHomeSet(ServerPlayerEntity player) {
    Text msg = Text.empty()
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.set-home"),
            LANG_LOADER.getText("paytp.home")
        ));
    player.sendMessage(msg, false);
  }

  public static void msgHomeNotSet(ServerPlayerEntity player) {
    player.sendMessage(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.no-home")), false);
  }

  public static void msgHelp(
      ServerPlayerEntity player,
      String tpCommandName,
      String tpDimCommandName,
      String tpPlayerCommandName,
      String acceptCommandName,
      String denyCommandName,
      String homeCommandName,
      String setHomeCommandName
  ) {
    Text msg = Text.empty()
        .append(PayTpTextFormatter.format(LANG_LOADER.getText("paytp.help"),
            Text.literal("/"+tpCommandName).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + tpCommandName + " ~ ~ ~"))),
            Text.literal("/"+tpDimCommandName).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + tpDimCommandName + " " + player.getServerWorld().getRegistryKey().getValue().toString() + " ~ ~ ~"))),
            Text.literal("/"+tpPlayerCommandName).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + tpPlayerCommandName + " " + player.getName().getString()))),
            Text.literal("/"+acceptCommandName).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + acceptCommandName))),
            Text.literal("/"+denyCommandName).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + denyCommandName))),
            Text.literal("/"+homeCommandName).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + homeCommandName))),
            Text.literal("/"+setHomeCommandName).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + setHomeCommandName)))
        ));
    player.sendMessage(msg, false);
  }

}
