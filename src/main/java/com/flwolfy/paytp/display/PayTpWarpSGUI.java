package com.flwolfy.paytp.display;

import com.flwolfy.paytp.command.warp.PayTpWarpManager;
import com.flwolfy.paytp.data.lang.PayTpLangManager;
import com.flwolfy.paytp.util.PayTpMessageSender;
import com.flwolfy.paytp.util.PayTpTextBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class PayTpWarpSGUI extends SimpleGui {

  private static final int[] WARP_SLOTS = {
      11, 12, 13, 14, 15, 16, 17,
      20, 21, 22, 23, 24, 25, 26,
      29, 30, 31, 32, 33, 34, 35,
      38, 39, 40, 41, 42, 43, 44
  };
  private static final List<WeakReference<PayTpWarpSGUI>> OPEN_MENUS = new ArrayList<>();

  private final PayTpWarpManager manager;
  private final ToIntFunction<String> teleportAction;
  private PayTpWarpManager.AccessType filter;
  private int page;

  private PayTpWarpSGUI(
      ServerPlayer player,
      PayTpWarpManager manager,
      ToIntFunction<String> teleportAction
  ) {
    super(MenuType.GENERIC_9x6, player, false);
    this.manager = manager;
    this.teleportAction = teleportAction;
    synchronized (OPEN_MENUS) {
      OPEN_MENUS.add(new WeakReference<>(this));
    }
    setTitle(text("paytp.gui.warp.title"));
    setLockPlayerInventory(true);
    render();
  }

  public static void open(
      ServerPlayer player,
      PayTpWarpManager manager,
      ToIntFunction<String> teleportAction
  ) {
    PayTpWarpSGUI gui = new PayTpWarpSGUI(player, manager, teleportAction);
    if (gui.open()) {
      gui.playSound(SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.7F, 2.0F);
    }
  }

  public static void refreshAll(MinecraftServer server) {
    synchronized (OPEN_MENUS) {
      OPEN_MENUS.removeIf(reference -> {
        PayTpWarpSGUI gui = reference.get();
        if (gui == null) {
          return true;
        }
        if (gui.player.level().getServer() == server) {
          gui.render();
        }
        return false;
      });
    }
  }

  private void render() {
    GuiElementBuilder filler = element(Items.GRAY_STAINED_GLASS_PANE)
        .setName(Component.empty());
    for (int slot = 0; slot < getVirtualSize(); slot++) {
      setSlot(slot, filler.build());
    }
    GuiElementBuilder contentBackground = element(Items.BLACK_STAINED_GLASS_PANE)
        .setName(Component.empty());
    for (int slot : WARP_SLOTS) {
      setSlot(slot, contentBackground.build());
    }
    GuiElementBuilder divider = element(Items.CYAN_STAINED_GLASS_PANE)
        .setName(Component.empty());
    for (int row = 0; row < 6; row++) {
      setSlot(row * 9 + 1, divider.build());
    }

    List<PayTpWarpManager.WarpView> all = manager.getVisibleWarps(player, null);
    List<PayTpWarpManager.WarpView> filtered = filter == null
        ? all
        : all.stream().filter(warp -> warp.accessType() == filter).toList();
    int accessible = (int) filtered.stream().filter(PayTpWarpManager.WarpView::accessible).count();
    int pageCount = Math.max(1, (filtered.size() + WARP_SLOTS.length - 1) / WARP_SLOTS.length);
    page = Math.floorMod(page, pageCount);

    setSlot(5, element(Items.WRITABLE_BOOK)
        .setName(text("paytp.gui.warp.summary").copy().withStyle(ChatFormatting.GOLD))
        .addLoreLine(text("paytp.gui.warp.summary.filter", filterName()))
        .addLoreLine(text("paytp.gui.warp.summary.count", filtered.size(), accessible))
        .addLoreLine(text("paytp.gui.warp.page.value", page + 1, pageCount))
        .build());
    if (filtered.isEmpty()) {
      setSlot(23, element(Items.PAPER)
          .setName(text("paytp.gui.warp.empty").copy().withStyle(ChatFormatting.GRAY))
          .build());
    }

    int first = page * WARP_SLOTS.length;
    int last = Math.min(first + WARP_SLOTS.length, filtered.size());
    for (int index = first; index < last; index++) {
      setSlot(WARP_SLOTS[index - first], warpButton(filtered.get(index)));
    }

    if (pageCount > 1) {
      setSlot(2, element(Items.PLAYER_HEAD)
          .setProfile("MHF_ArrowLeft")
          .setName(text("paytp.gui.warp.page.previous").copy().withStyle(ChatFormatting.AQUA))
          .addLoreLine(text("paytp.gui.warp.page.value", page + 1, pageCount))
          .setCallback(() -> changePage(-1, pageCount))
          .build());
      setSlot(8, element(Items.PLAYER_HEAD)
          .setProfile("MHF_ArrowRight")
          .setName(text("paytp.gui.warp.page.next").copy().withStyle(ChatFormatting.AQUA))
          .addLoreLine(text("paytp.gui.warp.page.value", page + 1, pageCount))
          .setCallback(() -> changePage(1, pageCount))
          .build());
    }

    setSlot(0, categoryButton(null, Items.WATER_BUCKET, "all"));
    setSlot(9, categoryButton(
        PayTpWarpManager.AccessType.SERVER, Items.NETHERITE_INGOT, "server"
    ));
    setSlot(18, categoryButton(
        PayTpWarpManager.AccessType.OWNED, Items.GOLD_INGOT, "owned"
    ));
    setSlot(27, categoryButton(
        PayTpWarpManager.AccessType.PUBLIC, Items.IRON_INGOT, "public"
    ));
    setSlot(36, categoryButton(
        PayTpWarpManager.AccessType.INVITED, Items.COPPER_INGOT, "invited"
    ));
    setSlot(45, categoryButton(
        PayTpWarpManager.AccessType.LOCKED, Items.BUCKET, "locked"
    ));
    setSlot(47, element(Items.CLOCK)
        .setName(text("paytp.gui.warp.refresh").copy().withStyle(ChatFormatting.YELLOW))
        .addLoreLine(text("paytp.gui.warp.page.value", page + 1, pageCount))
        .addLoreLine(text("paytp.gui.warp.refresh.description"))
        .setCallback(() -> {
          playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
          render();
        })
        .build());
    setSlot(53, element(Items.BARRIER)
        .setName(text("paytp.gui.warp.close").copy().withStyle(ChatFormatting.RED))
        .setCallback(() -> close())
        .build());
  }

  private GuiElementBuilder categoryButton(
      PayTpWarpManager.AccessType type,
      Item item,
      String key
  ) {
    boolean selected = filter == type;
    GuiElementBuilder builder = element(item)
        .setName(text("paytp.gui.warp.filter." + key).copy().withStyle(
            selected ? ChatFormatting.GREEN : ChatFormatting.GRAY
        ))
        .addLoreLine(text("paytp.gui.warp.filter.description"))
        .setCallback(() -> {
          filter = type;
          page = 0;
          playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
          render();
        });
    if (selected) {
      builder.glow();
    }
    return builder;
  }

  private GuiElementBuilder warpButton(PayTpWarpManager.WarpView warp) {
    boolean accessible = warp.accessible();
    Item icon = accessible ? Items.EMERALD_BLOCK : Items.REDSTONE_BLOCK;
    GuiElementBuilder builder = element(icon)
        .setName(Component.literal(warp.name()).withStyle(
            accessible ? ChatFormatting.GREEN : ChatFormatting.RED
        ))
        .addLoreLine(warpText(
            accessible,
            "paytp.gui.warp.type",
            text("paytp.gui.warp.filter."
                + warp.accessType().name().toLowerCase(Locale.ROOT)).getString()
        ));
    if (warp.accessType() != PayTpWarpManager.AccessType.LOCKED) {
      builder.addLoreLine(warpText(
          accessible,
          "paytp.gui.warp.dimension",
          warp.destination().world().identifier()
      ));
      builder.addLoreLine(warpText(
          accessible,
          "paytp.gui.warp.position",
          String.format(
              Locale.ROOT,
              "%.1f, %.1f, %.1f",
              warp.destination().pos().x,
              warp.destination().pos().y,
              warp.destination().pos().z
          )
      ));
      if (warp.ownerId() != null) {
        builder.addLoreLine(warpText(
            accessible,
            "paytp.gui.warp.owner",
            warp.ownerName()
        ));
      }
    }
    builder.addLoreLine(text(accessible
        ? "paytp.gui.warp.available" : warp.inactive()
            ? "paytp.gui.warp.inactive" : "paytp.gui.warp.locked"));
    builder.addLoreLine(text(accessible
        ? "paytp.gui.warp.click" : "paytp.gui.warp.unavailable"));
    if (accessible) {
      builder.glow();
    }
    return builder.setCallback(() -> select(warp.name()));
  }

  private void select(String name) {
    PayTpWarpManager.WarpView current = manager.getWarpView(player, name);
    if (current == null) {
      PayTpMessageSender.msgNoWarp(player, name);
      render();
      return;
    }
    if (current.accessType() == PayTpWarpManager.AccessType.LOCKED) {
      PayTpMessageSender.msgWarpLocked(player, name);
      render();
      return;
    }
    if (current.inactive()) {
      PayTpMessageSender.msgWarpInactive(player, name);
      render();
      return;
    }
    close();
    teleportAction.applyAsInt(name);
  }

  private void changePage(int offset, int pageCount) {
    page = Math.floorMod(page + offset, pageCount);
    playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
    render();
  }

  private String filterName() {
    return text("paytp.gui.warp.filter." + (filter == null
        ? "all" : filter.name().toLowerCase(Locale.ROOT))).getString();
  }

  private void playSound(
      net.minecraft.sounds.SoundEvent sound,
      SoundSource source,
      float volume,
      float pitch
  ) {
    player.connection.send(new ClientboundSoundEntityPacket(
        Holder.direct(sound),
        source,
        player,
        volume,
        pitch,
        player.getRandom().nextLong()
    ));
  }

  private Component warpText(boolean accessible, String key, Object... arguments) {
    return PayTpTextBuilder.format(
        PayTpLangManager.getInstance().getText(key),
        PayTpTextBuilder.DEFAULT_TEXT_COLOR,
        accessible
            ? PayTpTextBuilder.DEFAULT_HIGHLIGHT_COLOR
            : PayTpTextBuilder.DEFAULT_WARN_COLOR,
        arguments
    );
  }

  private Component text(String key, Object... arguments) {
    return PayTpTextBuilder.format(
        PayTpLangManager.getInstance().getText(key),
        arguments
    );
  }

  private static GuiElementBuilder element(Item item) {
    return new GuiElementBuilder(item).hideDefaultTooltip();
  }
}
