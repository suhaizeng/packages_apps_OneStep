package com.smartisanos.sidebar.view;

import android.content.Context;
import android.content.CopyHistoryItem;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.smartisanos.sidebar.R;
import com.smartisanos.sidebar.SidebarController;
import com.smartisanos.sidebar.util.LOG;
import com.smartisanos.sidebar.util.RecentClipManager;
import com.smartisanos.sidebar.util.RecentUpdateListener;
import com.smartisanos.sidebar.util.Utils;

import java.util.List;

import smartisanos.util.SidebarUtils;

public class ClipboardAdapter extends BaseAdapter{
    private static LOG log = LOG.getInstance(ClipboardAdapter.class);

    private Context mContext;
    private RecentClipManager mClipManager;
    private List<CopyHistoryItem> mList;
    private Handler mHandler;
    public ClipboardAdapter(Context context){
        mContext = context;
        mClipManager = RecentClipManager.getInstance(mContext);
        mList = mClipManager.getCopyList();
        mHandler = new Handler(Looper.getMainLooper());
        mClipManager.addListener(new RecentUpdateListener(){
            @Override
            public void onUpdate() {
                mList = mClipManager.getCopyList();
                mHandler.post(new Runnable(){
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        });
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        log.error("getView position ["+position+"]");
        View res = convertView;
        if(res == null){
            res =  View.inflate(mContext,R.layout.copyhistoryitem, null);
        }
        TextView tv = (TextView)res.findViewById(R.id.text);
        tv.setText(mList.get(position).mContent);
        return res;
    }
}

