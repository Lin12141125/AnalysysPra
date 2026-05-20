package Task2;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class MyIoC {

    // 全局Map，存储所有被MyComponent注解标记的类的实例，key为类的Class对象，value为实例对象
    private final Map<Class<?>, Object> beanMap = new HashMap<>();

    // 扫描basePackage，实例化所有@MyComponent类，并完成依赖注入
    public MyIoC(String basePackage) throws Exception {
        // 1. 扫描包，得到所有类
        List<Class<?>> allClasses = scanPackage(basePackage);
        // 2. 实例化所有@MyComponent类，并存储到beanMap中
        for (Class<?> clazz : allClasses) {
            if (clazz.isAnnotationPresent(MyComponent.class)) {
                Object instance = clazz.getDeclaredConstructor().newInstance(); // 创建实例
                beanMap.put(clazz, instance); // 存储到beanMap中
            }
        }
        // 3. 完成依赖注入
        for (Object bean : beanMap.values()) {
            injectDependencies(bean);
        }
    }


    private List<Class<?>> scanPackage(String packageName) throws Exception {
        List<Class<?>> allClasses = new ArrayList<>();
        // 1、包名转路径
        String path = packageName.replace(".", "/");
        // 2、获取资源路径
        ClassLoader classLoader =Thread.currentThread().getContextClassLoader();
        java.net.URL resource = classLoader.getResource(path);
        if (resource == null) {
            throw new RuntimeException("找不到包: " + packageName);
        }
        // 3、获取目录
        File directory = new File(resource.toURI());
        // 4、遍历目录
        File[] files = directory.listFiles();
        if (files == null) return allClasses;
        for (File file : files) {

            String fileName = file.getName();
            if(file.isDirectory()) { // 如果是目录，递归扫描
                allClasses.addAll(scanPackage(packageName + "." + fileName));
            } else if (fileName.endsWith(".class")) { // 只处理 .class 文件
                // 去掉 .class
                String className =fileName.substring(0, fileName.length() - 6);
                // 完整类名
                String fullClassName =packageName + "." + className;
                // 动态加载类
                Class<?> clazz = Class.forName(fullClassName);
                // 跳过接口、枚举、注解、抽象类
                if (clazz.isInterface() || clazz.isEnum() || clazz.isAnnotation() ||
                    java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }
                allClasses.add(clazz); // 添加到类列表中
            }
        }
        return allClasses;
    }


    // 为某个bean注入依赖(字段上的@MyAutowired)
    private void injectDependencies(Object bean) throws Exception {
        Field[] fields = bean.getClass().getDeclaredFields(); // 获取对象的所有声明的字段
        for (Field field : fields) {
            if (field.isAnnotationPresent(MyAutowired.class)) { // 如果字段上有@MyAutowired注解
                Object dependency = beanMap.get(field.getType()); // 从beanMap中获取依赖对象
                if (dependency == null) {
                    throw new RuntimeException("无法注入依赖，找不到类型为: " + field.getType().getName() + " 的bean");
                }
                field.setAccessible(true);
                field.set(bean, dependency);
                System.out.println("\n为 "+field.getName()+"注入依赖: "+dependency.getClass());
            }
        }
    }

    public Collection<Object> getAllBeans() {
        return beanMap.values();
    }
}