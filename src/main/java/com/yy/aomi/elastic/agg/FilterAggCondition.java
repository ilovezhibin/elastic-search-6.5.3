package com.yy.aomi.elastic.agg;

import com.yy.aomi.elastic.AggregationCondition;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

/**
 * Created by chengaochang on 2017/7/11.
 */
public class FilterAggCondition extends AggregationCondition {

    public FilterAggCondition(String field, QueryBuilder queryBuilder) {
        super(field, queryBuilder);
    }

    @Override
    protected AggregationBuilder builderAgg() {
        return AggregationBuilders.filter(field, this.queryBuilder);
    }
}
