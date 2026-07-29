package com.flwolfy.paytp.util;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.data.PayTpTeleportType;
import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.data.script.PayTpScript;
import com.flwolfy.paytp.data.script.PayTpScriptManager;
import com.flwolfy.paytp.data.script.PayTpScriptPosition;
import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpPriceDeductionFlags;

import java.util.Map;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;

/**
 * Calculates teleport prices and manages currency balance and payment operations.
 */
public final class PayTpCalculator {

  private static final Logger LOGGER = PayTpMod.LOGGER;

  private PayTpCalculator() {}

  /**
   * Evaluates and clamps the configured price algorithm for one teleport.
   *
   * <p>A zero maximum price bypasses script execution.</p>
   *
   * @param from the player's current location
   * @param to the teleport destination
   * @param teleportType the operation that initiated the teleport
   * @param player the player being teleported and the Minecraft command source
   * @param otherPlayer the other request participant, or an empty string
   * @param priceConfig the price range, currency, and algorithm configuration
   * @return a negative value when payment must be canceled; otherwise the final price within the
   *     configured inclusive range
   */
  public static int calculatePrice(
      PayTpData from,
      PayTpData to,
      PayTpTeleportType teleportType,
      ServerPlayer player,
      String otherPlayer,
      PayTpConfigData.Price priceConfig
  ) {
    int minPrice = priceConfig.minPrice();
    int maxPrice = priceConfig.maxPrice();

    if (maxPrice == 0) return 0;

    try {
      int price = evaluatePrice(
          priceConfig.algorithm(),
          from,
          to,
          teleportType,
          player,
          otherPlayer
      );
      if (price < 0) return price;
      return Math.clamp(price, minPrice, maxPrice);
    } catch (Exception e) {
      LOGGER.error("PayTp price algorithm failed; canceling payment", e);
      return -1;
    }
  }

  private static int evaluatePrice(
      PayTpScript algorithm,
      PayTpData from,
      PayTpData to,
      PayTpTeleportType teleportType,
      ServerPlayer player,
      String otherPlayer
  ) {
    return PayTpScriptManager.getInstance().evaluate(
        algorithm,
        Integer.class,
        Map.of(
            "minecraft",
            new PayTpMinecraftCommandExecutor(player.createCommandSourceStack())
        ),
        Map.entry("from", PayTpScriptPosition.from(from)),
        Map.entry("to", PayTpScriptPosition.from(to)),
        Map.entry("teleportType", teleportType.toString()),
        Map.entry("player", player.getName().getString()),
        Map.entry("otherPlayer", otherPlayer)
    );
  }

  /**
   * Counts all available currency permitted by the configured storage flags.
   *
   * @param currencyItemFullId the namespaced currency item identifier
   * @param player the player whose accessible storage is inspected
   * @param settingFlags combined {@link PayTpPriceDeductionFlags} values
   * @return the total number of matching currency items
   */
  public static int checkBalance(
      String currencyItemFullId,
      Player player,
      int settingFlags
  ) {
    Item currencyItem = PayTpItemHandler.getItemByStringId(currencyItemFullId);

    int totalCount = PayTpItemHandler.getInventoryCount(player.getInventory(), currencyItem, Flags.check(settingFlags, PayTpPriceDeductionFlags.ALLOW_SHULKER_BOX));
    if (Flags.check(settingFlags, PayTpPriceDeductionFlags.ALLOW_ENDER_CHEST)) {
      totalCount += PayTpItemHandler.getInventoryCount(player.getEnderChestInventory(), currencyItem, Flags.check(settingFlags, PayTpPriceDeductionFlags.ALLOW_SHULKER_BOX));
    }

    return totalCount;
  }

  /**
   * Removes a price from the player's permitted storage in configured priority order.
   *
   * @param currencyItemFullId the namespaced currency item identifier
   * @param player the player being charged
   * @param price the number of items to remove
   * @param configFlags combined {@link PayTpPriceDeductionFlags} values
   * @return {@code true} when the requested amount was removed; otherwise {@code false}
   */
  public static boolean proceedPayment(
      String currencyItemFullId,
      Player player,
      int price,
      int configFlags
  ) {
    Item currencyItem = PayTpItemHandler.getItemByStringId(currencyItemFullId);

    // Proceed payment based on priority level
    int remaining = price;
    Inventory playerInventory = player.getInventory();
    PlayerEnderChestContainer enderChestInventory = player.getEnderChestInventory();

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 1: Ender Chest Shulker -> Ender Chest -> Inventory Shulker -> Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    if (Flags.equivalent(configFlags,
        PayTpPriceDeductionFlags.ALLOW_ENDER_CHEST,
        PayTpPriceDeductionFlags.PRIORITIZE_ENDER_CHEST,
        PayTpPriceDeductionFlags.ALLOW_SHULKER_BOX,
        PayTpPriceDeductionFlags.PRIORITIZE_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeShulkerItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 2: Ender Chest -> Ender Chest Shulker -> Inventory -> Inventory Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.equivalent(configFlags,
        PayTpPriceDeductionFlags.ALLOW_ENDER_CHEST,
        PayTpPriceDeductionFlags.PRIORITIZE_ENDER_CHEST,
        PayTpPriceDeductionFlags.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
    }


    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 3: Inventory Shulker -> Inventory -> Ender Chest Shulker -> Ender Chest
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.equivalent(configFlags,
        PayTpPriceDeductionFlags.ALLOW_ENDER_CHEST,
        PayTpPriceDeductionFlags.ALLOW_SHULKER_BOX,
        PayTpPriceDeductionFlags.PRIORITIZE_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 4: Inventory -> Inventory Shulker -> Ender Chest -> Ender Chest Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.equivalent(configFlags,
        PayTpPriceDeductionFlags.ALLOW_ENDER_CHEST,
        PayTpPriceDeductionFlags.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(enderChestInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 5: Ender Chest -> Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpPriceDeductionFlags.ALLOW_ENDER_CHEST,
        PayTpPriceDeductionFlags.PRIORITIZE_ENDER_CHEST)) {

      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 6: Inventory -> Ender Chest
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpPriceDeductionFlags.ALLOW_ENDER_CHEST)) {

      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 7: Inventory -> Inventory Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpPriceDeductionFlags.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 7: Inventory Shulker -> Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpPriceDeductionFlags.ALLOW_SHULKER_BOX,
        PayTpPriceDeductionFlags.PRIORITIZE_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 8: Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    else {
      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
    }

    return remaining <= price;
  }
}
