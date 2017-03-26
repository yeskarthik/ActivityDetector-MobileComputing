package edu.asu.mc.group30;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by yeskarthik on 3/6/2017.
 */
public class DbDownloader extends AsyncTask<String, Void, Void> {
    private TaskDelegate delegate;

    private static final String SERVER_URL = "https://impact.asu.edu/CSE535Spring17Folder/group30.db";

    public void setDelegate(TaskDelegate delegate) {
        this.delegate=delegate;
    }

    public void setupHttpsCertificates()
    {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }
        } };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void downloadDb(String filename) {
        setupHttpsCertificates();
        File file = new File(Environment.getExternalStorageDirectory()+"/"+filename);

        InputStream inputStream = null;
        OutputStream outputStream = null;
        HttpsURLConnection httpsURLConnection = null;
        try {
            URL url = new URL(SERVER_URL);
            httpsURLConnection = (HttpsURLConnection) url.openConnection();

            httpsURLConnection.connect();

            if (httpsURLConnection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                System.out.println( "Response from server " + httpsURLConnection.getResponseCode() + " " + httpsURLConnection.getResponseMessage());
                return;
            }
            System.out.println("Response for download");
            System.out.println(httpsURLConnection.getResponseCode() + httpsURLConnection.getResponseMessage());

            inputStream = httpsURLConnection.getInputStream();
            outputStream = new FileOutputStream(file);
            byte data[] = new byte[4096];
            int count;
            while ((count = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, count);
            }
        } catch (Exception e) {
            return ;
        } finally {
            try {
                if (outputStream != null)
                    outputStream.close();
                if (inputStream != null)
                    inputStream.close();
                if (httpsURLConnection != null)
                    httpsURLConnection.disconnect();
            } catch (Exception e) {
            }
        }
    }


    @Override
    protected Void doInBackground(String... filenames) {
        downloadDb(filenames[0]);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        this.delegate.taskCompleteResult("Downloaded");
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}
