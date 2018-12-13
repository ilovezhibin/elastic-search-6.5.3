package com.yy.aomi.elastic;
import com.yy.aomi.elastic.agg.DateHistogramAggCondition;
import com.yy.aomi.elastic.agg.FilterAggCondition;
import com.yy.aomi.elastic.agg.TermAggCondition;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author <a href= "mailto:909074682@yy.com" style="color:##E0E;">zhangzhibin</a>
 * @version V1.0
 * @date 2017年5月24日下午5:45:39
 */
public class ESCondition {

    private String[] indexs;
    private String[] type;
    private Integer page;
    private Integer offset;
    private Integer pageSize = 1000;
    private String orderName;
    private Boolean isAsc;
    private String[] insertRecords;
    private String[] recordIds;

    private List<AggregationCondition> aggregationConditions = new ArrayList<>();

    private QueryBuilder queryBuilder;
    private AggregationBuilder aggregation;

    public static enum symbol {
        /**
         * 等于，相当于sql key = value
         */
        EQ,
        /**
         * 不等于，相当于sql key != value
         */
        NE,
        /**
         * 小于，相当于sql key < value
         */
        LT,
        /**
         * 大于，相当于sql key > value
         */
        GT,
        /**
         * 小于等于，相当于sql key <= value
         */
        LE,
        /**
         * 大于等于，相当于sql key >= value
         */
        GE,
        /**
         * 指定范围之内（包括边界），相当于sql between value1 and value2，至少两个value
         */
        RAN,
        /**
         * 不在指定范围之内（包括边界），相当于sql not between value1 and value2，至少两个value
         */
        NRAN,
        /**
         * 指定值之中，相当于sql  in(value1,value2,value3...)至少一个value
         */
        IN,
        /**
         * 不在指定值之中，相当于sql  not in(value1,value2,value3...)至少一个value
         */
        NIN,
        /**
         * 判断某个key（属性）存在
         */
        EXIST,
        /**
         * 判断某个key（属性）不存在
         */
        NEXIST,
        
        /**
         * 判断前缀相同
         */
        PREFIX,
        /**
         * 判断前缀不相同
         */
        NE_PREFIX,
        /**
         * 部分匹配，类似sql的 "%关键字%"，如果关键字里有*和?，会被自动转换，慎用，因为官方强烈不建议以通配符开头的查询
         */
        LIKE,
        /**
         * 模糊查找，只支持*和?，*为多字符匹配，?为单字符匹配，慎用，因为官方强烈不建议以通配符开头的查询
         */
        WILD_CARD
    }

    public ESCondition() {

    }

    public ESCondition(String type, String... indexs) {
        this.indexs = indexs;
        this.type = new String[]{type};
    }

    /**自动计算数据库范围和加上时间条件
     * @param elkDBVO
     * @param from
     * @param to
     */
    public ESCondition(ElkDBVO elkDBVO, Date from, Date to){
        String[] indexs = elkDBVO.getIndexRand(from,to);
        this.type = new String[]{elkDBVO.getType()};
        this.indexs = indexs;
        this.and(ESCondition.symbol.RAN, ELKConstant.timestamp, from.getTime(), to.getTime());
    }


    private ESCondition(String[] indexs, String[] type, String[] insertRecords) {
        super();
        this.indexs = indexs;
        this.type = type;
        this.insertRecords = insertRecords;
    }

    private ESCondition(String[] indexs, String[] type, String[] recordIds, String[] insertRecords) {
        super();
        this.indexs = indexs;
        this.type = type;
        this.insertRecords = insertRecords;
        this.recordIds = recordIds;
    }


    public AggregationCondition addTermsAggCondition(String field, String alias) {
        AggregationCondition agCondition = new TermAggCondition(field, alias);
        aggregationConditions.add(agCondition);
        return agCondition;
    }

    public AggregationCondition addTermsAggCondition(String field, String alias,int size) {
        AggregationCondition agCondition = new TermAggCondition(field, alias, size);
        aggregationConditions.add(agCondition);
        return agCondition;
    }
    
