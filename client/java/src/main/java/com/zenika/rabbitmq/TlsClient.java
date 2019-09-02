package com.zenika.rabbitmq;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;
import com.rabbitmq.client.GetResponse;

/**
 * TLS client example for RabbitMQ
 */
public class TlsClient {
    
    public static void main( String[] args ) throws Exception {
        // TLS Client certificate
        final String pass = (args.length != 0) ? args[0] : "MySecretPassword";
        final char[] keyPassphrase = pass.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new FileInputStream("client_certificate.p12"), keyPassphrase);
        
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keyPassphrase);
        
        
        // TLS server certificate
        TrustManager[] clientTrustManagerList = {
                new X509TrustManager() {
                    //Dummy trust store that trusts any server you connect to.
                    //For demo purposes only
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                }
        };
        
        
        SSLContext tlsContext = SSLContext.getInstance("TLSv1.2");
        tlsContext.init(kmf.getKeyManagers(), clientTrustManagerList, null);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5671);
        factory.setVirtualHost("/");

        System.out.println("Connection created");
        factory.useSslProtocol(tlsContext);
        // Needed to authenticate with client certificate (peer verification)
        factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
        

        Connection conn = factory.newConnection();
        Channel channel = conn.createChannel();
        

        channel.queueDeclare("ssl-client-exclusive-queue", false, true, true, null);
        channel.basicPublish("", "ssl-client-exclusive-queue", null, "Hello, TLS World!".getBytes());

        GetResponse chResponse = channel.basicGet("ssl-client-exclusive-queue", false);
        if(chResponse == null) {
            System.out.println("No message retrieved");
        } else {
            byte[] body = chResponse.getBody();
            System.out.println("Received: " + new String(body));
        }
        
        channel.close();
        conn.close();
    }
    
}
