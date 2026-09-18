package com.flwolfy.paytp.modmenu.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PayTpConfigRecordMapper {

  private PayTpConfigRecordMapper() {}

  public static Map<String, Field> describe(Record current, Record defaults) {
    Map<String, Field> fields = new LinkedHashMap<>();
    describeRecord("", current, defaults, fields);
    return Collections.unmodifiableMap(new LinkedHashMap<>(fields));
  }

  public static <T extends Record> T reconstruct(
      Class<T> recordType,
      Map<String, Object> values
  ) {
    return reconstructRecord(recordType, "", values);
  }

  private static void describeRecord(
      String prefix,
      Record current,
      Record defaults,
      Map<String, Field> fields
  ) {
    if (current == null || defaults == null || current.getClass() != defaults.getClass()) {
      throw new IllegalArgumentException("Configuration records and defaults must have equal types");
    }

    for (RecordComponent component : current.getClass().getRecordComponents()) {
      String path = prefix.isEmpty() ? component.getName() : prefix + "." + component.getName();
      try {
        Object value = component.getAccessor().invoke(current);
        Object defaultValue = component.getAccessor().invoke(defaults);
        if (component.getType().isRecord()) {
          describeRecord(path, (Record) value, (Record) defaultValue, fields);
        } else {
          fields.put(path, new Field(
              path,
              component.getType(),
              component.getGenericType(),
              value,
              defaultValue
          ));
        }
      } catch (ReflectiveOperationException exception) {
        throw new IllegalStateException("Could not read configuration field " + path, exception);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends Record> T reconstructRecord(
      Class<T> recordType,
      String prefix,
      Map<String, Object> values
  ) {
    RecordComponent[] components = recordType.getRecordComponents();
    Object[] arguments = new Object[components.length];
    Class<?>[] parameterTypes = new Class<?>[components.length];
    for (int index = 0; index < components.length; index++) {
      RecordComponent component = components[index];
      parameterTypes[index] = component.getType();
      String path = prefix.isEmpty() ? component.getName() : prefix + "." + component.getName();
      arguments[index] = component.getType().isRecord()
          ? reconstructRecord((Class<? extends Record>) component.getType(), path, values)
          : values.get(path);
    }

    try {
      Constructor<T> constructor = recordType.getDeclaredConstructor(parameterTypes);
      constructor.setAccessible(true);
      return constructor.newInstance(arguments);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Could not reconstruct " + recordType.getName(), exception);
    }
  }

  public record Field(
      String path,
      Class<?> rawType,
      Type genericType,
      Object value,
      Object defaultValue
  ) {}
}
