package com.yy.aomi.elastic;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkItemResponse.Failure;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.*;


public class ElasticSearchImpl {

    private static Logger logger = LoggerFactory.getLogger(ElasticSearchImpl.class);

    private String cluster_name;
    private String client_transport_sniff;
    private String client_transport_ignore_cluster_name;
    private String client_transport_ping_timeout;
    private String client_transport_nodes_sampler_interval;
    private String elastic_hosts;
    private String xpack_security_user;
    
    public static final int TIME_OUT = 300000;
    
    //一次插入的最大数据量
    public static final int INSERT_MAX_SIZE=5000;
    //单次查询最大量
    public static final int SEARCH_MAX_SIZE=100000;
    
    //最大查询窗口
    public static final int MAX_RESULT_WINDOW=200000;
    
    /**所有数据库的代表_all
     * 
     */
    public static final String ALL_INDEXS = "_all";
    
    //一次最大返回的数据量
    public static final int RETURN_MAX_SIZE=1000000;


   
    
    private Properties properties;


	public ElasticSearchImpl(){
		logger.info("init ElasticSearchImpl...");
	}







    public String getHost() {
        return elastic_hosts;
    }

    TransportClient client;
    Builder settings;
    

	public void setClient(TransportClient client) {
		this.client = client;
	}



	public void init(String config) {
        try {

            loadConfig(config);
			initEs();
        } catch (Exception e) {
            logger.error("loadConfig ElkClient", e);
        }
    }

    public void initEs(){
		try {
		Builder settings = Settings.builder();

		if (!StringUtils.isBlank(cluster_name)) {
			settings.put("cluster.name", cluster_name.trim());
		}
		if (!StringUtils.isBlank(client_transport_sniff)) {
			settings.put("client.transport.sniff", client_transport_sniff.trim().equals("true"));
		}
		if (!StringUtils.isBlank(client_transport_ignore_cluster_name)) {
			settings.put("client.transport.ignore_cluster_name", client_transport_ignore_cluster_name.trim().equals("true"));
		}
		if (!StringUtils.isBlank(client_transport_ping_timeout)) {
			settings.put("client.transport.ping_timeout", client_transport_ping_timeout.trim());
		}
		if (!StringUtils.isBlank(client_transport_nodes_sampler_interval)) {
			settings.put("client.transport.nodes_sampler_interval", client_transport_nodes_sampler_interval.trim());
		}
		if (!StringUtils.isBlank(xpack_security_user)) {
			settings.put("xpack.security.user", xpack_security_user.trim());
		}

			client = new PreBuiltTransportClient(settings.build());

//		client = new PreBuiltXPackTransportClient(settings.build());

		if (!StringUtils.isBlank(elastic_hosts)) {
			String[] hosts = elastic_hosts.split(",");
			for (String host : hosts) {
				String[] hostip = host.split(":");
				if (hostip.length == 2) {
					client.addTransportAddress(new TransportAddress(InetAddress.getByName(hostip[0].trim()), Integer.parseInt(hostip[1].trim())));
				}
			}
		}
		logger.info("TransportClient init finish!!!");
		} catch (Exception e) {
			logger.error("create ElkClient", e);
		}
	}

    

    public void loadConfig(String config) throws Exception {
        properties = new Properties();
        if (config.toLowerCase().endsWith(".xml")) {
            properties.loadFromXML(new FileInputStream(config));
        } else {
            properties.load(new FileInputStream(config));
        }
        cluster_name = properties.getProperty("cluster_name");
        client_transport_sniff = properties.getProperty("cluster_name");
        client_transport_ignore_cluster_name = properties.getProperty("client_transport_ignore_cluster_name");
        client_transport_ping_timeout = properties.getProperty("client_transport_ping_timeout");
        client_transport_nodes_sampler_interval = properties.getProperty("client_transport_nodes_sampler_interval");
        elastic_hosts = properties.getProperty("elastic_hosts");
        xpack_security_user = properties.getProperty("xpack_security_user");
        
    }


