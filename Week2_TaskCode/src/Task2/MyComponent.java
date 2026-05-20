package Task2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) // 该注解只能用于类
@Retention(RetentionPolicy.RUNTIME) // 运行时保留注解信息，供反射读取
public @interface MyComponent {

}
