package mg.itu.util;

import java.util.Objects;

public class UrlMethod {
    private String url;
    private String method;

    public UrlMethod() {
    }

    public UrlMethod(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public boolean equals(Object object) {
        // if (object instanceof UrlMethod) {
            UrlMethod urlMethod = (UrlMethod) object;

        return this.getUrl().equals(urlMethod.getUrl()) && this.getMethod().equals(urlMethod.getMethod());
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method);
    }

}