    public TransportClient getClient() {
        return client;
    }


    
    /**插入
     * @param esCondition
     * @return
     * @throws Exception
     */
    public boolean insert(ESCondition esCondition) throws Exception{
    	//统计插入时间
    	long st = System.currentTimeMillis();

 		if(esCondition.getInsertRecords()== null ||esCondition.getInsertRecords().length == 0 ){
 			logger.warn("InsertRecords is null or size=0");
 			return false;
 		}
 		int count = esCondition.getInsertRecords().length;
 		boolean hasIds = false;
 		if(esCondition.getRecordIds() == null){
 			logger.info("RecordIds is null");
 		}else if(esCondition.getRecordIds().length != count){
 			String errorLog = "size not equel,RecordIds size="+esCondition.getRecordIds().length+" InsertRecords size="+count;
 			logger.error(errorLog);
 			throw new Exception(errorLog);
 		}else{
 			hasIds = true;
 		}
 		BulkRequestBuilder bulkRequest = client.prepareBulk();
 		
 		boolean result = true;
 		String[] keyArray = esCondition.getRecordIds();
 		String[] jsonArray = esCondition.getInsertRecords();

 		int j=0;
 		for(int i=0; i < count ; i++){
 			j++;
 			IndexRequestBuilder builder = null;
 			if(hasIds){
 				builder = client.prepareIndex(esCondition.getIndexs()[0], esCondition.getType()[0], keyArray[i]).setSource(jsonArray[i],XContentType.JSON);
 			}else{
 				builder = client.prepareIndex(esCondition.getIndexs()[0], esCondition.getType()[0]).setSource(jsonArray[i],XContentType.JSON);
 			}
 			bulkRequest.add(builder);
 			//分次插入，每5K条记录插入一次
 			if( j%INSERT_MAX_SIZE == 0 || count == j ){
				BulkResponse response = bulkRequest.execute().actionGet();
  				bulkRequest = client.prepareBulk();
  				if(result){
  					result = !response.hasFailures();
					if(!result){
						logger.warn("insert fail,failureMessage={}",response.buildFailureMessage());
					}
  				}
			}
 		}

 		long et = System.currentTimeMillis();
		logger.info("insert result={},size={},time={}",result , esCondition.getInsertRecords().length , et - st);
 		return result;
 	}
    
    
    
    
    /**查询
     * @param esCondition
     * @return
     * @throws Exception
     */
    public ElkSearchResponse search(ESCondition esCondition) throws Exception{
    	SearchRequestBuilder srb = client.prepareSearch(esCondition.getIndexs())
 		        .setTypes(esCondition.getType())
 		        .setSearchType(SearchType.QUERY_THEN_FETCH);
    	String orderName = esCondition.getOrderName();
    	Boolean isAsc = esCondition.getIsAsc();
 		if(orderName != null && isAsc != null){
 			if(isAsc){
 				srb.addSort(orderName, SortOrder.ASC);
 			}else{
 				srb.addSort(orderName, SortOrder.DESC);
 			}
 		}
 		if (esCondition.getQueryBuilder() != null){
 			srb.setQuery(esCondition.getQueryBuilder());
 		}
 		
 		if (esCondition.getOffset() != null){
 			srb.setFrom(esCondition.getOffset());
 		}else{
 			srb.setFrom(0);
 		}
 		if (esCondition.getPageSize() != null){
 			srb.setSize(esCondition.getPageSize());
 		}else{
 			srb.setSize(1000);
 		}
 		if (esCondition.getAggregationConditions()!= null && !esCondition.getAggregationConditions().isEmpty()){
            srb.addAggregation(esCondition.buildAggregation());
        }
 		
 		SearchResponse response = null;
 		try{
 			response = srb.execute().actionGet();
	    } catch (IndexNotFoundException e) {
	    	response = searchExistIndexs(esCondition.getIndexs(),srb);
		}catch (SearchPhaseExecutionException ee){
			response = searchByMaxWindow(srb, ee);
		}
 		ElkSearchResponse elkSearchResponse = new ElkSearchResponse(response);
 		return elkSearchResponse;
    }
    
