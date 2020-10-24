package com.azoraqua.auto;

import com.azoraqua.auto.annotation.Autowired;
import com.azoraqua.auto.annotation.Bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * This file is part of the QAuto project.
 * Copyright (C) 2020, Ronald Bunk ("Azoraqua")
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
public final class ClassScanner {
    private final Map<String, Method> beans = new LinkedHashMap<>();
    private final Map<Field, Method> wireds = new LinkedHashMap<>();
    private File file;

    public ClassScanner() {
        try {
            this.file = new File(ClassScanner.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not initialize ClassScanner ", e);
        }
    }

    public synchronized void scan() {
        if (file.exists() && file.isFile() && file.getName().endsWith(".jar")) {
            try (final JarInputStream input = new JarInputStream(new FileInputStream(file))) {
                JarEntry entry;

                while ((entry = input.getNextJarEntry()) != null) {
                    if (entry.getName().endsWith(".class")) {
                        final Class<?> clazz = Class.forName(entry.getName().replace(File.separator, ".").replace(".class", ""));

                        beans:
                        {
                            for (Method method : clazz.getDeclaredMethods()) {
                                if (method.isAnnotationPresent(Bean.class)) {
                                    beans.putIfAbsent(method.getReturnType().getSimpleName(), method);
                                }
                            }
                        }

                        wireds:
                        {
                            for (Field field : clazz.getDeclaredFields()) {
                                if (field.isAnnotationPresent(Autowired.class)) {
                                    if (beans.containsKey(field.getType().getSimpleName())) {
                                        wireds.putIfAbsent(field, beans.get(field.getType().getSimpleName()));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException("Failed scanning", e);
            }
        } else {
            throw new IllegalStateException("Jar is not found or invalid.");
        }
    }

    @SuppressWarnings("deprecation")
    public synchronized void process() {
        wireds.forEach((field, method) -> {
            boolean accessible = field.isAccessible(); // Deprecated but should be fine for now.

            if (!accessible) {
                field.setAccessible(true);
            }

            // TODO: Allow non-static usage.
            try {
                final Object obj = method.invoke(null);

                field.set(null, obj);
                field.setAccessible(accessible);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Cannot invoke bean-method", e);
            }
        });
    }
}
