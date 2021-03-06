package zx.soft.sent.dao.sentiment;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.sent.dao.common.MybatisConfig;
import zx.soft.sent.dao.domain.RecordInsert;
import zx.soft.sent.dao.domain.RecordSelect;
import zx.soft.sent.dao.domain.SelectParamsById;
import zx.soft.sent.dao.domain.SelectParamsByTime;
import zx.soft.sent.dao.domain.SentTablename;

public class SentimentRecord {

	private static Logger logger = LoggerFactory.getLogger(SentimentRecord.class);

	private static SqlSessionFactory sqlSessionFactory;

	public SentimentRecord(MybatisConfig.ServerEnum server) {
		try {
			sqlSessionFactory = MybatisConfig.getSqlSessionFactory(server);
		} catch (RuntimeException e) {
			logger.error("SentimentRecord RuntimeException: " + e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取最大Id
	 */
	public int getMaxId(String tablename) {
		try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
			SentimentRecordDao sentimentRecordDao = sqlSession.getMapper(SentimentRecordDao.class);
			return sentimentRecordDao.getMaxId(new SentTablename(tablename, ""));
		}
	}

	/**
	 * 插入数据表名
	 */
	public void insertTablename(String tablename, String name) {
		try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
			SentimentRecordDao sentimentRecordDao = sqlSession.getMapper(SentimentRecordDao.class);
			sentimentRecordDao.insertTablename(new SentTablename(tablename, name));
		}
	}

	/**
	 * 插入Record数据
	 */
	public void insertRecord(RecordInsert recordInsert) {
		try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
			SentimentRecordDao sentimentRecordDao = sqlSession.getMapper(SentimentRecordDao.class);
			sentimentRecordDao.insertRecord(recordInsert);
		}
	}

	/**
	 * 获取Record数据，根据md5的id
	 */
	public RecordSelect selectRecordById(String tablename, String id) {
		try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
			SentimentRecordDao sentimentRecordDao = sqlSession.getMapper(SentimentRecordDao.class);
			return sentimentRecordDao.selectRecordById(new SelectParamsById(tablename, id));
		}
	}

	/**
	 * 获取Records数据，根据lasttime
	 */
	public List<RecordSelect> selectRecordsByLasttime(String tablename, long low, long high) {
		try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
			SentimentRecordDao sentimentRecordDao = sqlSession.getMapper(SentimentRecordDao.class);
			return sentimentRecordDao.selectRecordsByLasttime(new SelectParamsByTime(tablename, low, high));
		}
	}

	/**
	 * 删除Record数据，根据md5的id
	 */
	public void deleteRecordById(String tablename, String id) {
		try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
			SentimentRecordDao sentimentRecordDao = sqlSession.getMapper(SentimentRecordDao.class);
			sentimentRecordDao.deleteRecordById(new SelectParamsById(tablename, id));
		}
	}

}
