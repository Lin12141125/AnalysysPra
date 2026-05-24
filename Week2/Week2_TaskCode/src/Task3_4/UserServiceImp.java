package Task3_4;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserServiceImp implements UserService{
    String name;
    @Override
    public void saveUser(String name) {
        this.name=name;
        System.out.println("用户"+this.name);
    }

    @Override
    public String method2() {
        System.out.println("Method2 has been executed.");
        return "Method2返回的String";
    }

}
