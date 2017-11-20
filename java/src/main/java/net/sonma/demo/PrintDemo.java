package net.sonma.demo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * User: wanhongming
 * Date: 2017-06-03
 * Time: 下午10:19
 */
public class PrintDemo {

    private static final Logger logger = LoggerFactory.getLogger(PrintDemo.class);

    public static void main(String[] args) throws Exception {
        PrintDemo demo = new PrintDemo(Env.DEMO_ONLINE);

        //使用accessKey secretKey鉴权
        String json = IOUtils.toString(ClassLoader.getSystemResourceAsStream("message.json"), "utf-8");
        JsonObject result = demo.print(123456789, json, 10086L, null);
        logger.info("打印结果:{}", result.get("message"));


        //生成token
        String token = demo.createToken("*", TimeUnit.HOURS.toSeconds(1));
        logger.info("生成的token:{}", token);


        //使用token进行鉴权
        result = demo.print(123456789, json, 10086L, token);
        logger.info("打印结果:{}", result.get("message"));
    }

    private Env env;
    private OkHttpClient client;


    public PrintDemo(Env env) {
        this.env = env;
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(logger::info);
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        this.client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
    }

    /**
     * @param sn       打印机编号
     * @param content  打印内容
     * @param template 模板编号/模板内容/模板URL, 如不传此参数会直接解析content
     * @param token    鉴权Token
     * @link http://docs.sonma.net/zh_CN/latest/interface.html#id3
     */
    public JsonObject print(long sn, String content, Long template, String token) {
        SortedMap<String, String> params = new TreeMap<>();
        params.put(API.PARAMS.SN, String.valueOf(sn));
        params.put(API.PARAMS.CONTENT, content);
        if (template != null) {
            params.put(API.PARAMS.TEMPLATE, template.toString());
        }
        if (token != null) {
            params.put(API.PARAMS.TOKEN, token);
        }
        return process("POST", API.PRINT, params, token == null);
    }

    /**
     * 使用token鉴权时需要将token生成坐在在服务端,供客户端调用
     *
     * @param scope   权限范围控制: * 表示所有打印机, 或者 [1002123456] 表示打印机组
     * @param seconds 有效期
     */
    public String createToken(String scope, long seconds) {
        //请求参数
        SortedMap<String, String> params = new TreeMap<>();
        params.put(API.PARAMS.SCOPE, scope);
        //过期时间
        long exp = Instant.now().getEpochSecond() + seconds;
        params.put(API.PARAMS.EXP, String.valueOf(exp));

        return process("GET", API.ACCESS_TOKEN, params, true).get("token").getAsString();
    }


    /**
     * 创建规范查询字符串
     *
     * @param params 待转换的参数列表
     * @return 转换之后的字符串
     */
    private String createQueryString(SortedMap<String, String> params) {
        StringBuilder queryString = new StringBuilder();
        params.forEach((key, value) -> {
            if (queryString.length() != 0) {
                queryString.append("&");
            }
            queryString.append(SignatureUtil.encodeRFC3986(key)).
                    append("=").
                    append(SignatureUtil.encodeRFC3986(value));
        });
        return queryString.toString();
    }


    private String createAuthorization(long timestamp, String canonicalQueryString) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        logger.info("规范查询字符串(CanonicalQueryString):" + canonicalQueryString);
        String hashedQueryString = SignatureUtil.sha1AsHex(canonicalQueryString);
        logger.info("规范查询字符串哈希(HashedCanonicalQueryString):" + hashedQueryString);
        String stringToSign = timestamp + "\n" + hashedQueryString;
        logger.info("待签字符串(StringToSign):" + stringToSign);
        String signature = SignatureUtil.macSignature(stringToSign, env.getSecretKey());
        logger.info("签名(Signature):" + signature);
        String authorization = Base64.getEncoder().encodeToString(String.format("HMAC-SHA1 %s:%s", env.getAccessKey(), signature).getBytes("UTF-8"));
        logger.info("鉴权字符串(Authorization):" + authorization);
        return authorization;
    }


    /**
     * @param method 请求方法
     * @param params 排序后的参数列表
     * @param sign   当参数不使用token时,需要使用对称加密的方式加签鉴权
     */
    private JsonObject process(String method, String api, SortedMap<String, String> params, boolean sign) {
        try {

            String queryString = createQueryString(params);

            Request.Builder builder = new Request.Builder();

            if (sign) {
                long timestamp = Instant.now().getEpochSecond();
                String authorization = createAuthorization(timestamp, queryString);
                builder.header(API.HEADER.AUTHORIZATION, authorization).header(API.HEADER.TIMESTAMP, String.valueOf(timestamp));
            }

            switch (method) {
                case "GET":
                    builder.url(MessageFormat.format("{0}{1}?{2}", env.getHost(), api, queryString)).get();
                    break;
                case "POST":
                    builder.url(MessageFormat.format("{0}{1}", env.getHost(), api)).post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), queryString));
                    break;
                default:
                    throw new RuntimeException(MessageFormat.format("{0} is not implemented", method));
            }

            try (Response response = client.newCall(builder.build()).execute()) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    if (response.isSuccessful()) {
                        return new JsonParser().parse(responseBody.string()).getAsJsonObject();
                    } else {
                        throw new RuntimeException(responseBody.string());
                    }
                } else {
                    throw new RuntimeException("empty response body");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
