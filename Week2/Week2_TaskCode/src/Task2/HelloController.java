package Task2;

@MyComponent // 标记这是一个组件类，告诉 IoC 容器管理这个类
public class HelloController {

    @MyAutowired // 注入 GreetService 依赖
    private GreetService greetService;
    
    @RequestMapping("/hello")
    public String hello() {
        return "<h1>Hello Controller</h1>";
    }

    @RequestMapping("/greet")
    public String greet() {
        return greetService.getGreeting();
    }
}
