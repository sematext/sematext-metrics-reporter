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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.sematext.metrics.client.AggType;
import com.sematext.metrics.client.SematextClient;
import com.sematext.metrics.client.StDatapoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * <p>Coda Metrics reporter implementation for sending metrics to
 * <a href="http://sematext.com/spm/index.html">Scalable Performance Monitoring</a></p>.
 *
 * <p>Usage:</p>
 * <pre>
 *   MetricRegistry metrics = new MetricRegistry();
 *
 *   SematextClient.initialize("[spm-token]");
 *
 *   SematextMetricsReporter reporter = SematextMetricsReporter.forClient(SematextClient.client())
 *     .withFilter(MetricFilter.ALL)
 *     .withRegistry(metrics)
 *     .withDurationUnit(TimeUnit.MILLISECONDS)
 *     .build();
 * </pre>
 */
public class SematextMetricsReporter extends ScheduledReporter {
  private static final String METRICS_REPORTER_NAME = "SematextMetricsReporter";

  private SematextClient sematextClient;

  private SematextMetricsReporter(SematextClient sematextClient, MetricRegistry registry, String name,
                                  MetricFilter filter, TimeUnit rateUnit, TimeUnit durationUnit) {
    super(registry, name, filter, rateUnit, durationUnit);
    this.sematextClient = sematextClient;
  }

  @Override
  public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
      SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {

    final List<StDatapoint> datapoints = new ArrayList<StDatapoint>();
    for (String timerName : timers.keySet()) {
      datapoints.addAll(createTimerDatapoints(timerName, timers.get(timerName)));
    }
    for (String meterName : meters.keySet()) {
      datapoints.addAll(createMeterDatapoints(meterName, meters.get(meterName)));
    }
    for (String gaugeName : gauges.keySet()) {
      datapoints.addAll(createGaugeDatapoints(gaugeName, gauges.get(gaugeName)));
    }
    for (String histogramName : histograms.keySet()) {
      datapoints.addAll(createCounterDatapoints(histogramName, histograms.get(histogramName)));
    }
    for (String counterName : counters.keySet()) {
      datapoints.addAll(createCountersDatapoints(counterName, counters.get(counterName)));
    }
    sematextClient.send(datapoints);
  }

  private String aggregationTypeFilterName(AggType type) {
    return String.format("agg.type=%s", type.getName());
  }

  private List<StDatapoint> createCountersDatapoints(String counterName, Counter counter) {
    StDatapoint avgDatapoint = StDatapoint.name(counterName)
        .value((double) counter.getCount())
        .aggType(AggType.AVG).build();
    return Arrays.asList(avgDatapoint);
  }

  private List<StDatapoint> createCounterDatapoints(String histogramName, Histogram histogram) {
    Snapshot snapshot = histogram.getSnapshot();
    StDatapoint minDatapoint = StDatapoint.name(histogramName)
        .filter1(aggregationTypeFilterName(AggType.MIN))
        .value((double) snapshot.getMin())
        .aggType(AggType.MIN)
        .build();

    StDatapoint maxDatapoint = StDatapoint.name(histogramName)
        .filter1(aggregationTypeFilterName(AggType.MAX))
        .value((double) snapshot.getMax())
        .aggType(AggType.MAX)
        .build();

    StDatapoint avgDatapoint = StDatapoint.name(histogramName)
        .filter1(aggregationTypeFilterName(AggType.AVG))
        .value(snapshot.getMean())
        .aggType(AggType.AVG)
        .build();
    return Arrays.asList(minDatapoint, maxDatapoint, avgDatapoint);
  }

  private List<StDatapoint> createTimerDatapoints(String timerName, Timer timer) {
    Snapshot snapshot = timer.getSnapshot();
    StDatapoint minDatapoint = StDatapoint.name(timerName)
        .filter1(aggregationTypeFilterName(AggType.MIN))
        .value(convertDuration(snapshot.getMin()))
        .aggType(AggType.MIN)
        .build();

    StDatapoint maxDatapoint = StDatapoint.name(timerName)
        .filter1(aggregationTypeFilterName(AggType.MAX))
        .value(convertDuration(snapshot.getMax()))
        .aggType(AggType.MAX)
        .build();

    StDatapoint avgDatapoint = StDatapoint.name(timerName)
        .filter1(aggregationTypeFilterName(AggType.AVG))
        .value(convertDuration(snapshot.getMean()))
        .aggType(AggType.AVG)
        .build();
    return Arrays.asList(minDatapoint, maxDatapoint, avgDatapoint);
  }

  private List<StDatapoint> createGaugeDatapoints(String gaugeName, Gauge gauge) {
    Object value = gauge.getValue();
    if (value instanceof Number) {
      StDatapoint avgDatapoint = StDatapoint.name(gaugeName)
          .value(((Number) value).doubleValue())
          .aggType(AggType.AVG).build();
      return Arrays.asList(avgDatapoint);
    }
    return Collections.emptyList();
  }

  private List<StDatapoint> createMeterDatapoints(String meterName, Meter meter) {
    StDatapoint sumDatapoint = StDatapoint.name(meterName)
        .value(convertRate(meter.getMeanRate()))
        .aggType(AggType.AVG).build();
    return Arrays.asList(sumDatapoint);
  }

  public static class Builder {
    private MetricRegistry registry;
    private MetricFilter filter = MetricFilter.ALL;
    private TimeUnit rateUnit = TimeUnit.SECONDS;
    private TimeUnit durationUnit = TimeUnit.MILLISECONDS;
    private SematextClient sematextClient;

    private Builder(SematextClient sematextClient) {
      this.sematextClient = sematextClient;
    }

    /**
     * Use metrics filter.
     * @param filter filter
     * @return builder
     */
    public Builder withFilter(MetricFilter filter) {
      this.filter = filter;
      return this;
    }

    /**
     * Set rate unit to be used for sent metrics.
     * @param rateUnit rate unit
     * @return builder
     */
    public Builder withRateUnit(TimeUnit rateUnit) {
      this.rateUnit = rateUnit;
      return this;
    }

    /**
     * Set duration unit to be used for sent metrics.
     * @param durationUnit duration unit
     * @return builder
     */
    public Builder withDurationUnit(TimeUnit durationUnit) {
      this.durationUnit = durationUnit;
      return this;
    }

    /**
     * Set registry.
     * @param registry registry
     * @return registry
     */
    public Builder withRegistry(MetricRegistry registry) {
      this.registry = registry;
      return this;
    }

    /**
     * Build reporter.
     * @return reporter
     * @throws IllegalArgumentException if {@code registry}, {@code filter}, {@code durationUnit} or
     * {@code rateUnit} is {@code null}.
     */
    public SematextMetricsReporter build() {
      if (registry == null) {
        throw new IllegalArgumentException("Registry should be defined.");
      }
      if (filter == null) {
        throw new IllegalArgumentException("Filter should be defined.");
      }
      if (durationUnit == null) {
        throw new IllegalArgumentException("Duration unit should be defined.");
      }
      if (rateUnit == null) {
        throw new IllegalArgumentException("Rate unit should be defined.");
      }
      return new SematextMetricsReporter(sematextClient,
          registry, METRICS_REPORTER_NAME, filter, rateUnit, durationUnit);
    }
  }

  /**
   * Create building for {@link SematextMetricsReporter}.
   * @param client client instance
   * @return builder
   */
  public static Builder forClient(SematextClient client) {
    if (client == null) {
      throw new IllegalArgumentException("Client should be defined");
    }
    return new Builder(client);
  }
}
