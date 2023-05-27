//package org.github.ponking66;
//
//import com.google.common.base.Charsets;
//import com.google.common.hash.HashCode;
//import com.google.common.hash.Hasher;
//import com.google.common.hash.Hashing;
//import org.junit.Test;
//
//import java.util.Random;
//
///**
// * @author pony
// * @date 2023/5/11
// */
//public class AlgorithmTest {
//
//    @Test
//    public void test() {
//        // 721b2e485683bd87c32f3c208f787a626c7397a759146a93be30e15ad3193084
//        String password = genToken(16);
//        Hasher hasher = Hashing.sha256().newHasher();
//        hasher.putString(password, Charsets.UTF_8);
//        HashCode sha256 = hasher.hash();
//        System.out.println(sha256);
//    }
//
//
//    public String genToken(int length) {
//        final String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*()_+-=[]|,./?><";
//        int len = str.length();
//        Random random = new Random();
//        StringBuilder builder = new StringBuilder();
//        while (length > builder.length()) {
//            int i = random.nextInt(len);
//            builder.append(str.charAt(i));
//        }
//        return builder.toString();
//    }
//}
