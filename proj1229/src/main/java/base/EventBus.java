package base;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class EventBus {
    private static class Handler {
        final Object target;
        final BiConsumer<Object, Object> invoker;

        Handler(Object target, BiConsumer<Object, Object> invoker) {
            this.target = target;
            this.invoker = invoker;
        }
    }

    private static final Map<Class<?>, List<Handler>> registry = new ConcurrentHashMap<>();

    public static void subscribe(Object subscriber) {
        Class<?> clazz = subscriber.getClass();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                try {
                    method.setAccessible(true);
                    Class<?> eventType = method.getParameterTypes()[0];
                    MethodHandle methodHandle = lookup.unreflect(method);
                    CallSite site = LambdaMetafactory.metafactory(
                            lookup,
                            "accept",
                            MethodType.methodType(BiConsumer.class),
                            MethodType.methodType(void.class, Object.class, Object.class),
                            methodHandle, MethodType.methodType(void.class, clazz, eventType)
                    );
                    BiConsumer<Object, Object> invoker = (BiConsumer<Object, Object>) site.getTarget().invokeExact();
                    registry.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(new Handler(subscriber, invoker));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void registerAll(String packageName) {
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages(packageName).scan()) {
            ClassInfoList classes = scanResult.getClassesWithMethodAnnotation(Subscribe.class.getName());
            for (ClassInfo classInfo : classes) {
                try {
                    Class<?> clazz = classInfo.loadClass();
                    if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        subscribe(instance);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void publish(Object event) {
        List<Handler> handlers = registry.get(event.getClass());
        if (handlers != null) {
            for (Handler handler : handlers) {
                handler.invoker.accept(handler.target, event);
            }
        }
    }
}