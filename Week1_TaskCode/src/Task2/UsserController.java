package Task2;

public class UsserController {

    @RequestMapping("/user")
    public String user() {
        return "User Controller";
    }
}
