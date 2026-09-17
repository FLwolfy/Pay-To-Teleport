package com.flwolfy.paytp.modmenu.entry.scalar;

import com.flwolfy.paytp.modmenu.builder.PayTpEntryContext;
import com.flwolfy.paytp.modmenu.entry.common.PayTpControlLayout;
import com.flwolfy.paytp.modmenu.entry.common.PayTpTooltipEntry;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

public final class PayTpBooleanEntry extends PayTpTooltipEntry<Boolean> {

  private final PayTpEntryContext context;
  private final boolean original;
  private final boolean defaultValue;
  private final Button valueButton;
  private final Button resetButton;

  public PayTpBooleanEntry(PayTpEntryContext context) {
    super(context.label(), context::tooltipValue);
    this.context = context;
    original = (Boolean) context.field().value();
    defaultValue = (Boolean) context.field().defaultValue();
    valueButton = Button.builder(Component.empty(), ignored -> {
      context.model().set(context.field().path(), !getValue());
      updateLabel();
    }).bounds(0, 0, 0, 20).build();
    resetButton = Button.builder(context.resetText(), ignored -> {
      context.model().set(context.field().path(), defaultValue);
      updateLabel();
    }).bounds(
        0, 0, Minecraft.getInstance().font.width(context.resetText()) + 6, 20
    ).build();
    setErrorSupplier(() -> context.model().error(
        context.field().path(), context.suppressErrors()
    ));
    updateLabel();
  }

  @Override
  public Boolean getValue() {
    return context.model().get(context.field().path(), Boolean.class);
  }

  @Override
  public Optional<Boolean> getDefaultValue() {
    return Optional.of(defaultValue);
  }

  @Override
  public boolean isEdited() {
    return getValue() != original;
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
    updateLabel();
    graphics.text(
        Minecraft.getInstance().font, getDisplayedFieldName(), x, y + 6, getPreferredTextColor()
    );
    int resetX = PayTpControlLayout.resetX(x, entryWidth, resetButton.getWidth());
    resetButton.setX(resetX);
    resetButton.setY(y);
    resetButton.active = isEditable() && getValue() != defaultValue;
    valueButton.setX(PayTpControlLayout.valueX(x, entryWidth));
    valueButton.setY(y);
    valueButton.setWidth(PayTpControlLayout.valueWidth(resetButton.getWidth()));
    valueButton.active = isEditable();
    valueButton.extractRenderState(graphics, mouseX, mouseY, delta);
    resetButton.extractRenderState(graphics, mouseX, mouseY, delta);
  }

  @Override
  public List<? extends GuiEventListener> children() {
    return List.of(valueButton, resetButton);
  }

  @Override
  public List<? extends NarratableEntry> narratables() {
    return List.of(valueButton, resetButton);
  }

  private void updateLabel() {
    valueButton.setMessage(Component.translatable(getValue()
        ? "text.cloth-config.boolean.value.true"
        : "text.cloth-config.boolean.value.false"));
  }
}
