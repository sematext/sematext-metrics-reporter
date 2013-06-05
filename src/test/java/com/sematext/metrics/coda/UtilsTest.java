/*
 * Copyright (c) Sematext International
 * All Rights Reserved
 * <p/>
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */
package com.sematext.metrics.coda;

import com.yammer.metrics.core.MetricName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {
  @Test
  public void testFormatName() {
    assertEquals("com.sematext.metrics.coda.UtilsTest.request-rate", Utils.formatName(new MetricName(UtilsTest.class, "request-rate", null)));
    assertEquals("UtilsTest.request-rate", Utils.formatName(new MetricName("", "UtilsTest", "request-rate")));
    assertEquals("request-rate.scope", Utils.formatName(new MetricName("", "", "request-rate", "scope")));
  }
}
