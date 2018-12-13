package com.yy.aomi.elastic;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href= "mailto:909074682@yy.com" style="color:##E0E;">zhangzhibin</a>
 * @version V1.0
 * @date 2017年6月21日下午6:30:18
 */
public abstract class AggregationCondition {
    /**
     * 要group by的字段，是完整的字段，如uriStat.key，而不是key
     */
    protected String field;
    /**
     * group by字段后的别名
     */
    protected String alias;
    protected String orderName;
    protected QueryBuilder queryBuilder;
    protected boolean isAsc;
    //这个很重要，解定了分组后的个数，只能往大里设置
    protected int size = 100000;

    public static enum operate {
        sum,
        max,
        min,
        avg
    }


    private List<AggregationBuilder> subAggList = new ArrayList<>();

    public AggregationCondition(String field, String alias) {
        this.field = field;
        this.alias = alias;
    }

    public AggregationCondition(String field, String alias, int size) {
        this.field = field;
        this.alias = alias;
        this.size = size;
    }

    public AggregationCondition(String field, QueryBuilder queryBuilder) {
        this.field = field;
        this.queryBuilder = queryBuilder;
    }


    public AggregationBuilder builder() {
        AggregationBuilder aggBuilder = builderAgg();
        builderSubAgg(aggBuilder);
        return aggBuilder;
    }

    protected abstract AggregationBuilder builderAgg();

    private void builderSubAgg(AggregationBuilder aggBuilder) {
        for (AggregationBuilder sub : subAggList) {
            aggBuilder.subAggregation(sub);
        }
    }

    public AggregationCondition sum(String field, String alias) {
        subAggList.add(AggregationBuilders.sum(alias).field(field));
        return this;
    }

    public AggregationCondition avg(String field, String alias) {
        subAggList.add(AggregationBuilders.avg(alias).field(field));
        return this;
    }

    public AggregationCondition min(String field, String alias) {
        subAggList.add(AggregationBuilders.min(alias).field(field));
        return this;
    }

    public AggregationCondition max(String field, String alias) {
        subAggList.add(AggregationBuilders.max(alias).field(field));
        return this;
    }

    public AggregationCondition terms(AggregationCondition aggCondition) {
        subAggList.addAll(aggCondition.getSubAggList());
        return this;
    }

    public AggregationCondition terms(String field, String alias){
         subAggList.add(AggregationBuilders.terms(alias).field(field));
         return this;
    }
    public AggregationCondition terms(String field, String alias, int size){
        subAggList.add(AggregationBuilders.terms(alias).field(field).size(size));
        return this;
    }

    public List<AggregationBuilder> getSubAggList() {
        return subAggList;
    }

    public void setSubAggList(List<AggregationBuilder> subAggList) {
        this.subAggList = subAggList;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public boolean isAsc() {
        return isAsc;
    }

    public void setAsc(boolean isAsc) {
        this.isAsc = isAsc;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public QueryBuilder getQueryBuilder() {
        return queryBuilder;
    }

    public void setQueryBuilder(QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }


}
