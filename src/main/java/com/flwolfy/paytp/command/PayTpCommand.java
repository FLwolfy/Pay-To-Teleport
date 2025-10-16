package com.flwolfy.paytp.command;

import com.flwolfy.paytp.config.PayTpConfig;
import com.flwolfy.paytp.config.PayTpConfigData;
import com.flwolfy.paytp.util.PayTpCalculator;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.command.ControlFlowAware.Command;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
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
    boolean paymentCheck = PayTpCalculator.proceedPayment(configData.currencyItem(), player, price, configData.allowEnderChest(), configData.prioritizeEnderChest());

    if (paymentCheck) {
      player.teleport(to.x, to.y, to.z, true);
      Text message = Text.literal("Teleport").formatted(Formatting.YELLOW)
          .append(Text.literal("Successfully").formatted(Formatting.GREEN));
      player.sendMessage(message, false);
    } else {
      Text message = Text.literal("Teleport").formatted(Formatting.YELLOW)
          .append(Text.literal("Failed").formatted(Formatting.RED));
      player.sendMessage(message, false);
    }

    return paymentCheck ? Command.SINGLE_SUCCESS : 0;
  }
}
