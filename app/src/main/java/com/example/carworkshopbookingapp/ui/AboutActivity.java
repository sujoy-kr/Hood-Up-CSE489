package com.example.carworkshopbookingapp.ui;
import android.os.Bundle;
import com.example.carworkshopbookingapp.R;
public final class AboutActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {super.onCreate(state);screen(R.layout.activity_about);findViewById(R.id.back).setOnClickListener(v->finish());}
}
