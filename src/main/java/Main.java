import com.google.gson.Gson;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.openqa.selenium.support.locators.RelativeLocator.with;

public class Main {
    public static void main(String[] args) {
        int port = 9090;
        try {
            var serverSocket = new ServerSocket(port);
            System.out.println("сокет активен");
            boolean firstRequest = true;
            while (true) {
                var socket = serverSocket.accept();
                var inputSocket = socket.getInputStream();
                var inStream = new DataInputStream(inputSocket);

                System.setProperty("webdriver.gecko.driver", "/home/piccodi/Drivers/geckodriver");

                String reference = inStream.readUTF();
                System.out.println(reference);

                WebDriver driver = new FirefoxDriver();
                driver.manage().window().maximize();
                JavascriptExecutor jse = (JavascriptExecutor) driver;
                Main.parse(driver, jse, reference, firstRequest);
                if(firstRequest) firstRequest = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static void parse(WebDriver driver, JavascriptExecutor jse, String lastRef, boolean firstRequest){
        try{

            driver.get("http://www.reddit.com/r/memes/new/");

            List<MemeModel> images = new ArrayList<>();
            var element =  Optional.of(driver.findElement(By.cssSelector("img[alt = 'Post image']")));
            Optional<WebElement> nextElem;
            int i = 0;
            Boolean alreadyFoundedImage = false;
            while (true){

                nextElem = Main.getElement(driver, element.get());
                if(nextElem.isEmpty()){
                    while(true){
                        jse.executeScript("arguments[0].scrollIntoView();", element.get());
                        jse.executeScript("window.scrollBy(0, 1200)");
                        Thread.sleep(100);
                        nextElem = Main.getElement(driver, element.get());
                        if(nextElem.isPresent()){break;}
                    }
                }
                if((firstRequest && i == 10) || nextElem.get().getAttribute("src").equals(lastRef)) break;

                if(!nextElem.get().getAttribute("src").startsWith("https://ex")){
                    images.add(MemeModel.setModel(nextElem.get().getAttribute("src"), 0, 0));
                    i++;
                }
                element = nextElem;
            }

            for (int j = images.size() - 1; j >= 0; j--) {
                driver.get(images.get(j).getReference());
                var img = driver.findElement(By.cssSelector("img"));
                System.out.println(img.getAttribute("src") + " --- " + img.getSize().height + " : " + img.getSize().width);
                images.get(j).setLength(img.getSize().height);
                images.get(j).setWidth(img.getSize().width);
            }
            System.out.println(images.size());
            driver.close();

            String url_query = "http://localhost:8080/memes";
            URL url = new URL(url_query);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setDoInput(true);
            http.addRequestProperty("Content-Type", "application/json; charset=UTF-8");

            String json = new Gson().toJson(images);
            System.out.println(json);

            OutputStream outputStream = http.getOutputStream();
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
            outputStream.close();

            InputStream in = new BufferedInputStream(http.getInputStream());
            String response = Arrays.toString(in.readAllBytes());
            System.out.println(response);


        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public static Optional<WebElement> getElement(WebDriver driver, WebElement previousElem){
        try{
            return Optional.of(driver.findElement(with(By.cssSelector("img[alt = 'Post image']")).below(previousElem)));
        }catch (Exception e) {
            return Optional.empty();
        }
    }
}
