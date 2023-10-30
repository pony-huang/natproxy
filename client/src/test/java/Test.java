/**
 * @author pony
 * @date 2023/8/10
 */
public class Test {

    public static void main(String[] args) {
        StringBuffer  b1 = new StringBuffer("good ");
        StringBuffer  b2 = new StringBuffer("bad ");

        change(b1,b2);

        System.out.println(b1.toString());
        System.out.println(b2.toString());
    }

    private static void change(StringBuffer b1, StringBuffer b2) {
        b2 = b1;

        String helloWorld = new String("hellpo");

        b1 = new StringBuffer("new world");

        b2.append("new world");


    }
}
