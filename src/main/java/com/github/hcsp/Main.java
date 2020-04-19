package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Main {
    private static final String USER_NAME = "root";
    private static final String USER_PASSWORD = "dandan";

    private static List<String> loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        }
        return results;
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        while (true) {
            //从数据库加载即将处理的链接
            Connection connection = getConnection();
            List<String> linkpool = loadUrlsFromDatabase(connection, "select link from LINKS_TO_BE_PROCESSED");
            if (linkpool.isEmpty()) {
                break;
            }
            //arraylist从尾部删除更有效率, remove会返回删掉的元素
            //先在数据库中拿出来一个Link，再在数据库里删掉这个Link
            String link = linkpool.remove(linkpool.size() - 1);
            try (PreparedStatement statement = connection.prepareStatement("delete from LINKS_TO_BE_PROCESSED where link = ?")) {
                statement.setString(1, link);
                statement.executeUpdate();
            }

            //询问数据库当前链接是不是已经被处理过了
            if (isLinkProcessed(connection, link)) {
                continue;
            }
            if (IsInterestedLink(link)) {
                //这是我们感兴趣的，我们只处理新浪站内的链接
                Document doc = httpGetAndParseHtml(link);
                //找到有用的a链接且放进链接池
                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);
                //假如这是一个新闻的详情页面，就存入数据库，否则什么都不做
                storeIntoDatabaseIfItIsNews(doc);
                //把已经处理过的链接放进LINKS_ALREADY_PROCESSED
                insertLinkIntoDatabase(connection, link, "insert into LINKS_ALREADY_PROCESSED (link) values (?)");


            }


        }
    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (IsInterestedLink(href)) {
                insertLinkIntoDatabase(connection, href, "insert into LINKS_TO_BE_PROCESSED (link) values (?)");
            }
        }
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    private static Connection getConnection() throws SQLException {
        File projectDir = new File(System.getProperty("basedir", System.getProperty("user.dir")));
        String jdbcUrl = "jdbc:h2:file:" + new File(projectDir, "news").getAbsolutePath();
        return DriverManager.getConnection(jdbcUrl, USER_NAME, USER_PASSWORD);
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
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

    private static void insertLinkIntoDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }


    private static void storeIntoDatabaseIfItIsNews(Document doc) {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.79 Safari/537.36");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            System.out.println(link);
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);

            return Jsoup.parse(html);
        }
    }

    private static boolean IsNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

    private static boolean IsInterestedLink(String link) {
        return IsNotLoginPage(link)
                && (IsNewsPage(link) || IsIndexPage(link));
    }

    private static boolean IsIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean IsNewsPage(String link) {
        return link.contains("news.sina.cn");
    }


}
