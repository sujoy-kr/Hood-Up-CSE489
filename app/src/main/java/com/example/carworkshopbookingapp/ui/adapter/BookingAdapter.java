package com.example.carworkshopbookingapp.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.ui.BookingDisplay;
import java.util.ArrayList;
import java.util.List;

public final class BookingAdapter extends BaseAdapter {
    private final com.example.carworkshopbookingapp.util.ListMotion motion = new com.example.carworkshopbookingapp.util.ListMotion();
    private final LayoutInflater inflater;
    private List<Booking> items = new ArrayList<>();
    public BookingAdapter(Context context) { inflater = LayoutInflater.from(context); }
    public void submit(List<Booking> items) { this.items = items; notifyDataSetChanged(); }
    @Override public int getCount() { return items.size(); }
    @Override public Booking getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }
    @Override public View getView(int position, View view, ViewGroup parent) {
        BookingDisplay.Fields holder;
        if (view == null) {
            view = inflater.inflate(R.layout.row_booking, parent, false);
            holder = new BookingDisplay.Fields(view); view.setTag(holder);
        } else holder = (BookingDisplay.Fields) view.getTag();
        holder.bind(getItem(position));
        motion.bind(view, getItem(position).id);
        return view;
    }
}
