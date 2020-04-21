package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkThenDelete() throws SQLException;
    boolean isLinkProcessed(String link) throws SQLException;

    void storeNewsIntoDatabase(String link, String title, String content) throws SQLException;

    void insertProcossedLink(String link);

    void insertLinkToBeProcessed(String href);
}
