package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by neshati on 2/5/2017.
 * Behpardaz
 */
public class Util {
    private static Util util;
    public static Util getInstance(){
        if(util == null)
            util = new Util();
        return util;
    }


    public String callGetService(String urladdress) {

        StringBuilder out = new StringBuilder();
        try {

            URL url = new URL(urladdress);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                out.append(output).append("\r\n");
            }

            conn.disconnect();
            return out.toString().trim();

        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;

    }


}
