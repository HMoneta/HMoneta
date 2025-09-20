package fan.summer.hmoneta.common.exception;

import fan.summer.hmoneta.common.enums.exception.HMExceptionEnum;

/**
 * HMException 是一个继承自 RuntimeException 的异常类，用于在系统中抛出带有特定错误代码和消息的异常。
 * 该异常通过关联一个实现了 HMExceptionEnum 接口的枚举值来携带具体的错误信息。这种设计使得可以方便地根据预定义的错误类型进行异常处理，
 * 同时也保证了错误信息的一致性和易于管理性。
 *
 * @param e 构造函数接受一个实现 HMExceptionEnum 接口的枚举实例作为参数，该枚举实例包含了异常的错误代码和描述信息。
 * @method getE 返回与当前异常关联的 HMExceptionEnum 实例。
 * @method fillInStackTrace 覆写了默认的 fillInStackTrace 方法，以优化性能。此方法直接返回当前异常对象本身而不执行堆栈跟踪填充操作。
 */
public class HMException extends RuntimeException{

    private final HMExceptionEnum e;

    public HMException(HMExceptionEnum e) {
        super(e.getMessage()); //调用父类构造函数设置消息
        this.e = e;
    }
    public HMExceptionEnum getE() {
        return e;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
