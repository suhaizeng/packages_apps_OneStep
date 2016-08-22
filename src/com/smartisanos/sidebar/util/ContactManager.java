package com.smartisanos.sidebar.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

public class ContactManager extends DataManager{
    private volatile static ContactManager sInstance;
    public synchronized static ContactManager getInstance(Context context){
        if(sInstance == null){
            synchronized(ContactManager.class){
                if(sInstance == null){
                    sInstance = new ContactManager(context);
                }
            }
        }
        return sInstance;
    }

    private Context mContext;
    private List<ContactItem> mContacts = new ArrayList<ContactItem>();
    private Handler mHandler;
    private ContactManager(Context context){
        mContext = context;
        HandlerThread thread = new HandlerThread(ContactManager.class.getName());
        thread.start();
        mHandler = new ContactManagerHandler(thread.getLooper());
        mHandler.obtainMessage(MSG_READ_CONTACTS).sendToTarget();
    }

    public List<ContactItem> getContactList() {
        List<ContactItem> ret = new ArrayList<ContactItem>();
        synchronized (mContacts) {
            ret.addAll(mContacts);
        }
        return ret;
    }

    private void readContacts(){
        List<ContactItem> list = ContactItem.getContactList(mContext);
        Collections.sort(list, new ContactComparator());
        synchronized(mContacts){
            mContacts.clear();
            mContacts.addAll(list);
        }
        notifyListener();
    }

    public void updateOrder() {
        synchronized (mContacts) {
            Collections.sort(mContacts, new ContactComparator());
        }
        notifyListener();
        mHandler.obtainMessage(MSG_UPDATE_ORDER).sendToTarget();
    }

    public void addContact(ContactItem ci) {
        synchronized (mContacts) {
            for (int i = 0; i < mContacts.size(); ++i) {
                if (ci.sameContact(mContacts.get(i))) {
                    ci.setIndex(mContacts.get(i).getIndex());
                    mContacts.set(i, ci);
                    saveContact(ci);
                    notifyListener();
                    return;
                }
            }
            if (mContacts.size() == 0) {
                ci.setIndex(mContacts.size());
            } else {
                int maxIndex = mContacts.get(0).getIndex();
                for (int i = 1; i < mContacts.size(); ++i) {
                    if (mContacts.get(i).getIndex() > maxIndex) {
                        maxIndex = mContacts.get(i).getIndex();
                    }
                }
                ci.setIndex(maxIndex + 1);
            }
            ci.isNewAdd = true;
            mContacts.add(0, ci);
            notifyListener();
            saveContact(ci);
        }
    }

    public void remove(ContactItem ci) {
        synchronized (mContacts) {
            for (int i = 0; i < mContacts.size(); ++i) {
                if (ci.sameContact(mContacts.get(i))) {
                    mContacts.remove(i);
                    notifyListener();
                    deleteContactFromDatabase(ci);
                    return;
                }
            }
        }
    }

    private void saveContact(ContactItem ci){
        mHandler.obtainMessage(MSG_SAVE_CONTACT, ci).sendToTarget();
    }

    private void deleteContactFromDatabase(ContactItem ci){
        mHandler.obtainMessage(MSG_DELETE_CONTACT, ci).sendToTarget();
    }

    public void onPackageAdded(String packageName){
        // NA
    }

    public void onPackageRemoved(String packageName) {
        boolean deleted = false;
        synchronized (mContacts) {
            for (int i = 0; i < mContacts.size(); ++i) {
                ContactItem ci = mContacts.get(i);
                if (ci.getPackageName().equals(packageName)) {
                    deleted = true;
                    mContacts.remove(i);
                    i--;
                    deleteContactFromDatabase(ci);
                }
            }
        }
        if (deleted) {
            notifyListener();
        }
    }

    public static final class ContactComparator implements Comparator<ContactItem> {
        @Override
        public int compare(ContactItem lhs, ContactItem rhs) {
            if (lhs.getIndex() > rhs.getIndex()) {
                return -1;
            } else if (lhs.getIndex() < rhs.getIndex()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private static final int MSG_SAVE_CONTACT = 0;
    private static final int MSG_DELETE_CONTACT = 1;
    private static final int MSG_UPDATE_ORDER = 2;
    private static final int MSG_READ_CONTACTS = 3;
    private class ContactManagerHandler extends Handler {
        public ContactManagerHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override
        public void handleMessage(Message msg) {
            ContactItem ci = (ContactItem) msg.obj;
            switch (msg.what) {
            case MSG_SAVE_CONTACT:
                ci.save();
                break;
            case MSG_DELETE_CONTACT:
                ci.deleteFromDatabase();
                break;
            case MSG_UPDATE_ORDER:
                synchronized(mContacts){
                    for(ContactItem cni : mContacts){
                        cni.save();
                    }
                }
                break;
            case MSG_READ_CONTACTS:
                readContacts();
            }
        }
    }
}
