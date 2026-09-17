package com.flwolfy.paytp.modmenu.builder;

import com.flwolfy.paytp.modmenu.model.PayTpConfigEditorModel;
import com.flwolfy.paytp.modmenu.model.PayTpConfigRecordMapper;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public final class PayTpEntryContext {

  private final PayTpConfigEditorModel model;
  private final PayTpConfigRecordMapper.Field field;
  private final Component label;
  private final Component[] tooltip;
  private final Component resetText;
  private final boolean suppressErrors;

  public PayTpEntryContext(
      PayTpConfigEditorModel model,
      PayTpConfigRecordMapper.Field field,
      Component label,
      Component[] tooltip,
      Component resetText,
      boolean suppressErrors
  ) {
    this.model = model;
    this.field = field;
    this.label = label;
    this.tooltip = tooltip.clone();
    this.resetText = resetText;
    this.suppressErrors = suppressErrors;
  }

  public PayTpConfigEditorModel model() {
    return model;
  }

  public PayTpConfigRecordMapper.Field field() {
    return field;
  }

  public Component label() {
    return label;
  }

  public Component[] tooltip() {
    return tooltip.clone();
  }

  public Component resetText() {
    return resetText;
  }

  public boolean suppressErrors() {
    return suppressErrors;
  }

  public Optional<Component[]> tooltipValue() {
    return tooltip.length == 0 ? Optional.empty() : Optional.of(tooltip());
  }
}
