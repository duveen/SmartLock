package kr.o3selab.smartlock.common;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class JSONHandler extends AsyncTask<Void, Void, String> {

    private URL url;
    private String param;

    public JSONHandler(String url, String param) {
        try {
            this.url = new URL(url);
            this.param = param;
        } catch (Exception e) {
            Log.e("e", e.getMessage());
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }

    @Override
    protected String doInBackground(Void... params) {
        try {

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setDoOutput(true);
            con.setDoInput(true);

            if(param != null) {
                OutputStream os = con.getOutputStream();
                os.write(param.getBytes("euc-kr"));
                os.flush();
                os.close();
            }

            InputStream is = con.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is, "euc-kr"));
            StringBuilder sb = new StringBuilder();
            String line;

            con.getResponseCode();

            while((line = br.readLine()) != null) {
                sb.append(line);
            }

            br.close();

            return sb.toString();

        } catch (Exception e) {
            Log.e("e", e.getMessage());
            return "error";
        }
    }
}
