package com.example.CoolWeather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.weather02.R;

public class MainActivity extends AppCompatActivity {
    public static boolean isfavorite = false;
    private Button searchButton;
    private EditText chengShi;
    private Button myfavorites;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取要查询的城市ID
        chengShi = findViewById(R.id.chengshi_text);
        searchButton = findViewById(R.id.search_button);
        myfavorites = findViewById(R.id.myfavorites);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchCountyCode = String.valueOf(chengShi.getText());
                if(searchCountyCode.length() != 6){
                    Toast.makeText(MainActivity.this,"城市ID长度为6位!",Toast.LENGTH_SHORT).show();
                }else{
                    Intent intent = new Intent(MainActivity.this,WeatherActivity.class);
                    intent.putExtra("adcode",searchCountyCode);
                    startActivity(intent);
                }
            }
        });
        myfavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,MyConcernList.class);
                startActivity(intent);
            }
        });
        SharedPreferences pres = getSharedPreferences(String.valueOf(this),MODE_PRIVATE);
        if (pres.getString("weather",null)!= null){
            Intent intent = new Intent(this,WeatherActivity.class);
            startActivity(intent);
            //应该可以注释掉finish？
            //finish();
        }
        isfavorite = false;
    }
}