    /**统计数量
     * @param esCondition
     * @return
     * @throws Exception
     */
    public long count(ESCondition esCondition) throws Exception{
    	try {
	    	SearchRequestBuilder request = client.prepareSearch(esCondition.getIndexs());
	    	request.setTypes(esCondition.getType());
	    	SearchSourceBuilder builder = new SearchSourceBuilder().size(0).query(esCondition.getQueryBuilder());
	    	SearchResponse response = request.setSource(builder).get();
	    	long size = response.getHits().getTotalHits();
	    	return size;
    	} catch (IndexNotFoundException e) {
			logger.warn("indexNotFound="+arrayToString(esCondition.getIndexs()));
		}
    	return 0;
    }
    
    public boolean delete(ESCondition esCondition) throws Exception{
    	boolean hasError = false;
	    try{
	    	 QueryBuilder typeqb = QueryBuilders.typeQuery(esCondition.getType()[0]);    //不接受type当参数，可以在QueryBuilder出指定
			 BoolQueryBuilder allQAndType = QueryBuilders.boolQuery().must(typeqb);
			 if(esCondition.getQueryBuilder()!=null){
				 allQAndType.must(esCondition.getQueryBuilder());
			 }
	    	 BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
	    			  .filter(allQAndType)
	    	   	      .source(esCondition.getIndexs())
	    	 		 .get();
	    	 
	    	 List<Failure> failures = response.getBulkFailures();
	    	 for(Failure failure : failures){
	    		 //该方法判断是否成功有待验证
	    		 if(failure.getStatus().getStatus() != 200){
	    			 hasError = true;
	    		 }
	    	 }
	    } catch (IndexNotFoundException e) {
			logger.warn("indexNotFound="+arrayToString(esCondition.getIndexs()));
	    } 
    	return !hasError;
	}
    
    
    /**删除数据库
     * @param indexs
     * @throws Exception
     */
    public void deleteIndexs(String... indexs) throws Exception{
    	client.admin().indices().delete(new DeleteIndexRequest(indexs)).actionGet();
    }
    
    /**更新
     * @param esCondition
     * @return
     * @throws Exception
     */
    public boolean update(ESCondition esCondition) throws Exception{
		//统计更新时间
		long st = System.currentTimeMillis();
// 		logger.info("elkinsert " + index + " type:" + type);
    	String[] keyArray = esCondition.getRecordIds();
    	String[] jsonArray = esCondition.getInsertRecords();
 		if(keyArray.length != jsonArray.length || jsonArray.length == 0){
 			logger.warn("keyArray.length != jsonArray.length || jsonArray.length == 0 keyArray.size=" 
 							+ keyArray.length +" jsonArray.length="+jsonArray.length);
 			return false;
 		}
 		BulkRequestBuilder bulkRequest = client.prepareBulk();
 		int count = jsonArray.length;
 		int j = 0;
 		boolean result = true;
 		for(int i=0; i < count; i++){
 			j++;
 			UpdateRequestBuilder updateBulder = client.prepareUpdate().setIndex(esCondition.getIndexs()[0])
 					.setType(esCondition.getType()[0])
 					.setDoc(jsonArray[i],XContentType.JSON)
 					.setId(keyArray[i]);
 					bulkRequest.add(updateBulder);
			if( j%INSERT_MAX_SIZE == 0 || count == j ){
				BulkResponse response = bulkRequest.execute().actionGet();
  				bulkRequest = client.prepareBulk();
  				if(result){
  					result = !response.hasFailures();
  				}
			}
 		}
		long et = System.currentTimeMillis();
		logger.info("update size="+esCondition.getInsertRecords().length+" time="+(et-st));
 		return result;
 	}
    
    
    
