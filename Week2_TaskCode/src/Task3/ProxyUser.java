package Task3;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyUser {
    public static void main(String args[]){
        // User
        UserService user=new UserServiceImp();
        // User代理
        UserService proxy=(UserService) Proxy.newProxyInstance(user.getClass().getClassLoader(), user.getClass().getInterfaces(), new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println("\n方法开始执行："+method.getName());
                Object result=method.invoke(user, args);
                System.out.println("方法执行完毕："+method.getName());
                return result;
            }
        });

        // 告诉代理的内容
        proxy.saveUser("User1");
        System.out.println("\n"+proxy.method2());
    }
}
