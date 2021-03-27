package com.ohjelmointi3.chatserver;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpContext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

// curl -k -d "es" -u "asd:qwe" https://localhost:8001/chat -H "Content-Type:application/json"
// curl -k -u "asd:qwe" https://localhost:8001/chat
// curl -k -d "asd:qwe" https://localhost:8001/registration -H "Content-Type:application/json"
// cd C:\git\ohjelmointi3\O3-chat-client-main & mvn test -Dtestsettings="test-config-1.xml"

public class ChatServer {
    public static void main(String[] args) throws Exception {
        try {
            log("Launching server");
            ChatDatabase.getInstance().open("test.db");
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            SSLContext sslContext = chatServerSSLContext();
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            ChatAuthenticator authenticator = new ChatAuthenticator();
            HttpContext chatContext = server.createContext("/chat", new ChatHandler());
            chatContext.setAuthenticator(authenticator);
            server.createContext("/registration", new RegistrationHandler(authenticator));

            server.setExecutor(null);
            log("Starting server");
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void log(String message) {
        System.out.println(LocalDateTime.now() + ": " + message);
    }

    private static SSLContext chatServerSSLContext()
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException,
            CertificateException, FileNotFoundException, IOException {
        char[] passphrase = "asdasd".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("keystore.jks"), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }
}
