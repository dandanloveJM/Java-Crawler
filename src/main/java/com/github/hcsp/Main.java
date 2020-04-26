package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.sql.SQLException;

public class Main {
    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) {
        CrawlerDao dao = new MybatisCrawlerDao();
        for (int i = 0; i < 8; i++) {
            new Crawler(dao).start();
        }
    }
}
