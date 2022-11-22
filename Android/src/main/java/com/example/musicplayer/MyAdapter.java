package com.example.musicplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class MyAdapter<T> extends BaseAdapter implements Filterable {


    private Context context;
    private ArrayList<T> mOriginalValues;
    private List<T> mObjects;

    private final Object mLock = new Object();

    private LayoutInflater mInflater;

    private String keyWrold = ""; // 关键字

    public MyAdapter(Context context, List<T> list) {
        this.context = context;
        this.mObjects = list;
        this.mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        if (mObjects == null || mObjects.size() == 0)
            return 0;
        return mObjects.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item,parent, false);
            viewHolder = new ViewHolder();
            viewHolder.txtv = (TextView) convertView.findViewById(R.id.txtv);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String str = mObjects.get(position).toString().toLowerCase();



        if (!keyWrold.equals("")) {
            String patten = "" + keyWrold;
            Pattern p = Pattern.compile(patten);
            Matcher m = p.matcher(str);
            SpannableString spannableString = new SpannableString(str);
            while (m.find()) {
                if (str.contains(m.group())) {
                    spannableString.setSpan(
                            new ForegroundColorSpan(0xffec8b44), m.start(),
                            m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            viewHolder.txtv.setText(spannableString);
        } else {
            SpannableString spannableString = new SpannableString(str);
            spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0,
                    str.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            viewHolder.txtv.setText(mObjects.get(position).toString());
        }
        return convertView;
    }

    class ViewHolder {
        TextView txtv;
    }

    @Override
    public Filter getFilter() {
        // TODO Auto-generated method stub
        return filter;
    }

    Filter filter = new Filter() {
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            mObjects = (List<T>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            // TODO Auto-generated method stub
            FilterResults filterResults = new FilterResults();

            if (mOriginalValues == null) {
                synchronized (mLock) {
                    mOriginalValues = new ArrayList<T>(mObjects);
                }
            }

            if (prefix == null || prefix.length() == 0) {
                ArrayList<T> list;
                synchronized (mLock) {
                    list = new ArrayList<T>(mOriginalValues);
                }
                filterResults.values = list;
                filterResults.count = list.size();
                keyWrold = "";
            } else {
                String prefixString = prefix.toString().toLowerCase();
                ArrayList<T> values;
                synchronized (mLock) {
                    values = new ArrayList<T>(mOriginalValues);
                }

                final int count = values.size();
                final ArrayList<T> newValues = new ArrayList<T>();

                for (int i = 0; i < count; i++) {
                    final T value = values.get(i);
                    final String valueText = value.toString().toLowerCase();
                    // First match against the whole, non-splitted value
                    if (valueText.contains(prefixString)) {
                        newValues.add(value);
                        keyWrold = prefixString;
                    }
                }

                filterResults.values = newValues;
                filterResults.count = newValues.size();
            }

            return filterResults;
        }

    };

}