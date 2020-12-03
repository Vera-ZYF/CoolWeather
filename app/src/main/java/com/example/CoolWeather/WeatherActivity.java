package com.example.CoolWeather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import com.bumptech.glide.Glide;
import com.example.CoolWeather.util.Utility;
import com.example.weather02.R;
import com.example.CoolWeather.gson.Weather;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import com.example.CoolWeather.util.HttpUtil;

import static com.example.CoolWeather.MyDBhelper.DB_NAME;
import static com.example.CoolWeather.MyDBhelper.TABLE_NAME;

import static com.example.CoolWeather.MainActivity.isfavorite;

public class WeatherActivity extends AppCompatActivity {

    public DrawerLayout drawerLayout;
    private Button navButton;
    private Button concern;
    private Button concealConcern;
//    private Button goBack;
//    private Button refresh;
    public SwipeRefreshLayout swipeRefresh;
    private ScrollView weatherLayout;
    private ImageView bingPicImg;
    private TextView provinceText;//省区
    private TextView cityText;//市区
    private TextView weatherText;//天气
    private TextView temperatureText;//温度
    private TextView humidityText;//湿度
    private TextView reportTimeText;//时间
    private String theId;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(isfavorite){
            setContentView(R.layout.favorite_weather);
        }else{
            setContentView(R.layout.activity_weather);
        }

        //初始化控件
        weatherLayout = findViewById(R.id.weather_layout);
        bingPicImg = findViewById(R.id.bing_pic_img);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        provinceText = findViewById(R.id.province_text);
        cityText = findViewById(R.id.city_text);
        weatherText = findViewById(R.id.weather_text);
        temperatureText = findViewById(R.id.temperature_text);
        humidityText = findViewById(R.id.humidity_text);
        reportTimeText = findViewById(R.id.reporttime_text);
        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);

        //goBack = findViewById(R.id.goBack);
        //refresh = findViewById(R.id.refresh);
        SharedPreferences prefs = getSharedPreferences(String.valueOf(this),MODE_PRIVATE);
        final String adcodeString = prefs.getString("weather",null);

        final String countyCode;
        final String countyName;

        //有缓存时直接获取天气信息
        if (adcodeString != null) {
            Weather weather = Utility.handleWeatherResponse(adcodeString);
            countyCode = weather.adcodeName;
            countyName = weather.cityName;
            showWeatherInfo(weather);
        } else {
            //没有缓存时到服务器查询天气信息
            countyCode = getIntent().getStringExtra("adcode");
            countyName = getIntent().getStringExtra("city");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(countyCode);
        }

        final String x = cityText.getText().toString();

        //下拉进度条监听器
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh() {
                requestWeather(countyCode);//回调方法
            }
        });

        //打开滑动菜单
        navButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                isfavorite = false;
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });


        if(isfavorite){
            concealConcern = findViewById(R.id.concealConcern);
            concealConcern.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MyDBhelper dbHelper = new MyDBhelper(WeatherActivity.this, DB_NAME, null, 1);
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete(TABLE_NAME,"city_code=?",new String[]{String.valueOf(countyCode)});
                    Toast.makeText(WeatherActivity.this, "取消关注成功！", Toast.LENGTH_LONG).show();
                }
            });
        }else{
            concern = findViewById(R.id.concern);
            concern.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MyDBhelper dbHelper = new MyDBhelper(WeatherActivity.this, DB_NAME, null, 1);
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("city_code", countyCode);
                    values.put("city_name", countyName);
                    db.insert(TABLE_NAME, null, values);
                    Toast.makeText(WeatherActivity.this, "关注成功！", Toast.LENGTH_LONG).show();
                }
            });
        }
        isfavorite = false;
    }

    public void requestWeather(final String adCode) {
        String weatherUrl = "https://restapi.amap.com/v3/weather/weatherInfo?city=" + adCode + "&key=9e9386c18e5f1094cb408725c777c9c0";
        //向服务器发送请求
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //将JSON数据转换成Weather对象
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                //切换到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null) {
                            //将天气信息缓存到SharedPreferences中，调用showWeatherInfo显示
                            SharedPreferences.Editor editor = getSharedPreferences(String.valueOf(this),MODE_PRIVATE).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "城市ID不存在，请重新输入！", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
                loadBingPic();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败！", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

   private void loadBingPic() {
        //设置天气界面背景
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    //显示Weather中的数据
     private void showWeatherInfo(Weather weather) {
        //获取天气信息
        String provinceName = weather.provinceName;
        String cityName = weather.cityName;
        String weatherName = weather.weatherName;
        String temperatureName = weather.temperatureName;
        String humidityName = weather.humidityName;
        String reportTime = weather.reportTimeName;

        provinceText.setText(provinceName);
        cityText.setText(cityName);
        weatherText.setText(weatherName);
        temperatureText.setText(temperatureName + "℃");
        humidityText.setText(humidityName + "%");
        reportTimeText.setText(reportTime);
        weatherLayout.setVisibility(View.VISIBLE);
    }
}