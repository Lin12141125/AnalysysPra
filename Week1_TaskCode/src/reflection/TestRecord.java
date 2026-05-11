package reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;

public class TestRecord {
    public static void main(String[] args) throws Exception {
        // Teacher t1 = new Teacher("Mr.Smith", 40);
        // System.out.println(t1); // Teacher[name=Mr. Smith, age=40]
        // System.out.println("Name: " + t1.name()); // Name: Mr. Smith
        // System.out.println("Age: " + t1.age()); // Age: 40

        // 获取Class
        Class<?> c1 = Teacher.class;
        System.out.println("Class Name: " + c1.getName()); // reflection.Teacher
        System.out.println("Is Record: " + c1.isRecord()); // Is Record: true
        RecordComponent[] components = c1.getRecordComponents(); // 获取 record 组件（字段描述）
        System.out.println("Record Components:");
        for (RecordComponent component : components) {
            // System.out.println(" - " + component.getName() + ": " + component.getType());
            // System.out.println(component);

            String name = component.getName();
            Class<?> type = component.getType();
            Method accessor = component.getAccessor(); //该组件的访问器方法（getter）

            Object value = accessor.invoke(c1.getDeclaredConstructor(String.class, int.class).newInstance("Ms.Jones", 35)); // 获取实例化后该组件的值
            System.out.println(name + ": " + value + " (type: " + type + ")");
        }

        // 获取构造器-->实例化
        Constructor<?>[] cons=c1.getDeclaredConstructors();
        for (Constructor<?> con:cons){
            System.out.println(con);
        }

        Constructor<?> con=c1.getDeclaredConstructor(String.class, int.class);
        con.setAccessible(true);
        Teacher teacherRecord=(Teacher) con.newInstance("Ms.Jones", 35);
        System.out.println(teacherRecord); // Teacher[name=Ms. Jones, age=35]

        // 读取属性
        System.out.println("Name: " + c1.getMethod("name").invoke(teacherRecord)); // Ms.Jones
        System.out.println("Age: " + c1.getMethod("age").invoke(teacherRecord)); // 35

        System.out.println();
        //调用Teacher的callMethod方法
        c1.getMethod("callMethod").invoke(teacherRecord); // callMethod is called
    }
}
