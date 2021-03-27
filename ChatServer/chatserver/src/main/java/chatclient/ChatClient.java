package chatclient;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class ChatClient {

    public static void main( String[] args ) throws Exception {

        boolean usingClient = true;


        URL url;
        String certificateLocation = "C:\\git\\ohjelmointi3\\O3-chat-client-main\\localhost.cer";

        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(
                new FileInputStream(certificateLocation));
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null, null);
        keystore.setCertificateEntry("localhost", certificate);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keystore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        
        HttpsURLConnection connection;
        byte[] out = null;
        
        Scanner scanner = new Scanner(System.in);
        String username = null;
        String password = null;
        

        while(usingClient){
            System.out.println("Type <registration> or <chat> or <quit> to continue");
            String answer = scanner.nextLine();

            if(answer.equals("quit")){
                usingClient = false;
            }
            if(answer.equals("registration")){
                url = new URL("https://localhost:8001/registration");

                System.out.println("Give username");
                String username = scanner.nextLine();
                System.out.println("Give password");
                String password = scanner.nextLine();
                System.out.println("Give email");
                String email = scanner.nextLine();

                connection = (HttpsURLConnection)url.openConnection();
                connection.setSSLSocketFactory(sslContext.getSocketFactory());

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                String jsonString = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"email\":\"" + email + "\"}";
                byte[] jsonBytes = jsonString.getBytes("UTF-8");
                int bytesLenght = jsonBytes.length;

                connection.setFixedLengthStreamingMode(bytesLenght);
                connection.setRequestProperty("Content-Type", "application/json");

                connection.connect();
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(jsonBytes);
                outputStream.close();
            }
            if(answer.equals("chat")){
                url = new URL("https://localhost:8001/chat");

                if(password == null || username == null){
                    System.out.println("Give username");
                    username = scanner.nextLine();
                    System.out.println("Give password");
                    password = scanner.nextLine();
                }

                System.out.println("<get> or <post> messages");
                answer = scanner.nextLine();

                connection = (HttpsURLConnection)url.openConnection();
                connection.setSSLSocketFactory(sslContext.getSocketFactory());
                connection.setRequestProperty("Content-Type", "application/json");
                String credentialsEncoding = Base64.getEncoder().encodeToString((username + ":" + password).getBytes("UTF-8"));
                connection.setRequestProperty("Authorization", "Basic " + credentialsEncoding);
                

                if(answer.equals("get")){
                    connection.setDoInput(true);
                    connection.setRequestMethod("GET");

                    connection.connect();
                    InputStream inputStream = connection.getInputStream();

                    inputStream.close();

                    System.out.println(inputStream);
                }
                if(answer.equals("post")){
                    System.out.println("Give message");
                    String message = scanner.nextLine();
    
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    
    
                    String jsonString = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"message\":\"" + message + "\"}";
                    byte[] jsonBytes = jsonString.getBytes("UTF-8");
                    int bytesLenght = jsonBytes.length;
    
                    connection.setFixedLengthStreamingMode(bytesLenght);
                    
                    connection.connect();
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(jsonBytes);
                    outputStream.close();
                }


            }
            
        }
    }
}