    public AggregationCondition addFilterAggCondition(String field, QueryBuilder query) {
        AggregationCondition agCondition = new FilterAggCondition(field, query);
        aggregationConditions.add(agCondition);
        return agCondition;
    }


    public AggregationCondition addDateHistogramAggCondition(String field, String alias, long interval) {
        AggregationCondition agCondition = new DateHistogramAggCondition(field, alias, interval);
        aggregationConditions.add(agCondition);
        return agCondition;
    }


    public static ESCondition getInsertEntity(String index, String type, String... insertRecords) {
        return new ESCondition(new String[]{index}, new String[]{type}, insertRecords);
    }

    public static ESCondition getInsertEntity(String index, String type, String[] recordIds, String[] insertRecords) {
        return new ESCondition(new String[]{index}, new String[]{type}, recordIds, insertRecords);
    }

    public static ESCondition getUpdateEntity(String[] indexs, String type, String[] recordIds, String[] insertRecords) {
        return new ESCondition(indexs, new String[]{type}, recordIds, insertRecords);
    }


    public ESCondition or(ESCondition esCondition) {
        if(esCondition == null) return this;
        QueryBuilder qb = esCondition.getQueryBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (queryBuilder == null) {
            queryBuilder = qb;
        } else {
            queryBuilder = boolQuery.should(queryBuilder).should(qb);
        }

        return this;
    }

    public ESCondition or(String key, Object... values) throws Exception {
        return or(symbol.EQ, key, values);
    }
    
    public ESCondition orRand(String key,Object left,Object right,boolean leftEq,boolean rightEq) throws Exception{
    	return or(symbol.RAN, key, left,right,leftEq,rightEq);
    }

    public ESCondition or(symbol type, String key, Object... values) throws Exception {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        QueryBuilder qb = builderOneConditionQuery(type, key, values);
        if (queryBuilder == null) {
            queryBuilder = qb;
        } else {
            queryBuilder = boolQuery.should(queryBuilder).should(qb);
        }
        return this;
    }

    public ESCondition and(ESCondition esCondition) {
        if(esCondition == null) return this;
        QueryBuilder qb = esCondition.getQueryBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (queryBuilder == null) {
            queryBuilder = qb;
        } else {
            queryBuilder = boolQuery.must(queryBuilder).must(qb);
        }
        return this;
    }

    public ESCondition and(String key, Object... values) {
        return and(symbol.EQ, key, values);
    }
    
    public ESCondition andRand(String key,Object left,Object right,boolean leftEq,boolean rightEq){
    	return and(symbol.RAN, key, left,right,leftEq,rightEq);
    }


    public ESCondition and(symbol type, String key, Object... values){
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        QueryBuilder qb = builderOneConditionQuery(type, key, values);
        if (queryBuilder == null) {
            queryBuilder = qb;
        } else {
            queryBuilder = boolQuery.must(queryBuilder).must(qb);
        }

        return this;
    }

    public ESCondition not() {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        queryBuilder = boolQuery.mustNot(queryBuilder);
        return this;
    }
    


