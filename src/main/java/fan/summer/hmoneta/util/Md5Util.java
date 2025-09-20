package fan.summer.hmoneta.util;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Md5Util类提供了一组用于生成MD5摘要的方法。它支持对字符串进行加盐处理后再进行MD5加密，以及生成随机盐值。
 */
public class Md5Util {
    /**
     * 生成给定字符串的MD5摘要，并支持加盐处理。
     *
     * @param source 待加密的原始字符串
     * @param salt   用于混淆的整数盐值
     * @return 返回经过MD5加密后的16进制字符串形式的摘要
     */
    public static String md5Digest(String source, Integer salt) {
        try {
            // 对传入的source进行加盐混淆
            char[] tmp = source.toCharArray();
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = (char) (tmp[i] + salt);
            }
            String target = new String(tmp);
            // 生成MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(target.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 生成一个随机的整数盐值。
     *
     * 该方法首先从预定义的整数数组中随机选择一个元素作为基础值，然后加上0到999之间的一个随机整数来生成最终的盐值。
     * 生成的盐值可以用于字符串加密等场景中的混淆处理。
     *
     * @return 返回一个整型的随机盐值
     */
    public static int saltGenerator() {
        Random rand = new Random();
        int[] tmp = {12, 50, 79, 23, 54, 51, 38, 56, 36, 58};
        int hearder = tmp[rand.nextInt(tmp.length)];
        return hearder + rand.nextInt(1000);
    }
}
