package com.flwolfy.paytp.util;

import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpSettingFlags;
import com.flwolfy.paytp.data.PayTpData;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PayTpCalculator {

  private PayTpCalculator() {}

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

    @SuppressWarnings("resource")
    RegistryKey<World> fromWorld = from.world().getRegistryKey();
    @SuppressWarnings("resource")
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
    if (Flags.equivalent(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST,
        PayTpSettingFlags.PRIORITIZE_ENDER_CHEST,
        PayTpSettingFlags.ALLOW_SHULKER_BOX,
        PayTpSettingFlags.PRIORITIZE_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeShulkerItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 2: Ender Chest -> Ender Chest Shulker -> Inventory -> Inventory Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.equivalent(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST,
        PayTpSettingFlags.PRIORITIZE_ENDER_CHEST,
        PayTpSettingFlags.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
    }


    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 3: Inventory Shulker -> Inventory -> Ender Chest Shulker -> Ender Chest
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.equivalent(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST,
        PayTpSettingFlags.ALLOW_SHULKER_BOX,
        PayTpSettingFlags.PRIORITIZE_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 4: Inventory -> Inventory Shulker -> Ender Chest -> Ender Chest Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.equivalent(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST,
        PayTpSettingFlags.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(ei, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 5: Ender Chest -> Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST,
        PayTpSettingFlags.PRIORITIZE_ENDER_CHEST)) {

      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 6: Inventory -> Ender Chest
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpSettingFlags.ALLOW_ENDER_CHEST)) {

      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeInventoryItems(ei, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 7: Inventory -> Inventory Shulker
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpSettingFlags.ALLOW_SHULKER_BOX)) {

      remaining = PayTpItemHandler.removeInventoryItems(pi, currencyItem, remaining);
      remaining = PayTpItemHandler.removeShulkerItems(pi, currencyItem, remaining);
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Priority 7: Inventory Shulker -> Inventory
    // ------------------------------------------------------------------------------------------------------------------------
    else if (Flags.check(configFlags,
        PayTpSettingFlags.ALLOW_SHULKER_BOX,
        PayTpSettingFlags.PRIORITIZE_SHULKER_BOX)) {

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
