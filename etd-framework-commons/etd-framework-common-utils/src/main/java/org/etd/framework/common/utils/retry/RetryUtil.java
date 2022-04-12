package org.etd.framework.common.utils.retry;

import java.util.concurrent.Callable;

/**
 * 重试工具类
 */
public class RetryUtil {


    public static <T> RetryUtil.Result<T> invoke(Callable<T> callee, int retryNum) throws Exception {
        boolean success = true;
        Exception exception = null;
        final int retry = retryNum <= 0 ? 1 : retryNum > 5 ? 5 : retryNum;
        Object value = null;
        for (int i = 0; i < retry; i++) {
            try {
                value = callee.call();
            } catch (Exception e) {
                exception = e;
                success = false;
            }
            if (!success) {
                long millis = 100 << i;
                System.out.println("第" + i + "次重试," + millis + "毫秒后将尝试下次重试");
                Thread.sleep(millis);
            }
        }


        boolean finalSuccess = success;
        Exception finalException = exception;
        Object finalValue = value;
        RetryUtil.Result result = new Result() {
            @Override
            public boolean isSuccess() {
                return finalSuccess;
            }

            @Override
            public Exception exception() {
                return finalException;
            }

            @Override
            public Object value() {
                return finalValue;
            }
        };
        return result;
    }


    @FunctionalInterface
    public interface Executor {
        Object execute() throws Exception;
    }


    public interface Result<T> {

        boolean isSuccess();

        Exception exception();

        T value();
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Result<String> result = RetryUtil.invoke(() -> {
            throw new RuntimeException("测试重试工具类");
        }, 20);
        System.out.println(result);
    }

}
