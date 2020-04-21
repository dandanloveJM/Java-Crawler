package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.sql.*;

public class JdbcCrawlerDao implements CrawlerDao {
    private static final String USER_NAME = "root";
    private static final String USER_PASSWORD = "dandan";
    Connection connection;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        File projectDir = new File(System.getProperty("basedir", System.getProperty("user.dir")));
        String jdbcUrl = "jdbc:h2:file:" + new File(projectDir, "news").getAbsolutePath();
        try {
            this.connection = DriverManager.getConnection(jdbcUrl, USER_NAME, USER_PASSWORD);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }

    }
    public String getNextLinkThenDelete() throws SQLException {
        //先从数据库里拿出来一个链接，(拿出来并从数据库中删除掉)，准备处理之
        String link = getNextLink("select link from LINKS_TO_BE_PROCESSED limit 1");
        if (link != null) {
            updateDatabase(link, "delete from LINKS_TO_BE_PROCESSED where link = ?");
        }
        return link;
    }

    String getNextLink(String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }


    public boolean isLinkProcessed(String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("select link from LINKS_ALREADY_PROCESSED where link = ?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    public void updateDatabase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    public void storeNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into NEWS (TITLE, CONTENT, URL, CREATED_AT, MODIFIED_AT) VALUES ( ?,?,?,NOW(), NOW())")) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, link);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertProcossedLink(String link) {

    }

    @Override
    public void insertLinkToBeProcessed(String href) {

    }
}
