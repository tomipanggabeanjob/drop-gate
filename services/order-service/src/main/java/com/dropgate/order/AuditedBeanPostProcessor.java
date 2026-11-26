package com.dropgate.order;

import org.springframework.aop.framework.ProxyFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class AuditedBeanPostProcessor implements BeanPostProcessor {

    private final AuditEventSink auditEventSink;
    private final String currentUser;

    public AuditedBeanPostProcessor(AuditEventSink auditEventSink, String currentUser) {
        this.auditEventSink = auditEventSink;
        this.currentUser = currentUser;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
        if (bean == null) {
            return bean;
        }

        boolean hasAuditedMethods = Arrays.stream(bean.getClass().getMethods())
                .anyMatch(method -> method.isAnnotationPresent(Audited.class));

        if (!hasAuditedMethods) {
            return bean;
        }

        ProxyFactory factory = new ProxyFactory(bean);
        factory.setProxyTargetClass(true);
        factory.addAdvice((MethodInterceptor) methodInvocation -> {
            String methodName = methodInvocation.getMethod().getName();
            String summary = summarizeArgs(methodInvocation.getArguments());
            long startNanos = System.nanoTime();
            String outcome = "SUCCESS";

            try {
                return methodInvocation.proceed();
            } catch (RuntimeException ex) {
                outcome = "ERROR";
                throw ex;
            } catch (Throwable ex) {
                outcome = "ERROR";
                throw new IllegalStateException("audited method failed", ex);
            } finally {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                auditEventSink.publish(new AuditEvent(
                        beanName,
                        methodName,
                        summary,
                        currentUser == null ? "system" : currentUser,
                        outcome,
                        durationMs,
                        Instant.now()));
            }
        });

        return factory.getProxy();
    }

    private String summarizeArgs(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return "[]";
        }

        return Arrays.stream(arguments)
                .map(arg -> arg == null ? "null" : arg.toString())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }
}
