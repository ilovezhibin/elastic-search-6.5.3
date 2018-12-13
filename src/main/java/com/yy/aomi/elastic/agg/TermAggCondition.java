package com.yy.aomi.elastic.agg;

import com.yy.aomi.elastic.AggregationCondition;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

/**
 * Created by chengaochang on 2017/7/11.
 */
public class TermAggCondition extends AggregationCondition {

    public TermAggCondition(String field, String alias) {
        super(field, alias);
    }

    public TermAggCondition(String field, String alias,int size) {
        super(field, alias, size);
    }

    public AggregationBuilder builderAgg(){
        TermsAggregationBuilder tb = AggregationBuilders.terms(alias).field(field);
        tb.size(this.size);
        if (this.orderName != null) {
            tb.order(BucketOrder.aggregation(this.orderName, this.isAsc));
        }
        return tb;
    }
}
