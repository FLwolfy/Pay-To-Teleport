package com.flwolfy.paytp.modmenu.model;

import com.flwolfy.paytp.data.config.PayTpConfigData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public final class PayTpConfigEditorModel {

  private final Map<String, PayTpConfigRecordMapper.Field> fields;
  private final Map<String, Object> values = new LinkedHashMap<>();
  private long revision;
  private long validatedRevision = -1;
  private List<String> invalidFields = List.of();

  public PayTpConfigEditorModel(PayTpConfigData current, PayTpConfigData defaults) {
    fields = PayTpConfigRecordMapper.describe(current, defaults);
    fields.forEach((path, field) -> values.put(path, copy(field.value())));
  }

  public PayTpConfigRecordMapper.Field field(String path) {
    PayTpConfigRecordMapper.Field field = fields.get(path);
    if (field == null) {
      throw new IllegalArgumentException("Unknown configuration field " + path);
    }
    return field;
  }

  public <T> T get(String path, Class<T> type) {
    return type.cast(values.get(path));
  }

  public void set(String path, Object value) {
    if (!fields.containsKey(path)) {
      throw new IllegalArgumentException("Unknown configuration field " + path);
    }
    Object copied = copy(value);
    if (!Objects.equals(values.get(path), copied)) {
      values.put(path, copied);
      revision++;
    }
  }

  public long revision() {
    return revision;
  }

  public PayTpConfigData build() {
    return PayTpConfigRecordMapper.reconstruct(PayTpConfigData.class, values);
  }

  public Optional<Component> error(String path, boolean suppress) {
    if (suppress) {
      return Optional.empty();
    }
    validateIfChanged();
    return invalidFields.contains(path)
        ? Optional.of(Component.translatable("paytp.config." + path + ".invalid"))
        : Optional.empty();
  }

  public List<String> invalidFields() {
    validateIfChanged();
    return invalidFields;
  }

  private void validateIfChanged() {
    if (validatedRevision == revision) {
      return;
    }
    invalidFields = build().validate();
    validatedRevision = revision;
  }

  private static Object copy(Object value) {
    if (value instanceof List<?> list) {
      return List.copyOf(new ArrayList<>(list));
    }
    return value;
  }
}
