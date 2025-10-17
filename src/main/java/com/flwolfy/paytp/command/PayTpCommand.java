package com.flwolfy.paytp.command;

import com.flwolfy.paytp.config.PayTpConfig;
import com.flwolfy.paytp.config.PayTpConfigData;
import com.flwolfy.paytp.util.PayTpCalculator;

import com.flwolfy.paytp.util.PayTpItemHandler;
import com.flwolfy.paytp.util.PayTpTextFormatter;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.command.ControlFlowAware.Command;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

public class PayTpCommand {

  private static PayTpConfigData configData;

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    configData = PayTpConfig.getInstance().data();
    dispatcher.register(CommandManager.literal(configData.commandName())
        // Coordinate Tp
        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
            .executes(ctx -> {
              PlayerEntity player = ctx.getSource().getPlayer();

              if (player != null) {
                Vec3d targetPos = Vec3ArgumentType.getVec3(ctx, "pos");
                return payTpCoords(player, targetPos);
              }

              return 0;
            })
        )
    );
  }

  private static int payTpCoords(PlayerEntity player, Vec3d to) {
    int price = PayTpCalculator.calculatePrice(configData.baseRadius(), configData.rate(), configData.minPrice(), configData.maxPrice(), player.getPos(), to);
    int balance = PayTpCalculator.checkBalance(configData.currencyItem(), player, configData.flags());

    MutableText message = Text.empty();
    if (balance >= price) {
      PayTpCalculator.proceedPayment(configData.currencyItem(), player, price, configData.flags());
      player.teleport(to.x, to.y, to.z, true);

      message = message
          .append(PayTpTextFormatter.format("paytp.teleport",
              Text.translatable("paytp.success").getString()
          ))
          .append(Text.literal("\n"))
          .append(PayTpTextFormatter.format("paytp.consume",
              price,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName().getString()
          ));
    } else {
      message = message
          .append(PayTpTextFormatter.format("paytp.teleport", PayTpTextFormatter.DEFAULT_TEXT_COLOR, PayTpTextFormatter.DEFAULT_WARN_COLOR,
              Text.translatable("paytp.failed").getString()
          ))
          .append(Text.literal("\n"))
          .append(PayTpTextFormatter.format("paytp.not-enough", PayTpTextFormatter.DEFAULT_TEXT_COLOR, PayTpTextFormatter.DEFAULT_WARN_COLOR,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName().getString(),
              price,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName().getString(),
              balance
          ));
    }

    player.sendMessage(message, false);
    return balance >= price ? Command.SINGLE_SUCCESS : 0;
  }
}
