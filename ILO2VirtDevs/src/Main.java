import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import mjson.Json;
import sun.misc.BASE64Encoder;

import com.hp.ilo2.virtdevs.virtdevs;


public class Main {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko";

    private static final String COOKIE_FILE = "data.cook";

    private static String username = "";
    private static String password = "";
    private static String hostname = "";

    public static URL baseURL;
    public static boolean appletActive;

    public static void setHostname(String hostname) {
        Main.hostname = hostname;
        Main.loginURL = "https://" + hostname + "/login.htm";
    }

    private static String loginURL = "";

    private static String sessionKey = "";
    private static String sessionIndex = "";
    private static String supercookie = "";

    private static CookieManager cookieManager = new CookieManager();


    private static void Stage1() throws Exception {
        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();
        URL obj = new URL(loginURL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        //con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Referer", loginURL);
        con.setRequestProperty("Host", hostname);
        con.setRequestProperty("Accept-Language", "de-DE");
        con.setRequestProperty("Cookie", "hp-iLO-Login=");


        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String res = response.toString();
        sessionKey = res.split("var sessionkey=\"")[1].split("\";")[0];
        sessionIndex = res.split("var sessionindex=\"")[1].split("\";")[0];
        System.out.println("Session key: " + sessionKey);
        System.out.println("Session  ID: " + sessionIndex);
    }


    private static void Stage2() throws Exception {
        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();

        URL obj = new URL("https://" + hostname + "/index.htm");

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Referer", loginURL);
        con.setRequestProperty("Host", hostname);
        con.setRequestProperty("Accept-Language", "de-DE");
        //Cookie:
        con.setDoOutput(true);
        BASE64Encoder enc = new BASE64Encoder(); //Authenticate

        con.setRequestProperty("Cookie", "hp-iLO-Login=" + sessionIndex + ":" + enc.encode(username.getBytes()) + ":" + enc.encode(password.getBytes()) + ":" + sessionKey);


        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));

        //noinspection StatementWithEmptyBody
        while (in.readLine() != null) { } // discard
        in.close();

        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        PrintWriter writer = new PrintWriter(COOKIE_FILE, "UTF-8");
        for (HttpCookie cookie : cookies) {
            System.out.format("Session cookie: %s: %s\n", cookie.getDomain(), cookie);
            writer.println(cookie.toString().replace("\"", ""));
        }
        writer.close();

    }


    public static HashMap<String, String> hmap = new HashMap<>();

    private static void Stage3() throws Exception {
        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();
        String url = "https://" + hostname + "/vtd028.htm";
        URL obj = new URL(url);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        //con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Referer", loginURL);
        con.setRequestProperty("Host", hostname);
        con.setRequestProperty("Accept-Language", "de-DE");
        if(supercookie != "") {
            con.setRequestProperty("Cookie", supercookie);
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String res = response.toString();

        hmap.put("INFO0", res.split("info0v=\"")[1].split("\";")[0]);
        hmap.put("INFO1", res.split("info1v=\"")[1].split("\";")[0]);
        hmap.put("INFO2", res.split("usbcfg=\"")[1].split("\";")[0]);
        hmap.put("INFO3", res.split("serverName=\"")[1].split("\";")[0]);
        if (!res.split("dp=")[1].split(";")[0].equals("0"))
            hmap.put("device", res.split("dp=")[1].split(";")[0]);

        /*hmap.put("hostAddress", );
        hmap.put("floppy", );
        hmap.put("cdrom", );
        hmap.put("config", );
        hmap.put("UNIQUE_FEATURES", );*/

        System.out.println("CABBASE = " + hmap.get("CABBASE"));
    }


    public static boolean isValid(String cookie) throws Exception {
        CookieHandler.setDefault(cookieManager);
        String url = "https://" + hostname + "/ie_index.htm";
        URL obj = new URL(url);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        //con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Referer", loginURL);
        con.setRequestProperty("Host", hostname);
        con.setRequestProperty("Accept-Language", "de-DE");
        con.setRequestProperty("Cookie", cookie);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String res = response.toString();

        return !(res.contains("Login Delay") || res.contains("Integrated Lights-Out 2 Login"));
    }


    public static void main(String[] args) {
        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();
        CookieHandler.setDefault(cookieManager);
        try {
            String config = new String(Files.readAllBytes(Paths.get("config.json")));
            System.out.println("Config JSON:" + config);
            Json js = Json.read(config);
            username = js.at("Username").asString();
            password = js.at("Password").asString();
            setHostname(js.at("Hostname").asString());
        } catch (Exception e) {
            System.err.println("Error in parsing config file!");
            e.printStackTrace();
            return;
        }
        try {
            try (BufferedReader br = new BufferedReader(new FileReader("data.cook"))) {
                System.out.println("Found datastore");
                String line;
                String lastline = "";
                while ((line = br.readLine()) != null) {
                    cookieManager.getCookieStore().add(new URI("https://" + hostname), new HttpCookie(line.split("=")[0], line.split("=")[1]));
                    lastline = line;
                }

                if(!isValid(lastline)) {
                    System.out.println("Datastore not valid, requesting Cookie");
                    Stage1();
                    Stage2();
                } else {
                    supercookie = lastline;
                }
            } catch (FileNotFoundException e) {
                System.out.println("Couldn't find datastore, requesting Cookie");
                Stage1();
                Stage2();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Stage3();
            //hmap.put("IPADDR", hostname);
            //hmap.put("DEBUG", "suckAdIck");

            Main.baseURL = new URL("https://" + hostname + "/vtd028.htm");
            virtdevs vd = new virtdevs();
            vd.setStub(new VirtDevsAppletStub());

            JFrame jf = new JFrame();
            Container c = jf.getContentPane();
            jf.setBounds(0, 0, 500,300);
            jf.setVisible(true);
            c.add(vd);

            vd.init();
            Main.appletActive = true;
            vd.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
