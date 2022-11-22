package com.example.musicplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayListMapAdapter {
    private Map<String,String> listMap = new HashMap<>();
    private List<String> listForSong;
    private List<String> pathList;
    private List<String> nameList = new ArrayList<String>();


    public PlayListMapAdapter(List<String> listSong){
        this.listForSong=listSong;
        this.pathList=listSong;

    }
    public PlayListMapAdapter(){
    }

    public Map<String,String> listToMap(){

        if (listForSong != null){
            int count = listForSong.size();
            for (String s : listForSong) {
                // 按指定模式在字符串查找
                String patternName = "[^\\/\\\\]+$";
                // 创建 Pattern 对象
                Pattern r = Pattern.compile(patternName);
                // 现在创建 matcher 对象
                Matcher m = r.matcher(s);
                if (m.find( )) {
                    String name=m.group(0);
                    String nameKey =name.substring(0, name.lastIndexOf("."));
                    listMap.put(nameKey,s);
                    nameList.add(nameKey);
//                    System.out.println(" listMap: " + listMap);
                } else {
                    System.out.println("NO MATCH");
                }
            }
        }
        return listMap;
    }
    public List<String> getNameList(){
        return nameList;
    }
    public List<String> getPathList(){
        return pathList;
    }

    public Map<String,Integer> getMapToOrderMap(Map<String,String> MapForList) {
        Map<String,Integer> MapToOrderMap=new HashMap<>();
        int i = 0;
        for (String value : MapForList.values()) {
//            System.out.println("value = " + value);
            MapToOrderMap.put(value,i);
            i++;
        }


        return MapToOrderMap;
    }
}
