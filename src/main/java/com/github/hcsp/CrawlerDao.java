package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLink(String sql) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void updateDatabase(String link, String sql) throws SQLException;

    void storeNewsIntoDatabase(String link, String title, String content) throws SQLException;
}
