package com.github.hcsp;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
        //待处理的链接池
        List<String> linkpool = new ArrayList<>();
        //已经处理的链接池
        Set<String> processedLinks = new HashSet<>();
        linkpool.add("https://sina.cn");
        while (true) {
            if (linkpool.isEmpty()) {
                break;
            }
            //arraylist从尾部删除更有效率, remove会返回删掉的元素
            String link = linkpool.remove(linkpool.size() - 1);
            if (processedLinks.contains(link)) {
                continue;
            }
            if (IsInterestedLink(link)) {
                //这是我们感兴趣的，我们只处理新浪站内的链接
                Document doc = httpGetAndParseHtml(link);
                //找到有用的a链接且放进链接池
                doc.select("a").stream().map(aTag->aTag.attr("href")).forEach(linkpool::add);

                //假如这是一个新闻的详情页面，就存入数据库，否则什么都不做
                storeIntoDatabaseIfItIsNews(doc);
                processedLinks.add(link);
            } else {
                //我们不感兴趣的链接，不处理它
                continue;
            }
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
