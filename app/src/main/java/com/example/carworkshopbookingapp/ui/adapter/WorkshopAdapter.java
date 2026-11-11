package com.example.carworkshopbookingapp.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.WorkshopBrowser;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import com.example.carworkshopbookingapp.data.model.Workshop;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WorkshopAdapter extends BaseAdapter {
    private final com.example.carworkshopbookingapp.util.ListMotion motion = new com.example.carworkshopbookingapp.util.ListMotion();
    private final LayoutInflater inflater;
    private final Map<String, ServiceItem> services;
    private String requestedServiceId;
    private List<Workshop> items = new ArrayList<>();

    public WorkshopAdapter(Context context, Map<String, ServiceItem> services) {
        inflater = LayoutInflater.from(context);
        this.services = services;
    }

    public void setRequestedService(String serviceId) { requestedServiceId = serviceId; }

    public void submit(List<Workshop> items) { this.items = items; notifyDataSetChanged(); }
    @Override public int getCount() { return items.size(); }
    @Override public Workshop getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    private static final class Holder {
        final android.widget.ImageView image;
        final TextView name, area, rating, open, price;
        Holder(View view) {
            image = view.findViewById(R.id.row_image);
            view.findViewById(R.id.row_card).setClipToOutline(true);
            name = view.findViewById(R.id.row_name); area = view.findViewById(R.id.row_area);
            rating = view.findViewById(R.id.row_rating); open = view.findViewById(R.id.row_open);
            price = view.findViewById(R.id.row_price);
        }
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_workshop, parent, false);
            holder = new Holder(convertView);
            convertView.setTag(holder);
        } else { holder = (Holder) convertView.getTag(); }
        Context context = convertView.getContext();
        Workshop item = getItem(position);
        com.example.carworkshopbookingapp.util.WorkshopImages.bind(holder.image, item);
        holder.name.setText(item.name); holder.area.setText(item.area);
        holder.rating.setText(context.getString(R.string.rating_value, item.rating));
        boolean open = WorkshopBrowser.isOpen(item, WorkshopDisplay.dhakaMinute());
        holder.open.setText(item.openHour == item.closeHour ? R.string.hours_all_day : open ? R.string.open_now : R.string.closed_now);
        holder.open.setBackgroundResource(open ? R.drawable.status_ready : R.drawable.chip_default);
        holder.open.setTextColor(ContextCompat.getColor(context, open ? R.color.success : R.color.text_muted));
        ServiceItem requested = services.get(requestedServiceId);
        double price = requestedServiceId == null ? WorkshopBrowser.startingPrice(item, services) :
                requested == null ? Double.POSITIVE_INFINITY : requested.basePrice;
        holder.price.setText(Double.isFinite(price) ? context.getString(R.string.starting_price,
                WorkshopDisplay.money(price)) : context.getString(R.string.price_unavailable));
        motion.bind(convertView, item.id);
        return convertView;
    }
}
