package zx.soft.sent.dao.demo;

import java.sql.SQLException;

import org.apache.ibatis.session.SqlSessionFactory;

import zx.soft.sent.dao.common.MybatisConfig;

public class MybatisConfigDemo {

	public static void main(String[] args) throws SQLException {

		SqlSessionFactory sqlSessionFactory = MybatisConfig.getSqlSessionFactory(MybatisConfig.ServerEnum.local);
		System.out.println(sqlSessionFactory.getConfiguration().getEnvironment().getId());

	}

}
