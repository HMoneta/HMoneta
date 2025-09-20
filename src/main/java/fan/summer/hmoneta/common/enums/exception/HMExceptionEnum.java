package fan.summer.hmoneta.common.enums.exception;

/**
 * 该接口定义了异常枚举的基本结构，主要用于表示系统中可能出现的各种异常情况。
 * 每个实现此接口的枚举类型都需要提供一个错误代码和对应的错误信息。
 * 错误代码用于程序内部处理或记录日志时快速识别问题类型，而错误信息则更便于用户理解具体发生了什么错误。
 *
 * @see #getCode 获取错误代码
 * @see #getMessage 获取错误描述信息
 */
public interface HMExceptionEnum {
    String getCode();
    String getMessage();
}
