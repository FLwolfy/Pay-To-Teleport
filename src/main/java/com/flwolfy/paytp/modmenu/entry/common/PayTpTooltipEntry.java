package com.flwolfy.paytp.modmenu.entry.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.Tooltip;
import me.shedaniel.math.Point;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public abstract class PayTpTooltipEntry<T> extends AbstractConfigListEntry<T> {

  private final Supplier<Optional<Component[]>> tooltipSupplier;

  protected PayTpTooltipEntry(
      Component title,
      Supplier<Optional<Component[]>> tooltipSupplier
  ) {
    super(title, false);
    this.tooltipSupplier = tooltipSupplier;
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics,
      int index,
      int y,
      int x,
      int entryWidth,
      int entryHeight,
      int mouseX,
      int mouseY,
      boolean hovered,
      float delta
  ) {
    super.extractRenderState(
        graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta
    );
    if (!isMouseInside(mouseX, mouseY, x, y, entryWidth, entryHeight)) {
      return;
    }

    List<Component> lines = new ArrayList<>();
    if (tooltipSupplier != null) {
      tooltipSupplier.get().ifPresent(values -> lines.addAll(List.of(values)));
    }
    if (!isEnabled()) {
      lines.add(Component.translatable("paytp.config.disabled.tooltip"));
    }
    if (!lines.isEmpty()) {
      addTooltip(Tooltip.of(
          new Point(mouseX, mouseY),
          wrapLinesToScreen(lines.toArray(Component[]::new))
      ));
    }
  }
}
