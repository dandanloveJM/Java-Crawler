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

import java.io.IOException;
import java.sql.*;
import java.util.stream.Collectors;


public class Crawler {
    CrawlerDao dao = new MybatisCrawlerDao();
    public void run() throws SQLException, IOException {
        String link;
        //从数据库中加载下一个链接，如果能加载到，则进行循环
        while ((link = dao.getNextLinkThenDelete()) != null) {
            //询问数据库当前链接是不是已经被处理过了
            if (dao.isLinkProcessed(link)) {
                continue;
            }
            if (IsInterestedLink(link)) {
                System.out.println(link);
                //这是我们感兴趣的，我们只处理新浪站内的链接
                Document doc = httpGetAndParseHtml(link);
                //找到有用的a链接且放进链接池
                parseUrlsFromPageAndStoreIntoDatabase(doc);
                //假如这是一个新闻的详情页面，就存入数据库，否则什么都不做
                storeIntoDatabaseIfItIsNews(doc, link);
                //把已经处理过的链接放进LINKS_ALREADY_PROCESSED
                dao.insertProcossedLink(link);
                //dao.updateDatabase(link, "insert into LINKS_ALREADY_PROCESSED (link) values (?)");
            }


        }
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (IsInterestedLink(href)) {
                dao.insertLinkToBeProcessed(href);
            }
        }
    }


    private void storeIntoDatabaseIfItIsNews(Document doc, String link) throws SQLException {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                dao.storeNewsIntoDatabase(link, title, content);

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
