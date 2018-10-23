/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.util.impl

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import ddf.catalog.source.FederatedSource
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SourcePollerSpec extends Specification {

    private ExecutorService availabilityChecksThreadPool

    private ScheduledExecutorService pollAllSourcesThreadPool

    private SourcePoller sourcePoller

    def setup() {
        availabilityChecksThreadPool = Mock(ExecutorService)
        pollAllSourcesThreadPool = Mock(ScheduledExecutorService)
        sourcePoller = new SourcePoller(availabilityChecksThreadPool, pollAllSourcesThreadPool)
    }

    def cleanup() {
        sourcePoller.destroy()
    }

    @Unroll
    def 'test create #expectedSourceStatus FederatedSource'() {
        given:
        final FederatedSource mockFederatedSource = Mock(FederatedSource) {
            isAvailable() >> availability
            getId() >> 'mockFederatedSourceId'
        }
        ListenableFuture<SourceStatus> future = Futures.immediateFuture(mockFederatedSource.isAvailable() ? SourceStatus.AVAILABLE : SourceStatus.UNAVAILABLE)
        pollAllSourcesThreadPool.schedule(_ as Runnable, _ as long, _ as TimeUnit) >> { Runnable r -> r }
        availabilityChecksThreadPool.submit(_ as Callable) >> future

        when:
        sourcePoller.setFederatedSources([mockFederatedSource])
        sourcePoller.pollAllSources()

        then:
        sourcePoller.getSourceStatus(mockFederatedSource) == expectedSourceStatus

        where:
        availability || expectedSourceStatus
        true         || SourceStatus.AVAILABLE
        false        || SourceStatus.UNAVAILABLE
    }

    def 'test bind FederatedSource availability timeout'() {
        given:
        final FederatedSource mockFederatedSource = Mock(FederatedSource) {
            isAvailable() >> true
            getId() >> 'mockFederatedSourceId'
        }
        ListenableFuture<SourceStatus> future = Futures.immediateFailedFuture(new TimeoutException())
        pollAllSourcesThreadPool.schedule(_ as Runnable, _ as long, _ as TimeUnit) >> { Runnable r -> r }
        availabilityChecksThreadPool.submit(_ as Callable) >> future

        when:
        sourcePoller.setFederatedSources([mockFederatedSource])
        sourcePoller.pollAllSources()

        then:
        sourcePoller.getSourceStatus(mockFederatedSource) == SourceStatus.TIMEOUT
    }
}