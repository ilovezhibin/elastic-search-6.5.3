package com.yy.aomi.elastic;

import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

/**
 * create by zhangzhibin(909074682)
 * 2018/12/12
 */
public class test {

    public static final ElkDBVO  MIN5 = new ElkDBVO("aomi-min-5-", "logs", ElkDBVO.YYYYMMDD);


    ElasticSearchImpl elasticSearch;

    @Before
    public void init(){

        elasticSearch = new ElasticSearchImpl();
        elasticSearch.setElastic_hosts("localhost:9300");
        elasticSearch.setCluster_name("es_test_6.5.3");
        elasticSearch.initEs();
    }

    @Test
    public void testCount() throws Exception{
        Date now = new Date();
        Date from = new Date(now.getTime()-DateUtils.HOUR);
        ESCondition condition = new ESCondition(MIN5,from,now);
        long size = elasticSearch.count(condition);
        System.out.println("size="+size);
    }

    @Test
    public void testAgg() throws Exception{
        Date now = new Date();
        Date from = new Date(now.getTime()-DateUtils.HOUR);
        ESCondition condition = new ESCondition(MIN5,from,now);
        condition.addTermsAggCondition("businessType","businessType");
        JSONObject object = elasticSearch.searchByAggregation(condition).getAggregationResultNoArray();
        System.out.println(object);
    }

    @Test
    public void testSearch() throws Exception{
        Date now = new Date();
        Date from = new Date(now.getTime()-DateUtils.HOUR);
        ESCondition condition = new ESCondition(MIN5,from,now);
        condition.and("businessType","uri");
        List<JSONObject> result = elasticSearch.search(condition).getList();
        System.out.println("result size="+result.size());
    }

}