    /**聚合查询
     * @param esCondition
     * @throws Exception
     */
    public ElkSearchResponse searchByAggregation(ESCondition esCondition) throws Exception{
    	SearchRequestBuilder sbuilder = client.prepareSearch(esCondition.getIndexs()).setTypes(esCondition.getType());
		sbuilder.setSearchType(SearchType.QUERY_THEN_FETCH);
    	sbuilder.addAggregation(esCondition.buildAggregation());
		sbuilder.setQuery(esCondition.getQueryBuilder());
		SearchResponse response = null;
		try {
			response = sbuilder.execute().actionGet();
		} catch (IndexNotFoundException e) {
			response = searchExistIndexs(esCondition.getIndexs(),sbuilder);
		}
    	ElkSearchResponse esResponse = new ElkSearchResponse(response);
    	return esResponse;
    }

	public JSONObject searchByAggration(ESCondition esCondition, String... aggFields) throws Exception {
		JSONObject result = null;
		ElkSearchResponse esResponse = null;
		try {
			esResponse = searchByAggregation(esCondition);
			result = esResponse.getAggregationResult();
			return result;
		} catch (Exception e) {
			Throwable cause = e.getCause();
			while (cause != null) {
				if (cause.getMessage().contains("fielddata=true")) {
					updateAggMapping(esCondition, aggFields);
					esResponse = searchByAggregation(esCondition);
					result = esResponse.getAggregationResult();
					break;
				}
				cause = cause.getCause();
			}
			if(result == null){
				logger.error("",e);
			}
		}
		return result;
	}


	private void updateAggMapping(ESCondition esCondition, String... fields){
		try {
			List<AggregationCondition> aggregationConditions = esCondition.getAggregationConditions();
			if (aggregationConditions!=null && aggregationConditions.size() > 0) {
				return;
			}
			JSONObject properties = new JSONObject();

			for (String filed : fields) {
				JSONObject filedConfig = new JSONObject();
				filedConfig.put("fielddata", true);
				properties.put(filed, filedConfig);
			}

			JSONObject mappingConfig = new JSONObject();
			mappingConfig.put("properties", properties);
			String setting = mappingConfig.toJSONString();
			updateMapping(esCondition.getIndexs(), esCondition.getType(), setting);
		} catch (Exception e) {
			logger.error("", e);
		}

	}

    
    
    /**初始化数据库，设置字段的分词等,只对新库作初始化，忽略已经创建的库
 	 * @param indexs
 	 * @param types
 	 * @param mappingSetting
 	 */
 	public void initIndexByMapping(String[] indexs,String[] types,String mappingSetting) throws Exception{
 		Set<String> justCreateIndexs = new HashSet<>();
 		 for(int i=0;i<indexs.length;i++){
 			 String index = indexs[i];
 			 String type = types[i];
 			if(justCreateIndexs.contains(index) || !isExistsIndex(index)){
 				 if(!justCreateIndexs.contains(index)){
 					logger.info("start create index = "+index);
 					CreateIndexResponse  response = client.admin().indices().create(new CreateIndexRequest(index)).actionGet(); 
 					logger.info("create index = "+index+",response.isAcknowledged="+response.isAcknowledged());
 					justCreateIndexs.add(index);
 				 }
 				logger.info("start init mapping index = "+index+" type="+type);
 	 			updateMapping(new String[]{index}, new String[]{type}, mappingSetting);
			  }
 		 }
 	   }
 	
 	/**使用scroll方式取大量数据,所有数据,数据量不能超过1百万
 	 * @param esCondition
 	 * @return
 	 * @throws Exception
 	 */
 	public ElkSearchResponse searchAllDataByScroll(ESCondition esCondition) throws Exception{
 		ElkSearchResponse response = searchByScroll(esCondition);
 		logger.info("searchAllDataByScroll");
 		if(response.getResponse() == null){
 			return response;
 		}
		String scrollId = response.getResponse().getScrollId();
		List<JSONObject> list = response.getList();
		
		int size = list.size();
		List<JSONObject> allList = new LinkedList<>();
		allList.addAll(list);
		int whileTime = 1;
		while(size == SEARCH_MAX_SIZE){
			response = searchByScrollId(scrollId);
			scrollId = response.getResponse().getScrollId();
			list = response.getList();
			logger.info("searchAllDataByScroll get size="+list.size() + " allSize="+allList.size()+" whileTime="+(whileTime++));
			allList.addAll(list);
			if(allList.size() >= RETURN_MAX_SIZE){
				String errorMsg = "return max data size is "+RETURN_MAX_SIZE+",your data size > "+allList.size()
				+",escondition="+esCondition.toString();
				logger.error(errorMsg);
				new Exception(errorMsg);
			}
			size = list.size();
		}
		logger.info("get data size ="+allList.size());
		response.setList(allList);
		return response;
 	}
 	
