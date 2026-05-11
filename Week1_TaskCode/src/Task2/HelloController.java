package Task2;

public class HelloController {
    
    @RequestMapping("/hello")
    public String hello() {
        return "Hello Controller";
    }

    @RequestMapping("/greet")
    public String greet() {
        return "Greetings from Hello Controller";
    }
}
