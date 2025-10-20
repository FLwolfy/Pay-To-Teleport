package com.flwolfy.paytp.data.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.Map;

public class PayTpConfigMapper {

  /**
   * Flattens a record into a Map where keys are "path.to.field" strings and values are the corresponding field values.
   * Nested records are recursively flattened. The resulting Map does not contain nested Maps.
   *
   * @param record the record to flatten
   * @return a Map containing all fields of the record, flattened into "path.to.field" keys
   */
  public static Map<String, Object> flattenData(Record record) {
    Map<String, Object> result = new HashMap<>();
    flatten("", record, result);
    return result;
  }

  /**
   * Internal helper method for recursively flattening a record.
   *
   * @param prefix the current prefix for nested fields
   * @param record the record to flatten
   * @param result the Map to store flattened fields
   */
  private static void flatten(String prefix, Record record, Map<String, Object> result) {
    if (record == null) return;
    Class<?> clazz = record.getClass();
    for (RecordComponent component : clazz.getRecordComponents()) {
      try {
        Object value = component.getAccessor().invoke(record);
        String path = prefix.isEmpty() ? component.getName() : prefix + "." + component.getName();
        if (value != null && value.getClass().isRecord()) {
          flatten(path, (Record) value, result);
        } else {
          result.put(path, value);
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to flatten record " + clazz.getSimpleName(), e);
      }
    }
  }

  /**
   * Reconstructs a record instance from a flattened Map produced by {@link #flattenData(Record)}.
   * Nested records are recursively reconstructed. The Map must contain all field values required by the record's constructor.
   *
   * @param recordClass the class of the record to reconstruct
   * @param flatMap     the flattened Map containing "path.to.field" keys and field values
   * @param <T>         the record type
   * @return a new record instance with values populated from the flatMap
   * @throws RuntimeException if the reconstruction fails due to reflection or missing fields
   */
  @SuppressWarnings("unchecked")
  public static <T extends Record> T unflattenData(Class<T> recordClass, Map<String, Object> flatMap) {
    try {
      RecordComponent[] components = recordClass.getRecordComponents();
      Object[] values = new Object[components.length];

      for (int i = 0; i < components.length; i++) {
        RecordComponent component = components[i];
        String name = component.getName();
        Class<?> type = component.getType();

        if (type.isRecord()) {
          Map<String, Object> nestedMap = new HashMap<>();
          String prefix = name + ".";
          for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
              nestedMap.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
          }
          values[i] = unflattenData((Class<T>) type, nestedMap);
        } else {
          values[i] = flatMap.get(name);
        }
      }

      Constructor<T> ctor = recordClass.getDeclaredConstructor(
          java.util.Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new)
      );
      ctor.setAccessible(true);
      return ctor.newInstance(values);

    } catch (Exception e) {
      throw new RuntimeException("Failed to unflatten map to record " + recordClass.getSimpleName(), e);
    }
  }
}