    public static QueryBuilder builderOneConditionQuery(symbol type, String key, Object... values){
        Object value = null;
        boolean isString = false;
        if (values.length > 0) {
            value = values[0];
            isString = value instanceof String;
        }
        QueryBuilder result = null;
        switch (type) {
            case EQ:
                if (isString) {
                    result = QueryBuilders.queryStringQuery("\"" + value.toString() + "\"").field(key);
                    break;
                } else {
                    result = QueryBuilders.matchQuery(key, value);
                    break;
                }

            case NE:
                result = QueryBuilders.boolQuery().mustNot(builderOneConditionQuery(symbol.EQ, key, values));
                break;
            case LT:
                result = QueryBuilders.rangeQuery(key).to(value).includeUpper(false);
                break;
            case GT:
                result = QueryBuilders.rangeQuery(key).from(value).includeLower(false);
                break;
            case LE:
                result = QueryBuilders.rangeQuery(key).to(value).includeUpper(true);
                break;
            case GE:
                result = QueryBuilders.rangeQuery(key).from(value).includeLower(true);
                break;
            case RAN:
            	boolean lt = true;
            	boolean rt = true;
            	if(values.length >=4){
            		if(values[2] instanceof Boolean){
            			lt = (boolean) values[2];
            		}
            		if(values[3] instanceof Boolean){
            			rt = (boolean) values[3];
            		}
            	}
                result = QueryBuilders.rangeQuery(key).from(value).to(values[1]).includeLower(lt).includeUpper(rt);
                break;
            case NRAN:
                result = QueryBuilders.boolQuery().mustNot(builderOneConditionQuery(symbol.RAN, key, values));
                break;
            case IN:
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                for (Object oneValue : values) {
                    boolQuery.should(builderOneConditionQuery(symbol.EQ, key, oneValue));
                }
                result = boolQuery;
                break;
            case NIN:
                result = QueryBuilders.boolQuery().mustNot(builderOneConditionQuery(symbol.IN, key, values));
                break;
            case EXIST:
                result = QueryBuilders.existsQuery(key);
                break;
            case NEXIST:
                result = QueryBuilders.boolQuery().mustNot(builderOneConditionQuery(symbol.EXIST, key, values));
                break;
            case PREFIX:
            	result = QueryBuilders.prefixQuery(key, value.toString());
            	break;
            case NE_PREFIX:
            	result = QueryBuilders.boolQuery().mustNot(QueryBuilders.prefixQuery(key, value.toString()));
            	break;
            case LIKE:
                result = QueryBuilders.wildcardQuery(key,"*"+changeSpecialChar(value.toString())+"*");
                break;
            case WILD_CARD:
                result = QueryBuilders.wildcardQuery(key,value.toString());
                break;
        }

        return result;
    }

    /**正则查找时，特殊字符转换
     * @param text
     * @return
     */
    public static String changeSpecialChar(String text){
        return  QueryParserBase.escape(text);
    }


    public String[] getIndexs() {
        return indexs;
    }

    public void setIndexs(String... indexs) {
        this.indexs = indexs;
    }

    public String[] getType() {
        return type;
    }

    public void setType(String... type) {
        this.type = type;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getOffset() {
        if (offset == null && pageSize != null) {
            offset = getPageFrom(page, pageSize);
        } else if (offset == null && pageSize == null) {
            offset = 0;
        }
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<AggregationCondition> getAggregationConditions() {
        return aggregationConditions;
    }

    public void setAggregationConditions(List<AggregationCondition> aggregationConditions) {
        this.aggregationConditions = aggregationConditions;
    }

    public AggregationBuilder buildAggregation() {
        AggregationBuilder first = null;
        AggregationBuilder parentAgg = null;
        for (AggregationCondition condition : aggregationConditions) {
            if (first == null) {
                first = condition.builder();
                parentAgg = first;
            } else {
                AggregationBuilder subAgg = condition.builder();
                parentAgg.subAggregation(subAgg);
                parentAgg = subAgg;
            }
        }
        return first;
    }

    public void setAggregation(AggregationBuilder aggregation) {
        this.aggregation = aggregation;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public Boolean getIsAsc() {
        return isAsc;
    }

    public void setIsAsc(Boolean isAsc) {
        this.isAsc = isAsc;
    }

    public QueryBuilder getQueryBuilder() {
        return queryBuilder;
    }

    public void setQueryBuilder(QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    public String[] getInsertRecords() {
        return insertRecords;
    }

    public void setInsertRecords(String[] insertRecords) {
        this.insertRecords = insertRecords;
    }

    public void setInsertRecords(List<String> insertRecords) {
        this.insertRecords = listToArray(insertRecords);
    }

    public String[] getRecordIds() {
        return recordIds;
    }

    public void setRecordIds(String[] recordIds) {
        this.recordIds = recordIds;
    }

    public void setRecordIds(List<String> recordIds) {
        this.recordIds = listToArray(recordIds);
    }


    public String[] listToArray(List<String> list) {
        if (list != null) {
            String[] array = new String[list.size()];
            list.toArray(array);
            return array;
        }
        return new String[]{};
    }

    public int getPageFrom(Integer page, Integer pageSize) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10000;
        }
        return (page - 1) * pageSize;
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}

