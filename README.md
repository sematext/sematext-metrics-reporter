sematext-metrics-reporter[![Build Status](https://travis-ci.org/sematext/sematext-metrics-reporter.png?branch=metrics-2.2.0)](https://travis-ci.org/sematext/sematext-metrics-reporter)
====

[Coda Hale Metrics](http://metrics.codahale.com/) Reporter that uses [Sematext Metrics](http://github.com/sematext/sematext-metrics) library to send metrics to [SPM](http://sematext.com/spm/index.html).

## Usage
Add maven dependency:

    <dependency>
      <groupId>com.sematext</groupId>
      <artifactId>sematext-metrics-reporter</artifactId>
      <version>0.1.2.2.0</version>
    </dependency>


And configure reporter:

    MetricRegistry metrics = new MetricRegistry();

    SematextClient.initialize("[spm-token]");

    SematextMetricsReporter reporter = SematextMetricsReporter.forClient(SematextClient.client())
        .withPredicate(MetricPredicate.ALL)
        .withRegistry(metrics)
        .build();

    reporter.start(5, TimeUnit.MINUTES);

## License

Copyright 2013 Sematext Group, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
