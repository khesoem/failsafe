/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package dev.failsafe.internal;

import dev.failsafe.RateLimitExceededException;
import dev.failsafe.RateLimiter;
import dev.failsafe.spi.ExecutionResult;
import dev.failsafe.spi.PolicyExecutor;

import java.time.Duration;

/**
 * A PolicyExecutor that handles failures according to a {@link RateLimiter}.
 *
 * @param <R> result type
 * @author Jonathan Halterman
 */
public class RateLimiterExecutor<R> extends PolicyExecutor<R> {
  private final RateLimiterImpl<R> rateLimiter;
  private final Duration timeout;

  public RateLimiterExecutor(RateLimiterImpl<R> rateLimiter, int policyIndex) {
    super(rateLimiter, policyIndex);
    this.rateLimiter = rateLimiter;
    timeout = rateLimiter.getConfig().getTimeout();
  }

  @Override
  protected ExecutionResult<R> preExecute() {
    try {
      boolean acquired = timeout == null ? rateLimiter.tryAcquirePermit() : rateLimiter.tryAcquirePermit(timeout);
      return acquired ? null : ExecutionResult.failure(new RateLimitExceededException(rateLimiter));
    } catch (InterruptedException e) {
      // Set interrupt flag
      Thread.currentThread().interrupt();
      return ExecutionResult.failure(e);
    }
  }

  @Override
  public boolean isFailure(ExecutionResult<R> result) {
    return !result.isNonResult() && result.getFailure() instanceof RateLimitExceededException;
  }
}
