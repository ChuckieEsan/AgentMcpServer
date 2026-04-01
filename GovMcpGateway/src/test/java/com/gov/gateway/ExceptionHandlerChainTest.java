package com.gov.gateway;

import com.gov.gateway.core.dto.ToolError;
import com.gov.gateway.exception.ToolExceptionHandlerChain;
import com.gov.gateway.exception.handler.BusinessExceptionHandler;
import com.gov.gateway.exception.handler.DubboRpcExceptionHandler;
import com.gov.gateway.exception.handler.GenericExceptionHandler;
import com.gov.gateway.exception.handler.SystemExceptionHandler;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.SocketException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异常处理责任链测试
 */
@DisplayName("异常处理责任链测试")
class ExceptionHandlerChainTest {

    private ToolExceptionHandlerChain chain;

    @BeforeEach
    void setUp() {
        chain = new ToolExceptionHandlerChain(java.util.List.of(
            new DubboRpcExceptionHandler(),
            new GenericExceptionHandler(),
            new BusinessExceptionHandler(),
            new SystemExceptionHandler()
        ));
    }

    @Nested
    @DisplayName("DubboRpcExceptionHandler - Dubbo框架异常")
    class DubboRpcTests {

        @Test
        @DisplayName("Dubbo 超时应归类为 TRANSIENT_ERROR")
        void dubboTimeout_shouldBeTransient() {
            RpcException e = new RpcException(RpcException.TIMEOUT_EXCEPTION, "Timeout waiting for response");
            ToolError result = chain.process(e, null);

            assertEquals("TRANSIENT_ERROR", result.getErrorCategory());
            assertTrue(result.isRetryable());
        }

        @Test
        @DisplayName("Dubbo 网络异常应归类为 TRANSIENT_ERROR")
        void dubboNetwork_shouldBeTransient() {
            RpcException e = new RpcException(RpcException.NETWORK_EXCEPTION, "Network error");
            ToolError result = chain.process(e, null);

            assertEquals("TRANSIENT_ERROR", result.getErrorCategory());
            assertTrue(result.isRetryable());
        }

        @Test
        @DisplayName("Dubbo 限流异常应归类为 TRANSIENT_ERROR")
        void dubboLimitExceed_shouldBeTransient() {
            RpcException e = new RpcException("Rate limit exceeded");
            ToolError result = chain.process(e, null);

            assertEquals("TRANSIENT_ERROR", result.getErrorCategory());
            assertTrue(result.isRetryable());
        }

        @Test
        @DisplayName("Dubbo 无提供者应归类为 SYSTEM_ERROR")
        void dubboNoInvoker_shouldBeSystemError() {
            RpcException e = new RpcException("No provider available");
            ToolError result = chain.process(e, null);

            assertEquals("SYSTEM_ERROR", result.getErrorCategory());
            assertFalse(result.isRetryable());
        }
    }

    @Nested
    @DisplayName("GenericExceptionHandler - 解包GenericException")
    class GenericExceptionTests {

        @Test
        @DisplayName("GenericException 里面是 IllegalArgumentException 应归类为 CLIENT_ERROR")
        void genericIllegalArgument_shouldBeClientError() {
            org.apache.dubbo.rpc.service.GenericException e =
                new org.apache.dubbo.rpc.service.GenericException("java.lang.IllegalArgumentException", "Invalid param");
            ToolError result = chain.process(e, null);

            assertEquals("CLIENT_ERROR", result.getErrorCategory());
            assertFalse(result.isRetryable());
        }

        @Test
        @DisplayName("GenericException 里面有业务关键字应归类为 BUSINESS_ERROR")
        void genericBusiness_shouldBeBusinessError() {
            org.apache.dubbo.rpc.service.GenericException e =
                new org.apache.dubbo.rpc.service.GenericException("com.gov.mock.exception.BizException", "Order not found");
            ToolError result = chain.process(e, null);

            assertEquals("BUSINESS_ERROR", result.getErrorCategory());
            assertFalse(result.isRetryable());
        }

        @Test
        @DisplayName("GenericException 里面有 NPE 应归类为 SYSTEM_ERROR")
        void genericNPE_shouldBeSystemError() {
            org.apache.dubbo.rpc.service.GenericException e =
                new org.apache.dubbo.rpc.service.GenericException("java.lang.NullPointerException", "Null pointer");
            ToolError result = chain.process(e, null);

            assertEquals("SYSTEM_ERROR", result.getErrorCategory());
            assertFalse(result.isRetryable());
        }
    }

    @Nested
    @DisplayName("BusinessExceptionHandler - 业务错误")
    class BusinessErrorTests {

        @Test
        @DisplayName("包含'不存在'关键字应归类为 BUSINESS_ERROR")
        void notFound_shouldBeBusinessError() {
            RuntimeException e = new RuntimeException("Order not found");
            ToolError result = chain.process(e, null);

            assertEquals("BUSINESS_ERROR", result.getErrorCategory());
            assertFalse(result.isRetryable());
        }

        @Test
        @DisplayName("包含'实名等级'关键字应归类为 BUSINESS_ERROR")
        void authLevel_shouldBeBusinessError() {
            RuntimeException e = new RuntimeException("Auth level insufficient, require L3");
            ToolError result = chain.process(e, null);

            assertEquals("BUSINESS_ERROR", result.getErrorCategory());
            assertFalse(result.isRetryable());
        }
    }

    @Nested
    @DisplayName("SystemExceptionHandler - 系统错误（兜底）")
    class SystemErrorTests {

        @Test
        @DisplayName("未知异常应归类为 SYSTEM_ERROR")
        void unknownException_shouldBeSystemError() {
            NullPointerException e = new NullPointerException("Something is null");
            ToolError result = chain.process(e, null);

            assertEquals("SYSTEM_ERROR", result.getErrorCategory());
            assertFalse(result.isRetryable());
        }
    }

    @Nested
    @DisplayName("retryable 标志验证")
    class RetryableTests {

        @Test
        @DisplayName("TRANSIENT_ERROR 的 retryable 应为 true")
        void transient_shouldBeRetryable() {
            RpcException e = new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout");
            ToolError result = chain.process(e, null);

            assertTrue(result.isRetryable());
        }

        @Test
        @DisplayName("BUSINESS_ERROR 的 retryable 应为 false")
        void business_shouldNotBeRetryable() {
            RuntimeException e = new RuntimeException("Order not found");
            ToolError result = chain.process(e, null);

            assertFalse(result.isRetryable());
        }

        @Test
        @DisplayName("CLIENT_ERROR 的 retryable 应为 false")
        void client_shouldNotBeRetryable() {
            IllegalArgumentException e = new IllegalArgumentException("Invalid");
            ToolError result = chain.process(e, null);

            assertFalse(result.isRetryable());
        }

        @Test
        @DisplayName("SYSTEM_ERROR 的 retryable 应为 false")
        void system_shouldNotBeRetryable() {
            RuntimeException e = new RuntimeException("Unknown error");
            ToolError result = chain.process(e, null);

            assertFalse(result.isRetryable());
        }
    }
}