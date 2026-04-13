package com.example.sanbotapp.ajustes;
import android.os.Bundle;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class AjustesActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        setupTopBackBanner("Ajustes del Sistema");
    }
}