 	/**使用scroll方式取大量数据,只取每一页而已
 	 * @param esCondition
 	 * @return
 	 * @throws Exception
 	 */
 	public ElkSearchResponse searchByScroll(ESCondition esCondition) throws Exception{
 		
 		ElkSearchResponse elkSearchResponse = null;
 		SearchResponse response = null;
 		
 		SearchRequestBuilder builder = client.prepareSearch(esCondition.getIndexs())
	 	            .setTypes(esCondition.getType())
	 	            .setScroll(new TimeValue(TIME_OUT))
	 	            .setSize(SEARCH_MAX_SIZE).setTimeout(new TimeValue(TIME_OUT));
			if(esCondition.getQueryBuilder() != null){
				builder.setQuery(esCondition.getQueryBuilder());
			}
		try{
			logger.info("searchByScroll start");
			response = builder.execute().actionGet();
			logger.info("searchByScroll end");
	    } catch (IndexNotFoundException e) {
	    	response = searchExistIndexs(esCondition.getIndexs(), builder);
		}catch (SearchPhaseExecutionException ee){
			response = searchByMaxWindow(builder, ee);
		}
 		elkSearchResponse = new ElkSearchResponse(response);
 		return elkSearchResponse;
 		
 	}
 	
 	/**设置查询窗口
 	 * @throws Exception
 	 */
 	public void setMaxResultWindow() throws Exception{
 		logger.info("setMaxResultWindow");
		Builder builder = Settings.builder().put("index.max_result_window", MAX_RESULT_WINDOW);
		updateSetting(ALL_INDEXS, builder);
 	}
 	
 	/**通过scrollId取数据
 	 * @param scrollId
 	 * @return
 	 * @throws Exception
 	 */
 	public ElkSearchResponse searchByScrollId(String scrollId) throws Exception{
 		
 		SearchResponse response = null;
		logger.info("searchByScrollId start");
 		response = client.prepareSearchScroll(scrollId)
 	            .setScroll(new TimeValue(TIME_OUT))
 	            .execute()
 	            .actionGet();
 		logger.info("searchByScrollId end");

 		ElkSearchResponse elkSearchResponse =new ElkSearchResponse(response);
		return elkSearchResponse;
 		
 	}
 	
 	/**
 	 * @param index _all表示所有index
 	 * 
 	 * Settings.builder().put("index.max_result_window", 200000).put("index.number_of_replicas", 2)             
 	 * @param builder
 	 * @throws Exception
 	 */
 	public void updateSetting(String index,Builder builder) throws Exception{
 		
 		client.admin().indices().prepareUpdateSettings(index).setSettings(builder).execute().actionGet();

 	}
 	
 	/**更新mapping
 	 * @param indexs
 	 * @param types
 	 * @param mappingSetting
 	 * @throws Exception
 	 */
 	public void updateMapping(String[] indexs,String[] types,String mappingSetting) throws Exception{
 		for(int i=0;i<indexs.length;i++){
			String index = indexs[i];
			String type = types[i];
			JSONObject commonSetting = JSONObject.parseObject(mappingSetting);
			JSONObject setting = new JSONObject();
			setting.put(type, commonSetting);
			logger.info("index="+index+" setting="+setting.toJSONString());
			client.admin().indices()
	                .preparePutMapping(index)
	                .setType(type)
	                .setSource(setting.toJSONString(),XContentType.JSON).execute().actionGet();
			logger.info("init index = "+index +" type="+type);
 		}
 	}
 	
