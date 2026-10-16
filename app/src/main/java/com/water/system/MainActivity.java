package com.water.system;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout mainLayout;
    SQLiteDatabase db;
    int price = 600;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(0, 105, 160));
        getWindow().setNavigationBarColor(Color.BLACK);
        price = getSharedPreferences("settings", MODE_PRIVATE).getInt("price", 600);
        db = openOrCreateDatabase("WaterSystem.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS subscribers (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,shop TEXT,phone TEXT,previous INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS invoices (id INTEGER PRIMARY KEY AUTOINCREMENT,subscriber_id INTEGER,old_reading INTEGER,new_reading INTEGER,units INTEGER,amount INTEGER,date TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS receipts (id INTEGER PRIMARY KEY AUTOINCREMENT,subscriber_id INTEGER,amount INTEGER,date TEXT)");
        showHome();
    }

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    TextView text(String v,int s){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(Color.rgb(30,45,55));t.setGravity(Gravity.CENTER);t.setPadding(dp(8),dp(8),dp(8),dp(8));return t;}
    Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setTextSize(16);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(0,119,170)));b.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(4),dp(5),dp(4),dp(5));b.setLayoutParams(p);return b;}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setSingleLine(true);e.setPadding(dp(12),dp(8),dp(12),dp(8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(4),dp(3),dp(4),dp(3));e.setLayoutParams(p);return e;}
    void startPage(String title){mainLayout=new LinearLayout(this);mainLayout.setOrientation(LinearLayout.VERTICAL);mainLayout.setPadding(dp(12),dp(8),dp(12),dp(12));mainLayout.setBackgroundColor(Color.rgb(245,248,250));ScrollView scroll=new ScrollView(this);scroll.addView(mainLayout);setContentView(scroll);mainLayout.addView(text(title,23));mainLayout.addView(button("الرئيسية",this::showHome));}
    void msg(String m){new AlertDialog.Builder(this).setMessage(m).setPositiveButton("موافق",null).show();}
    String today(){return new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new java.util.Date());}
    int readInt(EditText e){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception ex){return 0;}}
    int invoiceBalance(int subId){
        int totalInvoices=0, totalReceipts=0, prev=0;
        Cursor c1=db.rawQuery("SELECT previous FROM subscribers WHERE id=?",new String[]{String.valueOf(subId)});
        if(c1.moveToFirst()) prev=c1.getInt(0); c1.close();
        Cursor c2=db.rawQuery("SELECT SUM(amount) FROM invoices WHERE subscriber_id=?",new String[]{String.valueOf(subId)});
        if(c2.moveToFirst()) totalInvoices=c2.getInt(0); c2.close();
        Cursor c3=db.rawQuery("SELECT SUM(amount) FROM receipts WHERE subscriber_id=?",new String[]{String.valueOf(subId)});
        if(c3.moveToFirst()) totalReceipts=c3.getInt(0); c3.close();
        return prev + totalInvoices - totalReceipts;
    }

    void showHome(){
        startPage("نظام المياه");
        mainLayout.addView(text("إدارة المشتركين والفواتير",16));
        mainLayout.addView(button("المشتركون",this::showSubscribers));
        mainLayout.addView(button("إصدار الفواتير وقراءة العدادات",this::showInvoices));
        mainLayout.addView(button("سند قبض",this::showReceipts));
        mainLayout.addView(button("كشف الحساب",this::showStatement));
        mainLayout.addView(button("آخر السجلات",this::showRecords));
        mainLayout.addView(button("الإعدادات",this::showSettings));
        mainLayout.addView(text("سعر الوحدة: "+price+" ريال",16));
    }

    void showSubscribers(){
        startPage("إدارة المشتركين");
        EditText name=input("اسم المشترك");EditText shop=input("رقم المحل");EditText phone=input("رقم الهاتف");EditText arrears=input("المتأخرات السابقة");
        mainLayout.addView(name);mainLayout.addView(shop);mainLayout.addView(phone);mainLayout.addView(arrears);
        mainLayout.addView(button("إضافة مشترك",()->{
            String n=name.getText().toString().trim();String s=shop.getText().toString().trim();
            if(n.isEmpty()||s.isEmpty()){msg("أدخل اسم المشترك ورقم المحل");return;}
            int oldArrears=readInt(arrears);
            ContentValues cv=new ContentValues();cv.put("name",n);cv.put("shop",s);cv.put("phone",phone.getText().toString().trim());cv.put("previous",oldArrears);
            long res=db.insert("subscribers",null,cv);if(res!=-1){msg("تمت الإضافة");showSubscribers();}else msg("فشل");
        }));
        EditText search=input("بحث بالاسم أو رقم المحل");mainLayout.addView(search);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);mainLayout.addView(list);
        Runnable refresh=()->{
            list.removeAllViews();String q=search.getText().toString().trim();
            Cursor c=db.rawQuery("SELECT id,name,shop,phone,previous FROM subscribers WHERE name LIKE ? OR shop LIKE ? ORDER BY id DESC",new String[]{"%"+q+"%","%"+q+"%"});
            while(c.moveToNext()){
                final int id=c.getInt(0);final String n=c.getString(1);final String s=c.getString(2);final String p=c.getString(3);final int a=c.getInt(4);
                list.addView(text("المشترك: "+n+"\nالمحل: "+s+"\nالهاتف: "+p+"\nالسابق: "+a+" ريال\nالمتبقي: "+invoiceBalance(id)+" ريال",16));
                list.addView(button("تعديل",()->editSubscriber(id,n,s,p,a)));
                list.addView(button("حذف",()->confirmDelete(id,n)));
            }c.close();
        };
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){refresh.run();}public void afterTextChanged(Editable e){}});
        refresh.run();
    }

    void editSubscriber(int id,String oldName,String oldShop,String oldPhone,int oldArrears){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        EditText name=input("الاسم");EditText shop=input("المحل");EditText phone=input("الهاتف");EditText arrears=input("السابق");
        name.setText(oldName);shop.setText(oldShop);phone.setText(oldPhone);arrears.setText(String.valueOf(oldArrears));
        box.addView(name);box.addView(shop);box.addView(phone);box.addView(arrears);
        new AlertDialog.Builder(this).setTitle("تعديل").setView(box).setPositiveButton("حفظ",(d,w)->{
            ContentValues cv=new ContentValues();cv.put("name",name.getText().toString().trim());cv.put("shop",shop.getText().toString().trim());cv.put("phone",phone.getText().toString().trim());cv.put("previous",readInt(arrears));
            db.update("subscribers",cv,"id=?",new String[]{String.valueOf(id)});showSubscribers();
        }).setNegativeButton("إلغاء",null).show();
    }

    void confirmDelete(int id,String name){
        new AlertDialog.Builder(this).setTitle("حذف المشترك").setMessage("هل تريد حذف "+name+" ؟").setPositiveButton("حذف",(d,w)->{db.delete("subscribers","id=?",new String[]{String.valueOf(id)});showSubscribers();}).setNegativeButton("إلغاء",null).show();
    }

    void showInvoices(){
        startPage("إصدار الفواتير");
        EditText shop=input("رقم المحل"); mainLayout.addView(shop);
        EditText oldR=input("القراءة السابقة"); EditText newR=input("القراءة الحالية");
        mainLayout.addView(oldR); mainLayout.addView(newR);
        mainLayout.addView(button("إصدار الفاتورة",()->{
            try{
                String s=shop.getText().toString().trim();
                Cursor c=db.rawQuery("SELECT id FROM subscribers WHERE shop=?",new String[]{s});
                if(!c.moveToFirst()){msg("المشترك غير موجود");c.close();return;}
                int subId=c.getInt(0);c.close();
                int oldV=readInt(oldR); int newV=readInt(newR);
                if(newV<oldV){msg("القراءة الحالية أقل من السابقة");return;}
                int units=newV-oldV; int amount=units*price;
                ContentValues cv=new ContentValues();cv.put("subscriber_id",subId);cv.put("old_reading",oldV);cv.put("new_reading",newV);cv.put("units",units);cv.put("amount",amount);cv.put("date",today());
                db.insert("invoices",null,cv); msg("تم إصدار الفاتورة: "+amount+" ريال ("+units+" وحدة)");
            }catch(Exception e){msg(e.getMessage());}
        }));
    }

    void showReceipts(){
        startPage("سند قبض");
        EditText shop=input("رقم المحل"); EditText amount=input("المبلغ المدفوع");
        mainLayout.addView(shop); mainLayout.addView(amount);
        mainLayout.addView(button("تسجيل الدفع",()->{
            Cursor c=db.rawQuery("SELECT id FROM subscribers WHERE shop=?",new String[]{shop.getText().toString().trim()});
            if(!c.moveToFirst()){msg("المشترك غير موجود");c.close();return;}
            int subId=c.getInt(0);c.close();
            int amt=readInt(amount);
            ContentValues cv=new ContentValues();cv.put("subscriber_id",subId);cv.put("amount",amt);cv.put("date",today());
            db.insert("receipts",null,cv); msg("تم تسجيل القبض");
        }));
    }

    void showStatement(){
        startPage("كشف الحساب");
        EditText shop=input("رقم المحل"); mainLayout.addView(shop);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);mainLayout.addView(list);
        mainLayout.addView(button("عرض",()->{
            list.removeAllViews();
            Cursor c=db.rawQuery("SELECT id FROM subscribers WHERE shop=?",new String[]{shop.getText().toString().trim()});
            if(!c.moveToFirst()){list.addView(text("غير موجود",16));c.close();return;}
            int subId=c.getInt(0);c.close();
            list.addView(text("المتبقي الكلي: "+invoiceBalance(subId)+" ريال",18));
            Cursor inv=db.rawQuery("SELECT old_reading,new_reading,units,amount,date FROM invoices WHERE subscriber_id=? ORDER BY id DESC",new String[]{String.valueOf(subId)});
            while(inv.moveToNext()) list.addView(text("فاتورة: "+inv.getInt(2)+" وحدة = "+inv.getInt(3)+" ريال بتاريخ "+inv.getString(4),14));
            inv.close();
            Cursor rec=db.rawQuery("SELECT amount,date FROM receipts WHERE subscriber_id=? ORDER BY id DESC",new String[]{String.valueOf(subId)});
while(rec.moveToNext()) list.addView(textLeft("قبض: "+rec.getInt(0)+" ريال - "+rec.getString(1),14));
            rec.close();
        }));
    }

    void showRecords(){
        startPage("آخر السجلات");
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);mainLayout.addView(list);
        Cursor c=db.rawQuery("SELECT i.units,i.amount,i.date,s.name,s.shop FROM invoices i JOIN subscribers s ON i.subscriber_id=s.id ORDER BY i.id DESC LIMIT 50",null);
        while(c.moveToNext()){
            int units=c.getInt(0);int amount=c.getInt(1);String date=c.getString(2);String name=c.getString(3);String shop=c.getString(4);
            list.addView(textLeft(name+" - محل "+shop+": "+amount+" ريال ("+units+" وحدة) - "+date,14));
        }
        c.close();
    }

    void showSettings(){
        startPage("الإعدادات");
        EditText p=input("سعر وحدة الماء"); p.setText(String.valueOf(price)); mainLayout.addView(p);
        mainLayout.addView(button("حفظ السعر",()->{
            price=readInt(p);
            getSharedPreferences("settings",MODE_PRIVATE).edit().putInt("price",price).apply();
            msg("تم الحفظ");
        }));
    }
}