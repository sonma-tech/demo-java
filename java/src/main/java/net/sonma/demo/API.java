package net.sonma.demo;

/**
 * Description:
 * User: wanhongming
 * Date: 2017-06-05
 * Time: 上午9:48
 */
public interface API {
    String HOST = "https://api.sonma.net";
    String PRINT = HOST + "/v1/print";
    String ACCESS_TOKEN = HOST + "/v1/auth/access_token";




    interface PARAMS {
        String SN = "sn";
        String CONTENT = "content";
        String TEMPLATE = "template";
        String TOKEN = "token";
        String SCOPE = "scope";
        String EXP = "exp";
    }

    interface HEADER {
        String AUTHORIZATION = "Authorization";
        String TIMESTAMP = "Timestamp";
    }
}
