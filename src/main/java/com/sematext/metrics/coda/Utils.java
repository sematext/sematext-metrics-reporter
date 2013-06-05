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
import com.yammer.metrics.core.MetricName;

public final class Utils {
  private Utils() {
  }

  public static boolean isNotEmpty(String str) {
    return !(str == null || str.isEmpty());
  }

  public static String formatFilterName(AggType type) {
    return String.format("agg.type=%s", type.getName());
  }

  public static String formatName(MetricName metricName) {
    StringBuilder builder = new StringBuilder();
    boolean hasPrev = false;
    if (isNotEmpty(metricName.getGroup())) {
      hasPrev = true;
      builder.append(metricName.getGroup());
    }
    if (isNotEmpty(metricName.getType())) {
      if (hasPrev) {
        builder.append(".");
      }
      builder.append(metricName.getType());
      hasPrev = true;
    }
    if (isNotEmpty(metricName.getName())) {
      if (hasPrev) {
        builder.append(".");
      }
      builder.append(metricName.getName());
      hasPrev = true;
    }
    if (isNotEmpty(metricName.getScope())) {
      if (hasPrev) {
        builder.append(".");
      }
      builder.append(metricName.getScope());
    }
    return builder.toString();
  }
}
