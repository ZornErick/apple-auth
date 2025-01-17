package com.zorn.json;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;

public class JsonUtility {
    public static <T extends Record> T parseJsonToRecord(JSONObject json, Class<T> recordClass) {
        try {
            RecordComponent[] components = recordClass.getRecordComponents();
            Object[] values = new Object[components.length];

            for (int i = 0; i < components.length; i++) {
                RecordComponent component = components[i];

                String componentName = component.getName();
                Class<?> componentType = component.getType();

                if (!json.has(componentName)) {
                    values[i] = null;
                    continue;
                }

                values[i] = switch (componentType.getName()) {
                    case "java.lang.String" -> json.getString(componentName);
                    case "java.lang.Long", "long" -> json.getLong(componentName);
                    case "java.lang.Integer", "int" -> json.getInt(componentName);
                    case "java.lang.Float", "float" -> json.getFloat(componentName);
                    case "java.lang.Double", "double" -> json.getDouble(componentName);
                    case "java.lang.Boolean", "boolean" -> json.getBoolean(componentName);
                    default -> throw new UnsupportedTypeException("Tipo não suportado: " + componentType.getName());
                };
            }

            Constructor<T> constructor = recordClass.getDeclaredConstructor(
                    Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new)
            );

            return constructor.newInstance(values);
        } catch (Exception e) {
            throw new JsonConversionException("Erro ao converter JSON para " + recordClass.getSimpleName());
        }
    }
}
