package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {
    private static final int TARGET_ROW_COUNT = 100_000;

    private static void mockData(SqlSessionFactory sqlSessionFactory, int howMany) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            List<News> currentNews = sqlSession.selectList("com.github.hcsp.MockMapper.selectNews");
            int count = howMany - currentNews.size();
            Random random = new Random();
            try {
                while (count-- > 0) {
                    //随便从现有的新闻List里拿一个新闻出来
                    int index = random.nextInt(currentNews.size());
                    News newsToBeInserted = new News(currentNews.get(index));

                    //修改时间戳
                    Instant currentTime = newsToBeInserted.getCreatedAt();
                    currentTime = currentTime.minusMillis(random.nextInt(3600 * 24 * 365));
                    newsToBeInserted.setModifiedAt(currentTime);
                    newsToBeInserted.setCreatedAt(currentTime);

                    sqlSession.insert("com.github.hcsp.MockMapper.insertNews", newsToBeInserted);
                    System.out.println("Left: " + count);
                    if (count %2000 == 0) {
                        sqlSession.flushStatements();
                        sqlSession.commit();
                    }
                }


            } catch (Exception e) {
                sqlSession.rollback();
                throw new RuntimeException(e);

            }
        }
    }

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mockData(sqlSessionFactory, 100_0000);

    }


}
