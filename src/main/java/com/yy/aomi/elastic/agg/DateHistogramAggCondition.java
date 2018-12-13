package com.yy.aomi.elastic.agg;

import com.yy.aomi.elastic.AggregationCondition;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.joda.time.DateTimeZone;

/**
 * Created by chengaochang on 2017/7/11.
 */
public class DateHistogramAggCondition extends AggregationCondition {

    private long interval;

    public DateHistogramAggCondition(String field, String alias, long interval) {
        super(field, alias);
        this.interval = interval;
    }

    @Override
    protected AggregationBuilder builderAgg() {
        DateHistogramAggregationBuilder dateAgg = AggregationBuilders.dateHistogram(alias).field(field);
//        dateAgg.dateHistogramInterval(DateHistogramInterval.DAY);
        dateAgg.interval(interval);
        dateAgg.timeZone(DateTimeZone.getDefault());
        return dateAgg;
    }
}
