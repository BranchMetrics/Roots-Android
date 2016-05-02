package io.branch.roots;

/**
 * Created by sojanpr on 4/28/16.
 * <p>
 * Class for representing content of a URL
 * </p>
 */
class URLContent {

    private final String contentType_;
    private final String contentEncoding_;
    private String htmlSource_;

    public URLContent(String contentType) {
        contentType_ = contentType;
        contentEncoding_ = "utf-8";
    }

    public void setHtmlSource_(String htmlSource) {
        htmlSource_ = htmlSource;
    }

    public String getContentEncoding() {
        return contentEncoding_;
    }

    public String getContentType() {
        return contentType_;
    }

    public String getHtmlSource() {
        return htmlSource_;
    }

}
