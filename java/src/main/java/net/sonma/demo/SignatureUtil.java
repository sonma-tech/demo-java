package net.sonma.demo;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class SignatureUtil {

        private final static char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        static String macSignature(String text, String secretKey) throws InvalidKeyException, NoSuchAlgorithmException {
            return hexEncode(hmac(text.getBytes(StandardCharsets.UTF_8), secretKey.getBytes(StandardCharsets.UTF_8)));
        }

        private static byte[] hmac(byte[] text, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
            String algorithm = "HmacSHA1";
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));
            return mac.doFinal(text);
        }


        private static String hexEncode(byte[] data) {
            int l = data.length;
            char[] out = new char[l << 1];
            for (int i = 0, j = 0; i < l; i++) {
                out[j++] = HEX[(0xF0 & data[i]) >>> 4];
                out[j++] = HEX[0x0F & data[i]];
            }
            return new String(out);
        }

        static String sha1AsHex(String data) {
            byte[] dataBytes = getDigest("SHA1").digest(data.getBytes(StandardCharsets.UTF_8));
            return hexEncode(dataBytes);
        }


        static String encodeRFC3986(String str) {
            String result;

            try {
                result = URLEncoder.encode(str, "UTF-8")
                        .replace("+", "%20")
                        .replace("*", "%2A")
                        .replace("%7E", "~");
            }

            // This exception should never occur.
            catch (UnsupportedEncodingException e) {
                result = str;
            }

            return result;
        }


        @SuppressWarnings("SameParameterValue")
        private static MessageDigest getDigest(String algorithm) {
            try {
                return MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + algorithm + "\"", ex);
            }
        }


    }