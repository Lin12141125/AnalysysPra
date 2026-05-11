package reflection;

public class Student {
    private String StudentName="Alice";
    private int age=18;

    public Student() {
    }

    private Student(String name, int age) {
        this.StudentName = name;
        this.age = age;
    }

    private Student(String name) {
        this.StudentName = name;
    }

    public Student(int age) {
        this.age = age;
    }

    public String getName() {
        System.out.println("getName method is called");
        return StudentName;
    }

    public void setName(String name) {
        this.StudentName = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "Student{name='" + StudentName + "', age=" + age + '}';
    }
}
