package com.kilogramm.mattermost.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.model.fromnet.InviteObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kepar on 17.11.16.
 */

public abstract class HeaderFooterRecyclerArrayAdapter<VH extends RecyclerView.ViewHolder, T extends Object> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<T> items;
    private List<View> headers = new ArrayList<>();
    private List<View> footers = new ArrayList<>();

    private static final int TYPE_HEADER = 111;
    private static final int TYPE_FOOTER = 222;
    private static final int TYPE_ITEM = 333;

    public HeaderFooterRecyclerArrayAdapter() {
        items = new ArrayList<>();
    }

    public abstract VH onCreateGenericViewHolder(ViewGroup parent, int viewType);

    public abstract void onBindGenericViewHolder(VH holder, int position);

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            return onCreateGenericViewHolder(parent, viewType);
        } else {
            FrameLayout frameLayout = new FrameLayout(parent.getContext());
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new HeaderFooterViewHolder(frameLayout);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position < headers.size()) {
            View v = headers.get(position);
            prepareHeaderFooter((HeaderFooterViewHolder) holder, v);
        } else if (position >= headers.size() + items.size()) {
            View v = footers.get(position - items.size() - headers.size());
            prepareHeaderFooter((HeaderFooterViewHolder) holder, v);
        } else {
            onBindGenericViewHolder((VH) holder, position - headers.size());
        }
    }

    @Override
    public int getItemCount() {
        return headers.size() + items.size() + footers.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < headers.size()) {
            return TYPE_HEADER;
        } else if (position >= headers.size() + items.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    public void add(T item){
        if(items == null) items = new ArrayList<T>();
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public T getItem(int position){
        return items.get(position);
    }

    public List<T> getData(){
        return items;
    }

    public void removeItem(int position){
        if(items.size() <= position) return;
        items.remove(position);
        notifyItemRemoved(position);
    }

    public int getHeaderItemCount(){
        return headers.size();
    }

    public int getFooterItemCount(){
        return footers.size();
    }

    private void prepareHeaderFooter(HeaderFooterViewHolder vh, View view){
        vh.base.removeAllViews();
        if(view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        vh.base.addView(view);
    }

    public void addHeader(View header){
        if(!headers.contains(header)){
            headers.add(header);
            notifyItemInserted(headers.size() - 1);
        }
    }

    public void removeHeader(View header){
        if(headers.contains(header)){
            notifyItemRemoved(headers.indexOf(header));
            headers.remove(header);
            if(header.getParent() != null) {
                ((ViewGroup) header.getParent()).removeView(header);
            }
        }
    }

    public void addFooter(View footer){
        if(!footers.contains(footer)){
            footers.add(footer);
            notifyItemInserted(headers.size()+items.size()+footers.size()-1);
        }
    }

    public void removeFooter(View footer){
        if(footers.contains(footer)) {
            notifyItemRemoved(headers.size()+items.size()+footers.indexOf(footer));
            footers.remove(footer);
            if(footer.getParent() != null) {
                ((ViewGroup) footer.getParent()).removeView(footer);
            }
        }
    }

    public static class HeaderFooterViewHolder extends RecyclerView.ViewHolder {
        FrameLayout base;
        public HeaderFooterViewHolder(View itemView) {
            super(itemView);
            this.base = (FrameLayout) itemView;
        }
    }
}