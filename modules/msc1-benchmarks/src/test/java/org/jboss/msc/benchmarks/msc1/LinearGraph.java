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

import java.util.concurrent.CountDownLatch;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
final class LinearGraph {

    private static volatile Throwable failure;

    static long benchmark (final ServiceContainer container, final ServiceController.Mode mode,
            final CountingService service, final int servicesCount, final int threadsCount) throws InterruptedException {
        final int range = servicesCount / threadsCount;
        final CountDownLatch threadsInitializedSignal = new CountDownLatch(threadsCount);
        final CountDownLatch runBenchmarkSignal = new CountDownLatch(1);
        final CountDownLatch threadsFinishedSignal = new CountDownLatch(threadsCount);
        int leftClosedIntervalIndex = 0, rightOpenIntervalIndex = range + (servicesCount % threadsCount);
        new Thread(new InstallTask(threadsInitializedSignal, runBenchmarkSignal, threadsFinishedSignal,
                leftClosedIntervalIndex, rightOpenIntervalIndex, container, mode, service)).start();
        for (int i = 1; i < threadsCount; i++) {
            leftClosedIntervalIndex = rightOpenIntervalIndex;
            rightOpenIntervalIndex += range;
            new Thread(new InstallTask(threadsInitializedSignal, runBenchmarkSignal, threadsFinishedSignal,
                    leftClosedIntervalIndex, rightOpenIntervalIndex, container, mode, service)).start();
        }
        threadsInitializedSignal.await();
        final long startTime = System.nanoTime();
        runBenchmarkSignal.countDown();
        threadsFinishedSignal.await();
        container.awaitStability();
        return failure == null ? System.nanoTime() - startTime : 0;
    }

    private static final class InstallTask implements Runnable {
        private final CountDownLatch threadsInitializedSignal;
        private final CountDownLatch runBenchmarkSignal;
        private final CountDownLatch threadsFinishedSignal;
        private final int leftClosedIntervalIndex;
        private final int rightOpenIntervalIndex;
        private final ServiceContainer container;
        private final ServiceController.Mode mode;
        private final CountingService service;

        private InstallTask(final CountDownLatch threadsInitializedSignal, final CountDownLatch runBenchmarkSignal, final CountDownLatch threadsFinishedSignal,
                final int leftClosedIntervalIndex, final int rightOpenIntervalIndex, final ServiceContainer container,
                final ServiceController.Mode mode, final CountingService service) {
            this.threadsInitializedSignal = threadsInitializedSignal;
            this.runBenchmarkSignal = runBenchmarkSignal;
            this.threadsFinishedSignal = threadsFinishedSignal;
            this.leftClosedIntervalIndex = leftClosedIntervalIndex;
            this.rightOpenIntervalIndex = rightOpenIntervalIndex;
            this.container = container;
            this.mode = mode;
            this.service = service;
        }

        public void run() {
            threadsInitializedSignal.countDown();
            while (true) try {runBenchmarkSignal.await(); break; } catch (Exception ignore) {}
            try {
                ServiceBuilder builder;
                for (int i = leftClosedIntervalIndex; i < rightOpenIntervalIndex; i++) {
                    builder = container.addService(ServiceName.of("" + i), service);
                    builder.setInitialMode(mode);
                    if (i != rightOpenIntervalIndex - 1) {
                        builder.addDependency(ServiceName.of("" + (i + 1)));
                    }
                    builder.install();
                }
            } catch (Throwable t) {
                failure = t;
                throw t;
            } finally {
                threadsFinishedSignal.countDown();
            }
        }
    }
}