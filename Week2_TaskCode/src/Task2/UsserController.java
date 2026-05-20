package Task2;

@MyComponent
public class UsserController {

    @RequestMapping("/user")
    public String user() {
        return "<h1>User Controller</h1>";
    }
}
