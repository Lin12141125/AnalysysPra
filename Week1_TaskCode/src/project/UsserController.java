package project;

public class UsserController {

    @RequestMapping("/user")
    public String user() {
        return "<h1>User Controller</h1>";
    }
}
