package com.flwolfy.paytp.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

public class PayTpCalculator {

  public static int calculatePrice(double baseRadius, double increaseRate, int minPrice, int maxPrice, Vec3d from, Vec3d to) {
    double distance = from.distanceTo(to);

    double distanceBeyondBase = Math.max(0, distance - baseRadius);
    int calculatedPrice = (int) Math.round(minPrice + distanceBeyondBase * increaseRate);

    return Math.min(calculatedPrice, maxPrice);
  }

  public static boolean proceedPayment(String currencyItemFullId, PlayerEntity player, int price, boolean allowEnderChest, boolean prioritizeEnderChest) {
    String namespace = currencyItemFullId.contains(":") ? currencyItemFullId.substring(0, currencyItemFullId.lastIndexOf(':')) : "minecraft";
    String id        = currencyItemFullId.contains(":") ? currencyItemFullId.substring(currencyItemFullId.lastIndexOf(':') + 1) : currencyItemFullId;

    Identifier currencyID = Identifier.of(namespace, id);
    Item currencyItem     = Registries.ITEM.get(currencyID);

    PlayerInventory pi     = player.getInventory();
    EnderChestInventory ei = player.getEnderChestInventory();

    int piCount = pi.count(currencyItem);
    int eiCount = allowEnderChest ? ei.count(currencyItem) : 0;

    if (piCount + eiCount >= price) {
      if (allowEnderChest && prioritizeEnderChest) {
        if (!removeFromInventory(ei, currencyItem, price)) {
          removeFromInventory(pi, currencyItem, price - eiCount);
        }
      } else {
        if (!removeFromInventory(pi, currencyItem, price)) {
          removeFromInventory(ei, currencyItem, price - piCount);
        }
      }

      return true;
    }

    return false;
  }

  private static boolean removeFromInventory(Inventory inventory, Item target, int amount) {
    int count = 0;

    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.getStack(i);
      if (stack.isOf(target)) {
        int removed = Math.min(stack.getCount(), amount - count);
        stack.decrement(removed);
        count += removed;

        if (count >= amount) {
          inventory.markDirty();
          return true;
        }
      }
    }

    inventory.markDirty();
    return false;
  }
}
