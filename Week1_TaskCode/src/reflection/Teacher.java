package reflection;

public record Teacher(String name, int age) {
    public void callMethod(){
        System.out.println("callMethod is called");
    }
}