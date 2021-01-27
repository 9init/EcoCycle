package com.lilHesham.ecocycle;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

final class utils {
    List<PlasticInfo> extractInfo(String strData){
        String[] separatedData = strData.trim().split(",");
        ArrayList<PlasticInfo> plasticInfoList = new ArrayList<>();
        for(int i = 0; i<separatedData.length; i=i+2){
            PlasticInfo plastic = new PlasticInfo(Double.parseDouble(separatedData[i]), separatedData[i+1]);
            plasticInfoList.add(plastic);
        }
        return plasticInfoList;
    }

    boolean permittedAccess(String url) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(5000); //set timeout to 5 seconds
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }catch (Exception e){
            return false;
        }
    }
}
