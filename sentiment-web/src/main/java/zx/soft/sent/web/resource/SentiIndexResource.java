package zx.soft.sent.web.resource;

import java.util.concurrent.ThreadPoolExecutor;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.utils.threads.ApplyThreadPool;
import zx.soft.sent.web.application.SentiIndexApplication;
import zx.soft.sent.web.common.ErrorResponse;
import zx.soft.sent.web.domain.PostData;

public class SentiIndexResource extends ServerResource {

	private static Logger logger = LoggerFactory.getLogger(SentiIndexResource.class);

	private static SentiIndexApplication application;

	private static ThreadPoolExecutor pool = ApplyThreadPool.getThreadPoolExector();

	@Override
	public void doInit() {
		application = (SentiIndexApplication) getApplication();
	}

	@Post("json")
	public Object acceptData(final PostData data) {

		if (getReference().getRemainingPart().length() != 0) {
			logger.error("query params is illegal, Url=" + getReference());
			return new ErrorResponse.Builder(20003, "your query params is illegal.").build();
		}

		if (data == null) {
			logger.info("Records' size=0");
			return new ErrorResponse.Builder(20003, "no post data.").build();
		}
		logger.info("Request Url=" + getReference() + ", Records' size=" + data.getRecords().size());

		// 另开一个线程索引数据，以免影响客户端响应时间
		pool.execute(new Runnable() {
			@Override
			public void run() {
				application.addDatas(data.getRecords());
			}
		});

		return new ErrorResponse.Builder(0, "ok").build();
	}

	@Get("json")
	public Object getQueryResult() {
		//
		return "no message!";
	}

}
