package com.github.hcsp;


public class News {
    private Integer id;
    private String url;
    private String content;

    public News(String url, String content, String title) {
        this.url = url;
        this.content = content;
        this.title = title;
    }

    private String title;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
