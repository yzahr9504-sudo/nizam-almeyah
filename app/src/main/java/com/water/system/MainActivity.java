package com.example.myapplication;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.Gravity;
import android.graphics.Color;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("التطبيق يعمل الآن ✅\n\nإذا ظهرت هذه الرسالة معناها المشكلة من ملف index.html\nارجع للكود الكامل");
        tv.setTextSize(22);
        tv.setTextColor(Color.BLACK);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(50,50,50,50);
        setContentView(tv);
    }
}