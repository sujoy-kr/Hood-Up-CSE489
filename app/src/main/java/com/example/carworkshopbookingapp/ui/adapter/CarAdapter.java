package com.example.carworkshopbookingapp.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.model.Car;
import java.util.ArrayList;
import java.util.List;

public final class CarAdapter extends BaseAdapter {
    public interface Actions { void edit(Car car); void delete(Car car); }
    private final com.example.carworkshopbookingapp.util.ListMotion motion = new com.example.carworkshopbookingapp.util.ListMotion();
    private final LayoutInflater inflater;
    private final Actions actions;
    private List<Car> items = new ArrayList<>();
    public CarAdapter(Context context, Actions actions) { inflater = LayoutInflater.from(context); this.actions = actions; }
    public void submit(List<Car> cars) { items = cars; notifyDataSetChanged(); }
    @Override public int getCount() { return items.size(); }
    @Override public Car getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }
    private static final class Holder {
        final TextView title, plate;
        final View edit, delete;
        Holder(View view) {
            title = view.findViewById(R.id.car_title); plate = view.findViewById(R.id.car_plate_value);
            edit = view.findViewById(R.id.edit_car); delete = view.findViewById(R.id.delete_car);
        }
    }
    @Override public View getView(int position, View view, ViewGroup parent) {
        Holder holder;
        if (view == null) { view = inflater.inflate(R.layout.row_car, parent, false); holder = new Holder(view); view.setTag(holder); }
        else holder = (Holder) view.getTag();
        Car car = getItem(position);
        holder.title.setText(view.getContext().getString(R.string.car_row_title, car.make, car.model, car.year));
        holder.plate.setText(car.plate);
        holder.edit.setOnClickListener(v -> actions.edit(car));
        holder.delete.setOnClickListener(v -> actions.delete(car));
        motion.bind(view, car.id);
        return view;
    }
}
