package com.flwolfy.paytp.util;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.data.PayTpTeleportType;
import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.data.script.PayTpScript;
import com.flwolfy.paytp.data.script.PayTpScriptManager;
import com.flwolfy.paytp.data.script.PayTpScriptPosition;
import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpSettingFlags;

import java.util.Map;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;

import org.slf4j.Logger;

/**
 * Calculates teleport prices and manages currency balance and payment operations.
 */
public class PayTpCalculator {

  private static final Logger LOGGER = PayTpMod.LOGGER;

  private PayTpCalculator() {}

  /**
   * Evaluates and clamps the configured price algorithm for one teleport.
   *
   * <p>A zero maximum price bypasses script execution. If a custom algorithm fails, the default
   * algorithm is evaluated with the same arguments before the result is clamped.</p>
   *
   * @param from the player's current location
   * @param to the teleport destination
   * @param teleportType the operation that initiated the teleport
   * @param player the name of the player being teleported
   * @param otherPlayer the other request participant, or an empty string
   * @param priceConfig the price range, currency, and algorithm configuration
   * @return the final price within the configured inclusive range
   * @throws IllegalStateException if the default algorithm fails
   */
  public static int calculatePrice(
      PayTpData from,
      PayTpData to,
      PayTpTeleportType teleportType,
      String player,
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
      return Math.clamp(price, minPrice, maxPrice);
    } catch (Exception e) {
      if (priceConfig.algorithm().equals(PayTpConfigData.Price.DEFAULT_ALGORITHM)) {
        throw new IllegalStateException("Default PayTp price algorithm failed", e);
      }

      LOGGER.error("Custom PayTp price algorithm failed, using default", e);
      int fallbackPrice = evaluatePrice(
          PayTpConfigData.Price.DEFAULT_ALGORITHM,
          from,
          to,
          teleportType,
          player,
          otherPlayer
      );
      return Math.clamp(fallbackPrice, minPrice, maxPrice);
    }
  }

  private static int evaluatePrice(
      PayTpScript algorithm,
      PayTpData from,
      PayTpData to,
      PayTpTeleportType teleportType,
      String player,
      String otherPlayer
  ) {
    return PayTpScriptManager.getInstance().evaluate(
        algorithm,
        Integer.class,
        Map.entry("from", PayTpScriptPosition.from(from)),
        Map.entry("to", PayTpScriptPosition.from(to)),
        Map.entry("teleportType", teleportType.toString()),
        Map.entry("player", player),
        Map.entry("otherPlayer", otherPlayer)
    );
  }

  /**
   * Compiles and test-executes a price algorithm with representative arguments.
   *
   * <p>This validation performs real script execution, including any shell commands contained in
   * the script.</p>
   *
   * @param algorithm the algorithm to validate
   * @throws RuntimeException if compilation, execution, or result type validation fails
   */
  public static void validatePriceAlgorithm(PayTpScript algorithm) {
    PayTpScriptManager.getInstance().evaluate(
        algorithm,
        Integer.class,
        Map.entry("from", new PayTpScriptPosition(
            0.0, 64.0, 0.0, "minecraft:overworld"
        )),
        Map.entry("to", new PayTpScriptPosition(
            100.0, 64.0, 100.0, "minecraft:overworld"
        )),
        Map.entry("teleportType", PayTpTeleportType.COORDINATE.toString()),
        Map.entry("player", "Player"),
        Map.entry("otherPlayer", "")
    );
  }

  /**
   * Counts all available currency permitted by the configured storage flags.
   *
   * @param currencyItemFullId the namespaced currency item identifier
   * @param player the player whose accessible storage is inspected
   * @param settingFlags combined {@link PayTpSettingFlags} values
   * @return the total number of matching currency items
   */
  public static int checkBalance(
      String currencyItemFullId,
      Player player,
      int settingFlags
  ) {
    Item currencyItem = PayTpItemHandler.getItemByStringId(currencyItemFullId);

    int totalCount = PayTpItemHandler.getInventoryCount(player.getInventory(), currencyItem, Flags.check(settingFlags, PayTpSettingFlags.ALLOW_SHULKER_BOX));
    if (Flags.check(settingFlags, PayTpSettingFlags.ALLOW_ENDER_CHEST)) {
      totalCount += PayTpItemHandler.getInventoryCount(player.getEnderChestInventory(), currencyItem, Flags.check(settingFlags, PayTpSettingFlags.ALLOW_SHULKER_BOX));
    }

    return totalCount;
  }

  /**
   * Removes a price from the player's permitted storage in configured priority order.
   *
   * @param currencyItemFullId the namespaced currency item identifier
   * @param player the player being charged
   * @param price the number of items to remove
   * @param configFlags combined {@link PayTpSettingFlags} values
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
        PayTpSettingFlags.ALLOW_ENDER_CHEST,
        PayTpSettingFlags.PRIORITIZE_ENDER_CHEST,
        PayTpSettingFlags.ALLOW_SHULKER_BOX,
        PayTpSettingFlags.PRIORITIZE_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeShulkerItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 2: Ender Chest -> Ender Chest Shulker -> Inventory -> Inventory Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.equivalent(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST,
        PayTpSettingFlags.PRIORITIZE_ENDER_CHEST,
        PayTpSettingFlags.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
    }


    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 3: Inventory Shulker -> Inventory -> Ender Chest Shulker -> Ender Chest
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.equivalent(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST,
        PayTpSettingFlags.ALLOW_SHULKER_BOX,
        PayTpSettingFlags.PRIORITIZE_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 4: Inventory -> Inventory Shulker -> Ender Chest -> Ender Chest Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.equivalent(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST,
        PayTpSettingFlags.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(enderChestInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 5: Ender Chest -> Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST,
        PayTpSettingFlags.PRIORITIZE_ENDER_CHEST)) {

      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 6: Inventory -> Ender Chest
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST)) {

      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(enderChestInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 7: Inventory -> Inventory Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpSettingFlags.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(playerInventory, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(playerInventory, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 7: Inventory Shulker -> Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpSettingFlags.ALLOW_SHULKER_BOX,
        PayTpSettingFlags.PRIORITIZE_SHULKER_BOX)) {

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
