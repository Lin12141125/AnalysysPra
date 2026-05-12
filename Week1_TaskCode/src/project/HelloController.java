package project;

public class HelloController {
    
    @RequestMapping("/hello")
    public String hello() {
        return "<h1>Hello Controller</h1>";
    }

    @RequestMapping("/greet")
    public String greet() {
        return "<h1>Greetings from Hello Controller</h1>";
    }
}
