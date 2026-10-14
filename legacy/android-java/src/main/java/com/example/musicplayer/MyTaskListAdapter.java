package com.example.musicplayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.musicplayer.TaskModel;

/**
 * 自定义具有过滤功能的adapter类
 *
 */
public class MyTaskListAdapter<TaskModel> extends BaseAdapter implements Filterable {
    private Context context;
    private List<String> mDatas;

    private final Object mLock = new Object();



    public MyTaskListAdapter(Context context, List<String> Strings) {
        this.context = context;
        this.mDatas = Strings;

    }

    private ArrayList<TaskModel> mOriginalValues;
    private ArrayFilter mFilter;

    public  Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return null;
    }


    private class ArrayFilter extends Filter {
        /**
         * 执行过滤的方法
         * @param prefix
         * @return
         */
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            // 过滤的结果
            FilterResults results = new FilterResults();
            // 原始数据备份为空时，上锁，同步复制原始数据
            if (mOriginalValues == null) {
                synchronized (mLock) {
                    mOriginalValues = (ArrayList<TaskModel>) new ArrayList<String>(mDatas);
                }
            }
            // 当首字母为空时
            if (prefix == null || prefix.length() == 0) {
                ArrayList<TaskModel> list;
                // 同步复制一个原始备份数据
                synchronized (mLock) {
                    list = new ArrayList<>(mOriginalValues);
                }
                // 此时返回的results就是原始的数据，不进行过滤
                results.values = list;
                results.count = list.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();

                ArrayList<TaskModel> values;
                // 同步复制一个原始备份数据
                synchronized (mLock) {
                    values = new ArrayList<>(mOriginalValues);
                }
                final int count = values.size();
                final ArrayList<TaskModel> newValues = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    // 从List<TaskModel>中拿到TaskModel对象
//                    final TaskModel value = values.get(i);
                    final com.example.musicplayer.TaskModel value = (com.example.musicplayer.TaskModel) values.get(i);

                    // TaskModel对象的任务名称属性作为过滤的参数
                    final String valueText = value.getTitle().toString().toLowerCase();
                    // 关键字是否和item的过滤参数匹配
                    if (valueText.indexOf(prefixString.toString()) != -1) {
                        // 将这个item加入到数组对象中
                        newValues.add((TaskModel) value);
                    } else {
                        // 处理首字符是空格
                        final String[] words = valueText.split(" ");
                        final int wordCount = words.length;

                        for (int k = 0; k < wordCount; k++) {
                            // 一旦找到匹配的就break，跳出for循环
                            if (words[k].indexOf(prefixString) != -1) {
                                newValues.add((TaskModel) value);
                                break;
                            }
                        }
                    }
                }
                // 此时的results就是过滤后的List<TaskModel>数组
                results.values = newValues;
                results.count = newValues.size();
            }
            return results;
        }


        /**
         * 得到过滤结果
         *
         * @param prefix
         * @param results
         */
        @Override
        protected void publishResults(CharSequence prefix, FilterResults results) {
            // 此时，Adapter数据源就是过滤后的Results
            mDatas = (List<String>) results.values;
            if (results.count > 0) {
                // 这个相当于从mDatas中删除了一些数据，只是数据的变化，故使用notifyDataSetChanged()
                notifyDataSetChanged();
            } else {
                // 当results.count<=0时，此时数据源就是重新new出来的，说明原始的数据源已经失效了
                notifyDataSetInvalidated();
            }
        }
    }


}

