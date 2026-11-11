package com.example.carworkshopbookingapp.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.util.ArrayList;
import java.util.List;

public final class ServiceAdapter extends BaseAdapter {
    public interface OnBook { void select(ServiceItem service); }
    private final com.example.carworkshopbookingapp.util.ListMotion motion = new com.example.carworkshopbookingapp.util.ListMotion();
    private final LayoutInflater inflater;
    private final OnBook listener;
    private List<ServiceItem> items = new ArrayList<>();

    public ServiceAdapter(Context context, OnBook listener) {
        inflater = LayoutInflater.from(context); this.listener = listener;
    }
    public void submit(List<ServiceItem> items) { this.items = items; notifyDataSetChanged(); }
    @Override public int getCount() { return items.size(); }
    @Override public ServiceItem getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    private static final class Holder {
        final TextView name, description, price;
        final Button book;
        Holder(View view) {
            name = view.findViewById(R.id.service_name); description = view.findViewById(R.id.service_description);
            price = view.findViewById(R.id.service_price); book = view.findViewById(R.id.book_service);
        }
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_service, parent, false);
            holder = new Holder(convertView); convertView.setTag(holder);
        } else { holder = (Holder) convertView.getTag(); }
        ServiceItem item = getItem(position);
        holder.name.setText(item.name); holder.description.setText(item.description);
        holder.price.setText(convertView.getContext().getString(R.string.service_price_duration,
                WorkshopDisplay.money(item.basePrice), item.durationMin));
        holder.book.setOnClickListener(view -> listener.select(item));
        motion.bind(convertView, item.id);
        return convertView;
    }
}
