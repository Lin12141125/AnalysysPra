package Task2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD) // 该注解只能用于字段
@Retention(RetentionPolicy.RUNTIME) // 运行时保留注解信息
public @interface MyAutowired {

}
