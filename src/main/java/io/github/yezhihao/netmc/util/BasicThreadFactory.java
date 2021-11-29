package io.github.yezhihao.netmc.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicThreadFactory implements ThreadFactory {

    private final AtomicInteger threadCounter;
    private final ThreadFactory wrappedFactory;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private final String namingPattern;
    private final Integer priority;
    private final Boolean daemon;

    private BasicThreadFactory(final Builder builder) {
        if (builder.wrappedFactory == null) {
            wrappedFactory = Executors.defaultThreadFactory();
        } else {
            wrappedFactory = builder.wrappedFactory;
        }
        namingPattern = builder.namingPattern;
        priority = builder.priority;
        daemon = builder.daemon;
        uncaughtExceptionHandler = builder.exceptionHandler;
        threadCounter = new AtomicInteger();
    }

    public final ThreadFactory getWrappedFactory() {
        return wrappedFactory;
    }

    public final String getNamingPattern() {
        return namingPattern;
    }

    public final Boolean getDaemonFlag() {
        return daemon;
    }

    public final Integer getPriority() {
        return priority;
    }

    public final Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler;
    }

    public long getThreadCount() {
        return threadCounter.get();
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        final Thread thread = getWrappedFactory().newThread(runnable);
        initializeThread(thread);
        return thread;
    }

    private void initializeThread(final Thread thread) {
        if (getNamingPattern() != null) {
            final Integer count = Integer.valueOf(threadCounter.incrementAndGet());
            thread.setName(String.format(getNamingPattern(), count));
        }
        if (getUncaughtExceptionHandler() != null) {
            thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler());
        }
        if (getPriority() != null) {
            thread.setPriority(getPriority().intValue());
        }
        if (getDaemonFlag() != null) {
            thread.setDaemon(getDaemonFlag().booleanValue());
        }
    }

    public static class Builder {
        private ThreadFactory wrappedFactory;
        private Thread.UncaughtExceptionHandler exceptionHandler;
        private String namingPattern;
        private Integer priority;
        private Boolean daemon;

        public Builder wrappedFactory(final ThreadFactory factory) {
            wrappedFactory = factory;
            return this;
        }

        public Builder namingPattern(final String pattern) {
            namingPattern = pattern;
            return this;
        }

        public Builder daemon(final boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public Builder priority(final int priority) {
            this.priority = priority;
            return this;
        }

        public Builder uncaughtExceptionHandler(final Thread.UncaughtExceptionHandler handler) {
            exceptionHandler = handler;
            return this;
        }

        public void reset() {
            wrappedFactory = null;
            exceptionHandler = null;
            namingPattern = null;
            priority = null;
            daemon = null;
        }

        public BasicThreadFactory build() {
            final BasicThreadFactory factory = new BasicThreadFactory(this);
            reset();
            return factory;
        }
    }
}