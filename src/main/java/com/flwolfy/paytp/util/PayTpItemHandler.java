package com.flwolfy.paytp.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class PayTpItemHandler {

  private PayTpItemHandler() {}

  /**
   * Gets an item from its full registry ID.
   *
   * @param fullId item ID, such as {@code minecraft:diamond}
   */
  public static Item getItemByStringId(String fullId) {
    Identifier identifier = Identifier.tryParse(fullId);
    if (identifier == null) {
      throw new IllegalArgumentException("Invalid item ID: " + fullId);
    }
    return BuiltInRegistries.ITEM.getOptional(identifier)
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown item: " + identifier
        ));
  }

  /**
   * Get item count in an inventory.
   * @param allowShulkerBox whether to allow counting items in shulker boxes in this inventory or not.
   */
  public static int getInventoryCount(Container inventory, Item target, boolean allowShulkerBox) {
    int count = 0;

    for (int i = 0; i < inventory.getContainerSize(); i++) {
      ItemStack stack = inventory.getItem(i);

      if (stack.is(target)) {
        count += stack.getCount();
      }

      if (allowShulkerBox && stack.getItem() instanceof BlockItem blockItem
          && blockItem.getBlock() instanceof ShulkerBoxBlock) {

        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
          for (ItemStackTemplate inner : container.nonEmptyItems()) {
            if (inner.is(target)) {
              count += inner.count();
            }
          }
        }
      }
    }

    return count;
  }

  /**
   * Remove target item in the given inventory with maximum amount.
   */
  public static int removeInventoryItems(Container inventory, Item target, int amount) {
    int remaining = amount;

    for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
      ItemStack stack = inventory.getItem(i);
      if (stack.is(target)) {
        int removed = Math.min(stack.getCount(), remaining);
        stack.shrink(removed);
        remaining -= removed;
      }
    }

    inventory.setChanged();
    return remaining;
  }

  /**
   * Remove target item in the shulker boxes in the given inventory with maximum amount.
   */
  public static int removeShulkerItems(Container inventory, Item targetItem, int amount) {
    final int[] remaining = {amount};

    for (int i = 0; i < inventory.getContainerSize() && remaining[0] > 0; i++) {
      ItemStack stack = inventory.getItem(i);
      if (!(stack.getItem() instanceof BlockItem blockItem) ||
          !(blockItem.getBlock() instanceof ShulkerBoxBlock)) continue;

      ItemContainerContents oldContainer = stack.get(DataComponents.CONTAINER);
      if (oldContainer == null) continue;

      ItemContainerContents newContainer = ItemContainerContents.fromItems(
          oldContainer.allItemsCopyStream()
              .peek(inner -> {
                if (inner.is(targetItem) && remaining[0] > 0) {
                  int take = Math.min(inner.getCount(), remaining[0]);
                  inner.shrink(take);
                  remaining[0] -= take;
                }
              })
              .toList()
      );

      stack.set(DataComponents.CONTAINER, newContainer);
    }

    inventory.setChanged();
    return remaining[0];
  }
}
