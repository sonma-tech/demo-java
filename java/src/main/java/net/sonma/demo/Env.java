package net.sonma.demo;

public enum Env {
    DEMO_ONLINE("123456789", "123456789", API.HOST);

    private String accessKey;
    private String secretKey;
    private String host;

    Env(String accessKey, String secretKey, String host) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.host = host;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getHost() {
        return host;
    }
}