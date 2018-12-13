package com.yy.aomi.elastic;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation.SingleValue;

import java.util.*;

/**
 * @author <a href= "mailto:909074682@yy.com" style="color:##E0E;">zhangzhibin</a>
 * @version V1.0
 * @date 2017年5月15日下午4:58:12
 */
public class ElkSearchResponse {

    private SearchResponse response;

    private List<JSONObject> list;


    public static final String AGG_COUNT_KEY = "docCount";

    private List beanList;


    public ElkSearchResponse(SearchResponse response) throws Exception {
        this.response = response;
    }



    public JSONObject getAggregationResult() {
        JSONObject result = new JSONObject();
        if (response != null && response.getAggregations() != null) {
            Aggregations aggregations = response.getAggregations();
            Map<String, Aggregation> subAggMap = aggregations.asMap();
            Set<String> keys = subAggMap.keySet();
            for (String key : keys) {
                Aggregation item = subAggMap.get(key);
                if (item instanceof MultiBucketsAggregation) {
                    result.put(key, getBucketResult(item));
                } else if (item instanceof SingleValue) {
                    double value = ((SingleValue) subAggMap.get(key)).value();
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    public List<JSONObject> getBucketResult(Aggregation aggregation) {
        List<JSONObject> mapList = new ArrayList<>();
        MultiBucketsAggregation bucketsAgg = (MultiBucketsAggregation) aggregation;
        if (bucketsAgg == null) return mapList;
        Iterator<? extends MultiBucketsAggregation.Bucket> termsBucketIt = bucketsAgg.getBuckets().iterator();

        while (termsBucketIt.hasNext()) {
            JSONObject itemMap = new JSONObject();
            MultiBucketsAggregation.Bucket buck = termsBucketIt.next();
            //获取group by字段名
            String team = buck.getKey().toString();
            //记录数
            long count = buck.getDocCount();
            JSONObject content = new JSONObject();
            content.put(AGG_COUNT_KEY,count);
            Map<String, Aggregation> subaggmap = buck.getAggregations().asMap();
            Set<String> keys = subaggmap.keySet();
            for (String key : keys) {
                Aggregation item = subaggmap.get(key);
                if (item instanceof MultiBucketsAggregation) {
                    content.put(key, getBucketResult(item));
                } else if (item instanceof SingleValue) {
                    double value = ((SingleValue) subaggmap.get(key)).value();
                    content.put(key, value);
                }
            }
            itemMap.put(team,content);
            mapList.add(itemMap);

        }
        return mapList;
    }


    /**结果去除不必要的数组
     * @return
     */
    public JSONObject getAggregationResultNoArray() {
        JSONObject result = new JSONObject();
        if (response != null && response.getAggregations() != null) {
            Aggregations aggregations = response.getAggregations();
            Map<String, Aggregation> subAggMap = aggregations.asMap();
            Set<String> keys = subAggMap.keySet();
            for (String key : keys) {
                Aggregation item = subAggMap.get(key);
                if (item instanceof MultiBucketsAggregation) {
                    result.put(key, getBucketResultNoArray(item));
                } else if (item instanceof SingleValue) {
                    double value = ((SingleValue) subAggMap.get(key)).value();
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**结果去除不必要的数组
     * @param aggregation
     * @return
     */
    public JSONObject getBucketResultNoArray(Aggregation aggregation) {
        JSONObject result = new JSONObject();
        MultiBucketsAggregation bucketsAgg = (MultiBucketsAggregation) aggregation;
        if (bucketsAgg == null) return result;
        Iterator<? extends MultiBucketsAggregation.Bucket> termsBucketIt = bucketsAgg.getBuckets().iterator();

        while (termsBucketIt.hasNext()) {
            MultiBucketsAggregation.Bucket buck = termsBucketIt.next();
            //获取group by字段名
            String team = buck.getKey().toString();
            //记录数
            long count = buck.getDocCount();
            JSONObject content = new JSONObject();
            content.put(AGG_COUNT_KEY,count);
            Map<String, Aggregation> subaggmap = buck.getAggregations().asMap();
            Set<String> keys = subaggmap.keySet();
            for (String key : keys) {
                Aggregation item = subaggmap.get(key);
                if (item instanceof MultiBucketsAggregation) {
                    content.put(key, getBucketResultNoArray(item));
                } else if (item instanceof SingleValue) {
                    double value = ((SingleValue) subaggmap.get(key)).value();
                    content.put(key, value);
                }
            }
            result.put(team,content);

        }
        return result;
    }


    /**
     * @param actionGet
     * @return
     */
    public static List<JSONObject> getResultAsJsonObject(SearchResponse actionGet){
        return getResultAsBeanList(actionGet,JSONObject.class);
    }

    /**
     * @param actionGet
     * @return
     */
    public static <T> List<T> getResultAsBeanList(SearchResponse actionGet,Class<T> clazz){
        List<T> result = new ArrayList<>(1);
        if (actionGet != null) {
            SearchHits hits = actionGet.getHits();
            if (hits != null && hits.getHits() != null) {
                SearchHit[] hitArray = hits.getHits();
                result = new ArrayList<>(hitArray.length);
                for (SearchHit hit : hitArray) {
                    String hitMap = hit.getSourceAsString();
                    if (hitMap == null) {
                        continue;
                    }
                    T source = JSONObject.parseObject(hitMap, clazz);
                    result.add(source);

                }
            }
        }
        return result;
    }

    private static List<JSONObject> getResultAsJSONListWithId(SearchResponse actionGet){
        List<JSONObject> result = new ArrayList<>(1);
        if (actionGet != null) {
            SearchHits hits = actionGet.getHits();
            if (hits != null && hits.getHits() != null) {
                SearchHit[] hitArray = hits.getHits();
                result = new ArrayList<>(hitArray.length);
                for (SearchHit hit : hitArray) {
                    String hitMap = hit.getSourceAsString();
                    if (hitMap == null) {
                        continue;
                    }
                    JSONObject source = JSONObject.parseObject(hitMap);
                    source.put("_id",hit.getId());
                    result.add(source);

                }
            }
        }
        return result;
    }




    public SearchResponse getResponse() {
        return response;
    }


    public void setResponse(SearchResponse response) {
        this.response = response;
    }


    public List<JSONObject> getList() {
        if(this.list == null){
            this.list = getResultAsJsonObject(response);
        }
        return list;
    }

    public List<JSONObject> getListWithId() {
        if(this.list == null){
            this.list = getResultAsJSONListWithId(response);
        }
        return list;
    }

    public <T> List<T> getBeanList(Class<T> tClass) {
        if(beanList == null){
            beanList = getResultAsBeanList(response, tClass);
        }
        return beanList;
    }


    public List<JSONObject> getReverseList() {
        Collections.reverse(list);
        return list;
    }


    public void setList(List<JSONObject> list) {
        this.list = list;
    }


}
