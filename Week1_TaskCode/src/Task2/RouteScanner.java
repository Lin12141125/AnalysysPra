package Task2;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class RouteScanner {
    // 存放 URL 到 方法 和 所属实例 的映射
    private Map<String, RouteInfo> routeMap = new HashMap<>();

    public void registerController(Object controller) throws Exception{ // 读入class实例
        Class<?> clazz = controller.getClass(); // 获取class实例的Class对象
        Method[] methods = clazz.getDeclaredMethods(); // 获取Class对象的所有方法
        for (Method method : methods) { // 遍历所有方法
            RequestMapping anno = method.getAnnotation(RequestMapping.class);
            if (anno != null) { // 如果方法上有@RequestMapping注解
                String url = anno.value(); // url=RequestMapping注解的value值-->该方法对应的url路径
                routeMap.put(url, new RouteInfo(controller, method)); // 储存在映射中，<路径，(class实例,方法)>
                System.out.println("\n"+"Controller: "+controller.getClass().getSimpleName());
                System.out.println("注册路由: " + url + " -> " + method.getName()); // 注册路由: url路径 -> 对应方法名

                // 执行
                String html=execute(url);
                System.out.println("执行: "+html); // 执行结果: 方法返回的字符串
            }
        }
    }

    static class RouteInfo { //储存路由信息，包括所属控制器实例和方法
        Object controller;
        Method method;
        RouteInfo(Object controller, Method method) {
            this.controller = controller;
            this.method = method;
        }
    }

    public String execute(String url) throws Exception { // 执行路由
        RouteInfo info = routeMap.get(url); // 从映射中获取对应url的RouteInfo(class实例,方法)
        if (info == null) return "404 Not Found"; // 如果没有找到对应的RouteInfo，返回404错误
        String result = (String) info.method.invoke(info.controller); // 反射调用方法，执行对应的控制器方法
        return result; // 返回方法执行结果
    }

    public static void main(String args[]) throws Exception {
        RouteScanner scanner = new RouteScanner();
        scanner.scanPackage("Task2");
    }


    public void scanPackage(String packageName) throws Exception {
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
        if (files == null) return;
        for (File file : files) {

            String fileName = file.getName();
            // 只处理 .class 文件
            if (fileName.endsWith(".class")) {
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

                Object controller;
                // 创建控制器实例
                try {
                    controller = clazz.getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException e) {
                    System.err.println("\n"+"跳过 " + className + "：缺少无参构造器，无法自动注册");
                    continue;
                }
                registerController(controller); // 注册控制器中的带注解方法
            }
        }
    }
}
