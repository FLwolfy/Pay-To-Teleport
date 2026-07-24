package com.flwolfy.paytp.util;

import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpSettingFlags;
import com.flwolfy.paytp.data.PayTpData;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PayTpCalculator {

  private PayTpCalculator() {}

  public static double calculateDistance(
      PayTpData from,
      PayTpData to
  ) {
    Vec3 fromPos = from.pos();
    Vec3 toPos = to.pos();

    ResourceKey<Level> fromWorld = from.world();
    ResourceKey<Level> toWorld = to.world();

    double distance;
    if (fromWorld == toWorld) {
      distance = fromPos.distanceTo(toPos);
    } else if (fromWorld == Level.END) {
      distance = fromPos.distanceTo(Vec3.ZERO);
    } else if (toWorld == Level.END) {
      distance = Vec3.ZERO.distanceTo(toPos);
    } else if (fromWorld == Level.NETHER) {
      distance = (fromPos.scale(8)).distanceTo(toPos);
    } else if (toWorld == Level.NETHER) {
      distance = fromPos.distanceTo(toPos.scale(0.125));
    } else {
      // Note: If you have other worlds, customize your distance calculation here.
      //       Default distance -> Euclidean distance
      distance = fromPos.distanceTo(toPos);
    }

    return distance;
  }

  public static int calculatePrice(
      double distance,
      double baseRadius,
      double increaseRate,
      double externalMultiplier,
      int minPrice,
      int maxPrice
  ) {
    double distanceBeyondBase = Math.max(0, distance - baseRadius);
    int calculatedPrice = (int) Math.round((minPrice + distanceBeyondBase * increaseRate) * externalMultiplier);

    return Math.min(calculatedPrice, maxPrice);
  }

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
