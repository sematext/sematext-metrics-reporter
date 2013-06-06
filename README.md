sematext-metrics-reporter
====

Coda Hale Metrics(http://metrics.codahale.com/) Reporter that uses [Sematext Metrics](http://github.com/sematext/sematext-metrics) to send metrics to [SPM](http://sematext.com/spm/index.html)

## Usage

    MetricRegistry metrics = new MetricRegistry();

    SematextClient.initialize("[spm-token]");
    SematextMetricsReporter reporter = SematextMetricsReporter.forClient(SematextClient.client())
      .withFilter(MetricFilter.ALL)
      .withRegistry(metrics)
      .withDurationUnit(TimeUnit.MILLISECONDS)
      .build();

    reporter.start(1, TimeUnit.MINUTES);

## License

Copyright 2013 Sematext Group, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
