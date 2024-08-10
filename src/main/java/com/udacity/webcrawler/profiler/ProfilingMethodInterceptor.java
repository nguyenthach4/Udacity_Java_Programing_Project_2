package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;

    private final Object delegate;

    private final ProfilingState profilingState;

    private final ZonedDateTime zonedDateTime;


    // TODO: You will need to add more instance fields and constructor arguments to this class.
    ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState profilingState,
                               ZonedDateTime zonedDateTime) {
        this.clock = Objects.requireNonNull(clock);
        this.delegate = delegate;
        this.profilingState = profilingState;
        this.zonedDateTime = zonedDateTime;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // TODO: This method interceptor should inspect the called method to see if it is a profiled
        //       method. For profiled methods, the interceptor should record the start time, then
        //       invoke the method using the object that is being profiled. Finally, for profiled
        //       methods, the interceptor should record how long the method call took, using the
        //       ProfilingState methods.
        Object invokedObject;
        Instant startTime = null;
        try {
            // print implicit argument
            System.out.print(clock);
            System.out.print(delegate);
            System.out.print(profilingState);
            System.out.print(zonedDateTime);


            if (Objects.nonNull(method.getAnnotation(Profiled.class))) {
                System.out.print(method.getAnnotation(Profiled.class));
                startTime = clock.instant();
            }
            // print method name
            System.out.print("." + method.getName() + "(");
            // print explicit arguments
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    System.out.print(args[i]);
                    if (i < args.length - 1) System.out.println(", ");
                }
            }
            System.out.println(")");
            invokedObject = method.invoke(delegate, args);
        } catch (InvocationTargetException invocationTargetEx) {
            throw invocationTargetEx.getTargetException();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (Objects.nonNull(method.getAnnotation(Profiled.class))) {
                profilingState.record(this.delegate.getClass(), method,
                        Duration.between(startTime, clock.instant()));
            }
        }
        return invokedObject;
    }
}
