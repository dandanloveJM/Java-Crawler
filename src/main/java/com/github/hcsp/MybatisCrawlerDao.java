package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MybatisCrawlerDao implements CrawlerDao {
    SqlSessionFactory sqlSessionFactory;

    public MybatisCrawlerDao() {

        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized String getNextLinkThenDelete() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = session.selectOne("com.github.hcsp.MyMapper.selectNextAvailableLink");
            if (link != null) {
                session.delete("com.github.hcsp.MyMapper.deleteLink", link);
            }
            return link;
        }
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            int count = sqlSession.selectOne("com.github.hcsp.MyMapper.countLink", link);
            return count != 0;
        }
    }

    @Override
    public void storeNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            sqlSession.insert("com.github.hcsp.MyMapper.insertNews", new News(link, content, title));
        }
    }

    @Override
    public void insertProcossedLink(String link) {
        //insert into links_already_processed (link) values (#link)
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "links_already_processed");
        param.put("link", link);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            sqlSession.insert("com.github.hcsp.MyMapper.insertLink", param);
        }


    }

    @Override
    public void insertLinkToBeProcessed(String link) {
        //insert into links_to_Be_processed (link) values (#link)
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "links_to_be_processed");
        param.put("link", link);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            sqlSession.insert("com.github.hcsp.MyMapper.insertLink", param);
        }
    }
}