 		/**
	     * 判断指定的索引名是否存在
	     * @param indexName 索引名
	     * @return  存在：true; 不存在：false;
	     */
	    public boolean isExistsIndex(String indexName) throws Exception{
	        IndicesExistsResponse  response = 
	                getClient().admin().indices().exists( 
	                        new IndicesExistsRequest().indices(new String[]{indexName})).actionGet();
	        if(!response.isExists()){
	        	logger.info(indexName + " is not exists");
	        }
	        return response.isExists();
	    }
	    
	    /**过滤不存在的数据库
	     * @param indexs
	     * @return
	     * @throws Exception
	     */
	    public String[] getExistIndexs(String[] indexs) throws Exception{
	    	List<String> list = new ArrayList<>(indexs.length);
	    	for(String index:indexs){
	    		if(isExistsIndex(index)){
	    			list.add(index);
	    		}
	    	}
	    	String[] result = new String[list.size()];
	    	list.toArray(result);
	    	return result;
	    }
	    
		/**数组转字符串，方便打日志
		 * @param array
		 * @return
		 */
		private static String arrayToString(String[] array){
			StringBuffer sb = new StringBuffer();
			if(array != null){
				for(String str:array){
					sb.append(str).append(',');
				}
			}
			return sb.toString();
		}
		
		/**重试，只重试已经存在的index
		 * @param indexs
		 * @param srb
		 * @return
		 * @throws Exception
		 */
		private SearchResponse searchExistIndexs(String[] indexs,SearchRequestBuilder srb) throws Exception{
			logger.warn("indexNotFound="+arrayToString(indexs));
			String[] newIndex = getExistIndexs(indexs);
			logger.info("exist index="+arrayToString(newIndex));
			SearchResponse response = null;
			if(newIndex.length > 0){
				srb.setIndices(newIndex);
				response = srb.execute().actionGet();
			}
			return response;
		}
		
		private SearchResponse searchByMaxWindow(SearchRequestBuilder srb,SearchPhaseExecutionException ee) throws Exception{
			logger.info("QueryPhaseExecutionException:"+ee.getCause().toString());
			SearchResponse response = null;
				if(ee.getCause().toString().contains("index.max_result_window")){
					setMaxResultWindow();
					response = srb.execute().actionGet();
				}
			return response;
		}
		
		/**关闭
		 * @throws Exception
		 */
		public void close() throws Exception{
			client.close();
		}

	public String getCluster_name() {
		return cluster_name;
	}

	public void setCluster_name(String cluster_name) {
		this.cluster_name = cluster_name;
	}

	public String getClient_transport_sniff() {
		return client_transport_sniff;
	}

	public void setClient_transport_sniff(String client_transport_sniff) {
		this.client_transport_sniff = client_transport_sniff;
	}

	public String getClient_transport_ignore_cluster_name() {
		return client_transport_ignore_cluster_name;
	}

	public void setClient_transport_ignore_cluster_name(String client_transport_ignore_cluster_name) {
		this.client_transport_ignore_cluster_name = client_transport_ignore_cluster_name;
	}

	public String getClient_transport_ping_timeout() {
		return client_transport_ping_timeout;
	}

	public void setClient_transport_ping_timeout(String client_transport_ping_timeout) {
		this.client_transport_ping_timeout = client_transport_ping_timeout;
	}

	public String getClient_transport_nodes_sampler_interval() {
		return client_transport_nodes_sampler_interval;
	}

	public void setClient_transport_nodes_sampler_interval(String client_transport_nodes_sampler_interval) {
		this.client_transport_nodes_sampler_interval = client_transport_nodes_sampler_interval;
	}

	public String getElastic_hosts() {
		return elastic_hosts;
	}

	public void setElastic_hosts(String elastic_hosts) {
		this.elastic_hosts = elastic_hosts;
	}

	public String getXpack_security_user() {
		return xpack_security_user;
	}

	public void setXpack_security_user(String xpack_security_user) {
		this.xpack_security_user = xpack_security_user;
	}
}
