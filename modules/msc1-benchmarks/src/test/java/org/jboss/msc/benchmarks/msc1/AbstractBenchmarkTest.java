/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.msc.benchmarks.msc1;

import static org.jboss.msc.benchmarks.framework.BenchmarksConfig.*;

import java.util.concurrent.TimeUnit;

import org.jboss.msc.benchmarks.framework.ServiceInvocationStatistics;
import org.jboss.msc.service.ServiceContainer;
import org.junit.After;
import org.junit.Before;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class AbstractBenchmarkTest {

    static ServiceContainer container;
    static ServiceInvocationStatistics statistics;
    static CountingService service;

    @Before
    public void setUp() throws Exception {
        container = ServiceContainer.Factory.create(MSC_THREADS_COUNT, 30L, TimeUnit.SECONDS);
        statistics = new ServiceInvocationStatistics();
        service = new CountingService(statistics);
    }

    @After
    public void tearDown() throws Exception {
        final long startTime = System.nanoTime();
        container.shutdown();
        container.awaitTermination();
        final long nanoseconds = System.nanoTime() - startTime;
        System.out.println("Shutdown time: " + toMillis(nanoseconds));
    }

    protected static String toMillis(final long nanoseconds) {
        return nanoseconds == 0 ? "N/A" : "" + (nanoseconds / 1000000);
    }

}
