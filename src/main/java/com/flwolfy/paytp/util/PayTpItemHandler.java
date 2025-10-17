package com.flwolfy.paytp.util;

import java.util.stream.StreamSupport;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class PayTpItemHandler {

  public static Item getItemByStringId(String fullId) {
    String namespace = fullId.contains(":") ? fullId.substring(0, fullId.lastIndexOf(':')) : "minecraft";
    String id        = fullId.contains(":") ? fullId.substring(fullId.lastIndexOf(':') + 1) : fullId;
    Identifier currencyID = Identifier.of(namespace, id);
    return Registries.ITEM.get(currencyID);
  }

  public static int getInventoryCount(Inventory inventory, Item target, boolean allowShulkerBox) {
    int count = 0;

    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.getStack(i);

      if (stack.isOf(target)) {
        count += stack.getCount();
      }

      if (allowShulkerBox && stack.getItem() instanceof BlockItem blockItem
          && blockItem.getBlock() instanceof ShulkerBoxBlock) {

        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container != null) {
          for (ItemStack inner : container.iterateNonEmpty()) {
            if (inner.isOf(target)) {
              count += inner.getCount();
            }
          }
        }
      }
    }

    return count;
  }

  public static int removeInventoryItems(Inventory inventory, Item target, int amount) {
    int remaining = amount;

    for (int i = 0; i < inventory.size() && remaining > 0; i++) {
      ItemStack stack = inventory.getStack(i);
      if (stack.isOf(target)) {
        int removed = Math.min(stack.getCount(), remaining);
        stack.decrement(removed);
        remaining -= removed;
      }
    }

    inventory.markDirty();
    return remaining;
  }

  public static int removeShulkerItems(Inventory inventory, Item targetItem, int amount) {
    final int[] remaining = {amount};

    for (int i = 0; i < inventory.size() && remaining[0] > 0; i++) {
      ItemStack stack = inventory.getStack(i);
      if (!(stack.getItem() instanceof BlockItem blockItem) ||
          !(blockItem.getBlock() instanceof ShulkerBoxBlock)) continue;

      ContainerComponent oldContainer = stack.get(DataComponentTypes.CONTAINER);
      if (oldContainer == null) continue;

      ContainerComponent newContainer = ContainerComponent.fromStacks(
          StreamSupport.stream(oldContainer.stream().spliterator(), false)
              .peek(inner -> {
                if (inner.isOf(targetItem) && remaining[0] > 0) {
                  int take = Math.min(inner.getCount(), remaining[0]);
                  inner.decrement(take);
                  remaining[0] -= take;
                }
              })
              .toList()
      );

      stack.set(DataComponentTypes.CONTAINER, newContainer);
    }

    inventory.markDirty();
    return remaining[0];
  }
}
