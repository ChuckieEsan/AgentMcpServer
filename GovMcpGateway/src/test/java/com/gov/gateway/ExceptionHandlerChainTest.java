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

import java.util.HashMap;
import java.util.Map;

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
        @DisplayName("Dubbo 限流异常应归类为 SYSTEM_ERROR")
        void dubboLimitExceed_shouldBeSystemError() {
            RpcException e = new RpcException("Rate limit exceeded");
            ToolError result = chain.process(e, null);

            assertEquals("SYSTEM_ERROR", result.getErrorCategory());
            assertFalse(result.isRetryable());
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
        @DisplayName("result.success=false 应归类为 BUSINESS_ERROR")
        void resultSuccessFalse_shouldBeBusinessError() {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "工单不存在");

            ToolError error = chain.process(null, result);

            assertEquals("BUSINESS_ERROR", error.getErrorCategory());
            assertEquals("工单不存在", error.getMessage());
            assertFalse(error.isRetryable());
        }

        @Test
        @DisplayName("result.success=true 不应归类为 BUSINESS_ERROR")
        void resultSuccessTrue_shouldNotBeBusinessError() {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", "some data");

            ToolError error = chain.process(null, result);

            // 应该继续传递到 SystemExceptionHandler
            assertEquals("SYSTEM_ERROR", error.getErrorCategory());
        }

        @Test
        @DisplayName("result 为 null 不应归类为 BUSINESS_ERROR")
        void resultNull_shouldNotBeBusinessError() {
            ToolError error = chain.process(new RuntimeException("some error"), null);

            assertEquals("SYSTEM_ERROR", error.getErrorCategory());
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
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "工单不存在");

            ToolError error = chain.process(null, result);

            assertFalse(error.isRetryable());
        }

        @Test
        @DisplayName("CLIENT_ERROR 的 retryable 应为 false")
        void client_shouldNotBeRetryable() {
            org.apache.dubbo.rpc.service.GenericException e =
                new org.apache.dubbo.rpc.service.GenericException("java.lang.IllegalArgumentException", "Invalid");
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