package org.etd.framework.demo.dto;

/**
 * @description: 下划线转换驼峰工具类
 * @author: yanling
 * @time: 2021/1/2
 */
public class ToHumpUtil {
    /**
     * 下划线对应的ASCII
     */
    private static final byte ASCII_UNDER_LINE = 95;

    private static final byte ASCII_MINUS_SIGN = 45;

    /**
     * 小写字母a的ASCII
     */
    private static final byte ASCII_a = 97;

    /**
     * 大写字母A的ASCII
     */
    private static final byte ASCII_A = 65;

    /**
     * 小写字母z的ASCII
     */
    private static final byte ASCII_z = 122;

    /**
     * 字母a和A的ASCII差距(a-A的值)
     */
    private static final byte ASCII_a_A = ASCII_a - ASCII_A;

    public static String toHump(String column, boolean upperCaseFirstOne) {
        byte[] bytes = changeIdx(column);
        bytes = removeUnderLine(bytes);
        return new String(bytes);
    }

    /**
     * 交换下划线和其后面字符的下标
     * 将column从下划线命名方式转换成驼峰命名方式
     * 0. 找到‘_’符号的ASCII码(95)对应的下标
     * 1. 将下划线的下标的下一个元素转换为大写字段(如果是小写字母的话)并放到下划线对应的下标
     * 2. 将下划线下标的下一个元素设置为下划线
     * 3. 返回数组
     *
     * @param column 字段名称
     */
    private static byte[] changeIdx(String column) {
        byte[] bytes = column.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            boolean isAsciiUnderLine = true;
            if (bytes[i] == ASCII_MINUS_SIGN) {
                isAsciiUnderLine = false;
            }
            if (bytes[i] == ASCII_UNDER_LINE || bytes[i] == ASCII_MINUS_SIGN) {
                if (i < bytes.length - 1) {
                    bytes[i] = toUpper(bytes[i + 1]);
                    bytes[i + 1] = isAsciiUnderLine ? ASCII_UNDER_LINE : ASCII_MINUS_SIGN;
                    i++;
                }
            }
        }
        return bytes;
    }

    /**
     * 将参数b转换为大写字母,小写字母ASCII范围(97~122)
     * 0. 判断参数是否为小写字母
     * 1. 将小写字母转换为大写字母(减去32)
     */
    private static byte toUpper(byte b) {
        if (b >= ASCII_a && b <= ASCII_z) {
            return (byte) (b - ASCII_a_A);
        }
        return b;
    }

    /**
     * 去除所有下划线
     * 0. 新创建一个数组
     * 1. 将所有非下划线字符都放入新数组中
     *
     * @param bytes 原始数组
     * @return 处理后的字节数组
     */
    private static byte[] removeUnderLine(byte[] bytes) {
        // 存放非下划线字符的数量
        int count = 0;
        for (byte b : bytes) {
            if (b == ASCII_UNDER_LINE || b == ASCII_MINUS_SIGN) {
                continue;
            }
            count++;
        }
        byte[] nBytes = new byte[count];
        count = 0;
        for (byte b : bytes) {
            if (b == ASCII_UNDER_LINE || b == ASCII_MINUS_SIGN) {
                continue;
            }
            nBytes[count] = b;
            count++;
        }
        return nBytes;
    }


    public static String upperCaseFirstOne(String str) {
        // 进行字母的ascii编码前移，效率要高于截取字符串进行转换的操作
        char[] cs = str.toCharArray();
        cs[0] -= 32;
        return String.valueOf(cs);
    }

}