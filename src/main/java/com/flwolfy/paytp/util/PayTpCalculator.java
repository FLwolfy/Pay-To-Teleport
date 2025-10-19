package com.flwolfy.paytp.util;

import com.flwolfy.paytp.config.PayTpConfigFlag;
import com.flwolfy.paytp.config.PayTpData;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PayTpCalculator {

  private PayTpCalculator() {}

  @SuppressWarnings("resource")
  public static int calculatePrice(
      double baseRadius,
      double increaseRate,
      double externalMultiplier,
      int minPrice,
      int maxPrice,
      PayTpData from,
      PayTpData to
  ) {
    Vec3d fromPos = from.pos();
    Vec3d toPos = to.pos();
    RegistryKey<World> fromWorld = from.world().getRegistryKey();
    RegistryKey<World> toWorld = to.world().getRegistryKey();

    double distance;
    if (fromWorld == toWorld) {
      distance = fromPos.distanceTo(toPos);
    } else if (fromWorld == World.END) {
      distance = fromPos.distanceTo(Vec3d.ZERO);
    } else if (toWorld == World.END) {
      distance = Vec3d.ZERO.distanceTo(toPos);
    } else if (fromWorld == World.NETHER) {
      distance = (fromPos.multiply(8)).distanceTo(toPos);
    } else if (toWorld == World.NETHER) {
      distance = fromPos.distanceTo(toPos.multiply(0.125));
    } else {
      // Note: If you have other worlds, customize your price calculation here.
      //       Default -> price * crossDimMultiplier
      distance = fromPos.distanceTo(toPos);
    }

    double distanceBeyondBase = Math.max(0, distance - baseRadius);
    int calculatedPrice = (int) Math.round((minPrice + distanceBeyondBase * increaseRate) * externalMultiplier);

    return Math.min(calculatedPrice, maxPrice);
  }

  public static int checkBalance(
      String currencyItemFullId,
      PlayerEntity player,
      int configFlags
  ) {
    Item currencyItem = PayTpItemHandler.getItemByStringId(currencyItemFullId);

    int totalCount = PayTpItemHandler.getInventoryCount(player.getInventory(), currencyItem, PayTpConfigFlag.check(configFlags, PayTpConfigFlag.ALLOW_SHULKER_BOX));
    if (PayTpConfigFlag.check(configFlags, PayTpConfigFlag.ALLOW_ENDER_CHEST)) {
      totalCount += PayTpItemHandler.getInventoryCount(player.getEnderChestInventory(), currencyItem, PayTpConfigFlag.check(configFlags, PayTpConfigFlag.ALLOW_SHULKER_BOX));
    }

    return totalCount;
  }

  public static boolean proceedPayment(
      String currencyItemFullId,
      PlayerEntity player,
      int price,
      int configFlags
  ) {
    Item currencyItem = PayTpItemHandler.getItemByStringId(currencyItemFullId);

    // Proceed payment based on priority level
    int remaining = price;
    PlayerInventory pi = player.getInventory();
    EnderChestInventory ei = player.getEnderChestInventory();

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 1: Ender Chest Shulker -> Ender Chest -> Inventory Shulker -> Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    if (PayTpConfigFlag.equivalent(configFlags,
        PayTpConfigFlag.ALLOW_ENDER_CHEST,
        PayTpConfigFlag.PRIORITIZE_ENDER_CHEST,
        PayTpConfigFlag.ALLOW_SHULKER_BOX,
        PayTpConfigFlag.PRIORITIZE_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeShulkerItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 2: Ender Chest -> Ender Chest Shulker -> Inventory -> Inventory Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (PayTpConfigFlag.equivalent(configFlags,
        PayTpConfigFlag.ALLOW_ENDER_CHEST,
        PayTpConfigFlag.PRIORITIZE_ENDER_CHEST,
        PayTpConfigFlag.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
    }


    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 3: Inventory Shulker -> Inventory -> Ender Chest Shulker -> Ender Chest
    // ------------------------------------------------------------------------------------------------------------------------
    else if (PayTpConfigFlag.equivalent(configFlags,
        PayTpConfigFlag.ALLOW_ENDER_CHEST,
        PayTpConfigFlag.ALLOW_SHULKER_BOX,
        PayTpConfigFlag.PRIORITIZE_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 4: Inventory -> Inventory Shulker -> Ender Chest -> Ender Chest Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (PayTpConfigFlag.equivalent(configFlags,
        PayTpConfigFlag.ALLOW_ENDER_CHEST,
        PayTpConfigFlag.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(ei, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 5: Ender Chest -> Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    else if (PayTpConfigFlag.check(configFlags,
        PayTpConfigFlag.ALLOW_ENDER_CHEST,
        PayTpConfigFlag.PRIORITIZE_ENDER_CHEST)) {

      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 6: Inventory -> Ender Chest
    // ------------------------------------------------------------------------------------------------------------------------
    else if (PayTpConfigFlag.check(configFlags,
        PayTpConfigFlag.ALLOW_ENDER_CHEST)) {

      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 7: Inventory -> Inventory Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (PayTpConfigFlag.check(configFlags,
        PayTpConfigFlag.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 7: Inventory Shulker -> Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    else if (PayTpConfigFlag.check(configFlags,
        PayTpConfigFlag.ALLOW_SHULKER_BOX,
        PayTpConfigFlag.PRIORITIZE_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 8: Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    else {
      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
    }

    return remaining <= price;
  }
}
