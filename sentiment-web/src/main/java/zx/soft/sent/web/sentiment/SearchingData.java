package zx.soft.sent.web.sentiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.solr.err.SpiderSearchException;
import zx.soft.sent.solr.utils.Config;
import zx.soft.sent.solr.utils.JsonUtils;
import zx.soft.sent.solr.utils.TimeUtils;
import zx.soft.sent.web.domain.QueryResult;
import zx.soft.sent.web.domain.SimpleFacetInfo;

/**
 * 搜索舆情数据
 * @author wanggang
 *
 */
public class SearchingData {

	private static Logger logger = LoggerFactory.getLogger(SearchingData.class);

	final CloudSolrServer server;

	public SearchingData() {
		Properties props = Config.getProps("solr_params.properties");
		server = new CloudSolrServer(props.getProperty("zookeeper_cloud"));
		server.setDefaultCollection(props.getProperty("collection"));
	}

	/**
	 * 测试函数
	 */
	public static void main(String[] args) {

		SearchingData search = new SearchingData();
		QueryParams queryParams = new QueryParams();
		// q:关键词
		queryParams.setQ("*:*"); // 美食
		queryParams.setFq("");
		queryParams.setSort(""); // lasttime:desc
		queryParams.setStart(0);
		queryParams.setRows(3);
		queryParams.setWt("json");
		queryParams.setFl(""); // nickname,content
		queryParams.setHlfl(""); // content
		queryParams.setFacetQuery("");
		queryParams.setFacetField(""); // platform
		QueryResult result = search.queryData(queryParams);
		System.out.println(JsonUtils.toJson(result));
	}

	/**
	 * 查询结果数据
	 */
	public QueryResult queryData(QueryParams queryParams) {
		SolrQuery query = getSolrQuery(queryParams);
		QueryResponse queryResponse = null;
		try {
			queryResponse = server.query(query, METHOD.GET);
		} catch (SolrServerException e) {
			throw new RuntimeException(e);
		}
		if (queryResponse == null) {
			throw new SpiderSearchException("no response!");
		}
		QueryResult result = new QueryResult();
		result.setHeader(queryResponse.getHeader());
		result.setSort(queryResponse.getSortValues());
		result.setHighlighting(queryResponse.getHighlighting());
		result.setGroup(queryResponse.getGroupResponse());
		result.setFacetQuery(queryResponse.getFacetQuery());
		result.setFacetFields(transFacetField(queryResponse.getFacetFields()));
		result.setFacetDates(transFacetField(queryResponse.getFacetDates()));
		result.setFacetRanges(queryResponse.getFacetRanges());
		result.setFacetPivot(queryResponse.getFacetPivot());
		result.setNumFound(queryResponse.getResults().getNumFound());
		result.setResults(queryResponse.getResults());
		// 处理时间timestamp、lasttime、first_time、update_time
		tackleTime(result);

		logger.info("numFound=" + queryResponse.getResults().getNumFound());
		logger.info("QTime=" + queryResponse.getQTime());

		return result;
	}

	private void tackleTime(QueryResult result) {
		SolrDocument str = null;
		for (int i = 0; i < result.getResults().size(); i++) {
			str = result.getResults().get(i);
			if (str.getFieldValueMap().get("timestamp") != null) {
				result.getResults()
						.get(i)
						.setField("timestamp",
								TimeUtils.transStrToCommonDateStr(str.getFieldValueMap().get("timestamp").toString()));
			}
			if (str.getFieldValueMap().get("lasttime") != null) {
				result.getResults()
						.get(i)
						.setField("lasttime",
								TimeUtils.transStrToCommonDateStr(str.getFieldValueMap().get("lasttime").toString()));
			}
			if (str.getFieldValueMap().get("first_time") != null) {
				result.getResults()
						.get(i)
						.setField("first_time",
								TimeUtils.transStrToCommonDateStr(str.getFieldValueMap().get("first_time").toString()));
			}
			if (str.getFieldValueMap().get("update_time") != null) {
				result.getResults()
						.get(i)
						.setField("update_time",
								TimeUtils.transStrToCommonDateStr(str.getFieldValueMap().get("update_time").toString()));
			}
		}
	}

	private List<SimpleFacetInfo> transFacetField(List<FacetField> facets) {
		List<SimpleFacetInfo> result = new ArrayList<>();
		if (facets == null) {
			return null;
		}
		for (FacetField facet : facets) {
			SimpleFacetInfo sfi = new SimpleFacetInfo();
			sfi.setName(facet.getName());
			for (Count temp : facet.getValues()) {
				sfi.getValues().put(temp.getName(), temp.getCount());
			}
			result.add(sfi);
		}
		return result;
	}

	/**
	 * 组合查询类
	 * @param q:abc或者[1 TO 100]
	 * @param fq:f1:abc,f2:cde,...
	 * @param fl:username,nickname.content,...
	 * @param hlfl:title,content,...
	 * @param sort:platform:desc,source_id:asc,...
	 * @return
	 */
	private SolrQuery getSolrQuery(QueryParams queryParams) {

		SolrQuery query = new SolrQuery();
		if (queryParams.getQ() != "") {
			query.setQuery(queryParams.getQ());
		}
		// 设置关键词连接逻辑是AND
		query.set("q.op", "AND");
		if (queryParams.getFq() != null) {
			for (String fq : queryParams.getFq().split(",")) {
				query.addFilterQuery(fq);
			}
		}
		if (queryParams.getSort() != "") {
			for (String sort : queryParams.getSort().split(",")) {
				query.addSort(sort.split(":")[0], "desc".equalsIgnoreCase(sort.split(":")[1]) ? ORDER.desc : ORDER.asc);
			}
		}
		if (queryParams.getStart() != 0) {
			query.setStart(queryParams.getStart());
		}
		if (queryParams.getRows() != 10) {
			query.setRows(queryParams.getRows());
		}
		if (queryParams.getFl() != "") {
			query.setFields(queryParams.getFl());
		}
		if (queryParams.getWt() != "") {
			query.set("wt", queryParams.getWt());
		}
		if (queryParams.getHlfl() != "") {
			query.setHighlight(true).setHighlightSnippets(1);
			query.addHighlightField(queryParams.getHlfl());
		}
		if (queryParams.getFacetQuery() != "") {
			query.addFacetQuery(queryParams.getFacetQuery());
		}
		if (queryParams.getFacetField() != "") {
			query.addFacetField(queryParams.getFacetField());
		}

		return query;
	}

	/**
	 * 关闭资源
	 */
	public void close() {
		server.shutdown();
	}

}
