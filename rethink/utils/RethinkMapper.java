/*
 * Copyright 2020 Azzerial
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rethink.utils;

import rethink.annotations.RethinkData;
import rethink.annotations.RethinkObject;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;

public final class RethinkMapper {

    private RethinkMapper() {}

    public static <T> T convert(Object object, Class<T> classType) {
        if (!classType.isAnnotationPresent(RethinkObject.class))
            throw new IllegalArgumentException("The conversion Class requires the @RethinkObject annotation.");

        /* Get the zero-arguments constructor if present */
        Constructor constructor = null;
        for (Constructor ctor : classType.getDeclaredConstructors())
            if (ctor.getParameterCount() == 0)
                constructor = ctor;

        /* Create an instance of the conversion Class */
        Object instance = null;
        try {
            if (constructor == null) { /* If no zero-arguments constructor was found, create one */
                constructor = ReflectionFactory
                    .getReflectionFactory()
                    .newConstructorForSerialization(classType, Object.class.getDeclaredConstructor(new Class[0]));
                instance = constructor.newInstance(new Object[0]);
            } else {
                boolean isAccessible = constructor.isAccessible();
                constructor.setAccessible(true);
                instance = constructor.newInstance();
                constructor.setAccessible(isAccessible);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        /* Loop through each field of the conversion Class and assign values from the (JSON) Object */
        HashMap<String, Object> json = (HashMap<String, Object>) object;
        for (Field field : classType.getDeclaredFields()) {
            if (!field.isAnnotationPresent(RethinkData.class))
                continue;
            boolean isAccessible = field.isAccessible();
            field.setAccessible(true);

            /* Move to the proper depth of the JSON */
            HashMap<String, Object> map = json;
            String[] fromPath = field.getAnnotation(RethinkData.class).path();
            if (fromPath.length != 0)
                for (String path : fromPath)
                    map = (HashMap<String, Object>) map.get(path);

            /* Get the JSON element based on the explicit key, if present, or default to the field name */
            String key = field.getAnnotation(RethinkData.class).key();
            if (key.isEmpty())
                key = field.getName();
            if (map == null || !map.containsKey(key)) {
                StringBuilder builder = new StringBuilder();
                builder.append("Invalid key \"")
                       .append(key)
                       .append("\"");
                if (fromPath.length != 0)
                    builder.append(" in path [\"")
                           .append(String.join("\", \"", fromPath))
                           .append("\"]");
                builder.append(" in object ")
                       .append(json);
                throw new IllegalArgumentException(builder.toString());
            }

            /* Cast the field in the specific cast type */
            /* Could use field.getType() but chose to use annotation to make it explicit */
            Object value = map.get(key);
            Class castType = field.getAnnotation(RethinkData.class).cast();
            if (!castType.equals(void.class))
                value = convertDataType(value, castType);

            /* In case of the field type is annotated with a @RethinkObject, instantiate properly the object */
            if (field.getType().isAnnotationPresent(RethinkObject.class))
                value = convert(field.getType().getAnnotation(RethinkObject.class).asRoot() ? json : value, field.getType());

            /* Assign the object to the field */
            try {
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            field.setAccessible(isAccessible);
        }

        return (T) instance;
    }

    private static Object convertDataType(Object object, Class castType) {
        if (object instanceof Number) {
            if (castType.equals(byte.class) || castType.equals(Byte.class))
                return ((Number) object).byteValue();
            if (castType.equals(double.class) || castType.equals(Double.class))
                return ((Number) object).doubleValue();
            if (castType.equals(float.class) || castType.equals(Float.class))
                return ((Number) object).floatValue();
            if (castType.equals(int.class) || castType.equals(Integer.class))
                return ((Number) object).intValue();
            if (castType.equals(long.class) || castType.equals(Long.class))
                return ((Number) object).longValue();
            if (castType.equals(short.class) || castType.equals(Short.class))
                return ((Number) object).shortValue();
            if (castType.equals(CharSequence.class) || castType.equals(String.class))
                return object.toString();
            if (castType.equals(boolean.class) || castType.equals(Boolean.class))
                return ((Number) object).intValue() == 1 ? true : false;
        }
        if (object instanceof Boolean) {
            if (castType.equals(CharSequence.class) || castType.equals(String.class))
                return ((Boolean) object).booleanValue() ? "true" : "false";
            if (castType.equals(byte.class) || castType.equals(double.class) || castType.equals(float.class)
                || castType.equals(int.class) || castType.equals(long.class) || castType.equals(short.class)
                || Number.class.isAssignableFrom(castType))
                return ((Boolean) object).booleanValue() ? 1 : 0;
        }
        if (object instanceof String) {
            if (castType.equals(boolean.class) || castType.equals(Boolean.class)) {
                if (!(((String) object).equalsIgnoreCase("true") && ((String) object).equalsIgnoreCase("false")))
                    return object;
                return ((String) object).equalsIgnoreCase("true") ? true : false;
            }
            if (castType.equals(byte.class) || castType.equals(Byte.class))
                return Byte.parseByte((String) object);
            if (castType.equals(double.class) || castType.equals(Double.class))
                return Double.parseDouble((String) object);
            if (castType.equals(float.class) || castType.equals(Float.class))
                return Float.parseFloat((String) object);
            if (castType.equals(int.class) || castType.equals(Integer.class))
                return Integer.parseInt((String) object);
            if (castType.equals(long.class) || castType.equals(Long.class))
                return Long.parseLong((String) object);
            if (castType.equals(short.class) || castType.equals(Short.class))
                return Short.parseShort((String) object);
        }
        if (object instanceof ArrayList && castType.isArray()) {
            Object array = Array.newInstance(castType.getComponentType(), ((ArrayList) object).size());

            for (int i = 0; i != ((ArrayList) object).size(); i += 1)
                Array.set(array, i, convertDataType(((ArrayList) object).get(i), castType.getComponentType()));
            return array;
        }
        return object;
    }

}
