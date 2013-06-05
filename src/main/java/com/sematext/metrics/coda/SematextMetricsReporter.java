/**
 * Copyright 2013 Sematext Group, Inc.
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
package com.sematext.metrics.coda;

import com.sematext.metrics.client.AggType;
import com.sematext.metrics.client.SematextClient;
import com.sematext.metrics.client.StDatapoint;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricProcessor;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Summarizable;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * <p>Coda Metrics reporter implementation for sending metrics to
 * <a href="http://sematext.com/spm/index.html">Scalable Performance Monitoring</a></p>.
 * <p/>
 * <p>Usage:</p>
 * <pre>
 *   MetricsRegistry metrics = new MetricsRegistry();
 *
 *   SematextClient.initialize("[token]");
 *
 *   SematextMetricsReporter reporter = SematextMetricsReporter.forClient(SematextClient.client())
 *     .withPredicate(MetricPredicate.ALL)
 *     .withRegistry(metrics)
 *     .build();
 *
 *    reporter.start(1, TimeUnit.SECONDS);
 * </pre>
 */
public class SematextMetricsReporter extends AbstractPollingReporter implements MetricProcessor<List<StDatapoint>> {
  private Logger LOG = LoggerFactory.getLogger(SematextMetricsReporter.class);

  private SematextClient sematextClient;
  private MetricPredicate predicate;

  private SematextMetricsReporter(MetricsRegistry registry, MetricPredicate predicate, SematextClient client) {
    super(registry, "sematext-metrics-reporter");
    this.sematextClient = client;
    this.predicate = predicate;
  }

  @Override
  public void run() {
    List<StDatapoint> datapoints = new ArrayList<StDatapoint>();
    for (Map.Entry<String, SortedMap<MetricName, Metric>> metrics : getMetricsRegistry().groupedMetrics(predicate).entrySet()) {
      for (Map.Entry<MetricName, Metric> metric : metrics.getValue().entrySet()) {
        try {
          metric.getValue().processWith(this, metric.getKey(), datapoints);
        } catch (Exception e) {
          LOG.error("Error while processing metric.", e);
        }
      }
    }
    sematextClient.send(datapoints);
  }

  @Override
  public void processMeter(MetricName name, Metered meter, List<StDatapoint> context) throws Exception {
    StDatapoint sumDatapoint = StDatapoint.name(Utils.formatName(name))
        .value(meter.meanRate())
        .aggType(AggType.AVG).build();
    context.add(sumDatapoint);
  }

  @Override
  public void processCounter(MetricName name, Counter counter, List<StDatapoint> context) throws Exception {
    StDatapoint avgDatapoint = StDatapoint.name(Utils.formatName(name))
        .value((double) counter.count())
        .aggType(AggType.AVG).build();
    context.add(avgDatapoint);
  }

  private void processSummarizable(MetricName name, Summarizable summarizable, List<StDatapoint> context) throws Exception {
    StDatapoint minDatapoint = StDatapoint.name(Utils.formatName(name))
        .filter1(Utils.formatFilterName(AggType.MIN))
        .value(summarizable.min())
        .aggType(AggType.MIN)
        .build();

    context.add(minDatapoint);

    StDatapoint maxDatapoint = StDatapoint.name(Utils.formatName(name))
        .filter1(Utils.formatFilterName(AggType.MAX))
        .value(summarizable.max())
        .aggType(AggType.MAX)
        .build();

    context.add(maxDatapoint);

    StDatapoint avgDatapoint = StDatapoint.name(Utils.formatName(name))
        .filter1(Utils.formatFilterName(AggType.AVG))
        .value(summarizable.mean())
        .aggType(AggType.AVG)
        .build();

    context.add(avgDatapoint);
  }

  @Override
  public void processHistogram(MetricName name, Histogram histogram, List<StDatapoint> context) throws Exception {
    processSummarizable(name, histogram, context);
  }

  @Override
  public void processTimer(MetricName name, Timer timer, List<StDatapoint> context) throws Exception {
    processSummarizable(name, timer, context);
  }

  @Override
  public void processGauge(MetricName name, Gauge<?> gauge, List<StDatapoint> context) throws Exception {
    Object value = gauge.value();
    if (value instanceof Number) {
      StDatapoint avgDatapoint = StDatapoint.name(Utils.formatName(name))
          .value(((Number) value).doubleValue())
          .aggType(AggType.AVG).build();
      context.add(avgDatapoint);
    }
  }

  /**
   * Builder for Sematext Metrics Reporter.
   */
  public static class Builder {
    private SematextClient client;
    private MetricPredicate predicate;
    private MetricsRegistry registry;

    private Builder(SematextClient client) {
      this.client = client;
    }

    /**
     * Set predicate to filter metrics.
     * @param predicate predicate
     * @return builder
     */
    public Builder withPredicate(MetricPredicate predicate) {
      this.predicate = predicate;
      return this;
    }

    /**
     * Use given registry.
     * @param registry registry
     * @return registry
     */
    public Builder withRegistry(MetricsRegistry registry) {
      this.registry = registry;
      return this;
    }

    /**
     * Build reporter.
     * @return reporter
     * @throws IllegalArgumentException if {@code predicate} or {@code registry} is {@code null}.
     */
    public SematextMetricsReporter build() {
      if (predicate == null) {
        throw new IllegalArgumentException("Predicate should be defined.");
      }
      if (registry == null) {
        throw new IllegalArgumentException("Registry should be defined.");
      }
      return new SematextMetricsReporter(registry, predicate, client);
    }
  }

  /**
   * Create builder for client.
   * @param client client
   * @return builder
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   */
  public static Builder forClient(SematextClient client) {
    if (client == null) {
      throw new IllegalArgumentException("Client should be defined.");
    }
    return new Builder(client);
  }
}
