package com.water.system;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.database.*;
import android.content.*;
import java.util.Locale;

public class MainActivity extends Activity {
    LinearLayout mainLayout;
    android.database.sqlite.SQLiteDatabase db;
    int price = 600;
    int subscribersCount = 0;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        price = getSharedPreferences("settings", MODE_PRIVATE).getInt("price", 600);
        db = openOrCreateDatabase("WaterSystem.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS subscribers (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,shop TEXT,phone TEXT,previous INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS invoices (id INTEGER PRIMARY KEY AUTOINCREMENT,subscriber_id INTEGER,old_reading INTEGER,new_reading INTEGER,units INTEGER,amount INTEGER,date TEXT,collector_id INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS receipts (id INTEGER PRIMARY KEY AUTOINCREMENT,subscriber_id INTEGER,amount INTEGER,date TEXT,collector_id INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS collectors (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,username TEXT,password TEXT,perm_receipt INTEGER DEFAULT 1,perm_print INTEGER DEFAULT 1,perm_show INTEGER DEFAULT 1,perm_invoice INTEGER DEFAULT 0,is_open INTEGER DEFAULT 0)");
        showHome();
    }

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    void msg(String m){new AlertDialog.Builder(this).setMessage(m).setPositiveButton("موافق",null).show();}
    String today(){return new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new java.util.Date());}
    int readInt(EditText e){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception ex){return 0;}}

    GradientDrawable cardBg(boolean selected, boolean greenBorder){
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.WHITE);
        d.setCornerRadius(dp(14));
        if(greenBorder) d.setStroke(dp(2), Color.rgb(76,175,80));
        else if(selected) d.setStroke(dp(2), Color.rgb(33,150,243));
        else d.setStroke(dp(1), Color.rgb(230,230,230));
        return d;
    }

    LinearLayout card(String icon, String title, String sub, boolean selected, boolean greenBorder, Runnable action){
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setBackground(cardBg(selected, greenBorder));
        c.setPadding(dp(8),dp(14),dp(8),dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,-2);
        lp.weight=1; lp.setMargins(dp(5),dp(5),dp(5),dp(5));
        c.setLayoutParams(lp);
        c.setElevation(dp(2));
        TextView ic = new TextView(this); ic.setText(icon); ic.setTextSize(28); ic.setGravity(Gravity.CENTER);
        TextView t = new TextView(this); t.setText(title); t.setTextSize(13); t.setTextColor(Color.rgb(30,30,30)); t.setGravity(Gravity.CENTER); t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0,dp(4),0,0);
        c.addView(ic); c.addView(t);
        if(sub!=null){TextView s = new TextView(this); s.setText(sub); s.setTextSize(11); s.setTextColor(Color.GRAY); s.setGravity(Gravity.CENTER); c.addView(s);}
        c.setOnClickListener(v->action.run());
        return c;
    }

    void startPage(String headerTitle){
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.rgb(235,242,255));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setBackgroundColor(Color.rgb(125,200,255));
        top.setPadding(dp(12),dp(14),dp(12),dp(14));
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = new TextView(this); icon.setText("💧"); icon.setTextSize(28);
        TextView ht = new TextView(this); ht.setText(headerTitle); ht.setTextSize(16); ht.setTextColor(Color.WHITE); ht.setTypeface(null, android.graphics.Typeface.BOLD);
        ht.setPadding(dp(8),0,0,0); ht.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1,-2); hp.weight=1;
        ht.setLayoutParams(hp);
        TextView set = new TextView(this); set.setText("⚙️"); set.setTextSize(20);
        set.setOnClickListener(v->showSettings());
        top.addView(set); top.addView(ht); top.addView(icon);
        mainLayout.addView(top);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8),dp(8),dp(8),dp(8));
        scroll.addView(content);
        mainLayout.addView(scroll);
        setContentView(mainLayout);
        mainLayout = content;
    }

    void showHome(){
        Cursor cc = db.rawQuery("SELECT COUNT(*) FROM subscribers",null);
        if(cc.moveToFirst()) subscribersCount = cc.getInt(0);
        cc.close();
        startPage("نظام المياه 5.19 - صناديق\nالمحصلين والنسخ الاحتياطي");
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(grid);
        LinearLayout r1 = new LinearLayout(this); r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.addView(card("💰","سند صرف",null,false,false, this::showReceipts));
        r1.addView(card("💰","سند قبض",null,false,false, this::showReceipts));
        grid.addView(r1);
        LinearLayout r2 = new LinearLayout(this); r2.setOrientation(LinearLayout.HORIZONTAL);
        r2.addView(card("🧾","إصدار فواتير",null,false,false, this::showInvoices));
        r2.addView(card("👥","خانة المشتركين",subscribersCount+" مشترك",true,false, this::showSubscribers));
        grid.addView(r2);
        LinearLayout r3 = new LinearLayout(this); r3.setOrientation(LinearLayout.HORIZONTAL);
        r3.addView(card("☁️⬆️","مزامنة البيانات",null,false,false, ()->msg("قريباً")));
        r3.addView(card("☁️⬇️","تحميل البيانات",null,false,false, ()->msg("تحميل")));
        grid.addView(r3);
        LinearLayout r4 = new LinearLayout(this); r4.setOrientation(LinearLayout.HORIZONTAL);
        r4.addView(card("📊","كشف حساب",null,false,false, this::showStatement));
        r4.addView(card("⚙️","الإعدادات",null,false,false, this::showSettings));
        grid.addView(r4);
        LinearLayout r5 = new LinearLayout(this); r5.setOrientation(LinearLayout.HORIZONTAL);
        r5.addView(card("🗂️","آخر السجلات",null,false,false, this::showRecords));
        r5.addView(card("➖","تسوية",null,false,false, ()->msg("تسوية")));
        grid.addView(r5);
        LinearLayout r6 = new LinearLayout(this); r6.setOrientation(LinearLayout.HORIZONTAL);
        r6.addView(card("✉️","رسائل جماعية",null,false,false, ()->msg("واتساب جماعي")));
        r6.addView(card("📈","تقارير الحسابات",null,false,false, this::showReports));
        grid.addView(r6);
        LinearLayout r7 = new LinearLayout(this); r7.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout big = card("🔐","إدارة صناديق المحصلين",null,false,true, this::showCollectors);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1,-2);
        bp.setMargins(dp(5),dp(8),dp(120),dp(5));
        big.setLayoutParams(bp);
        r7.addView(big);
        grid.addView(r7);
    }

    TextView txt(String s,int sz){TextView t=new TextView(this);t.setText(s);t.setTextSize(sz);t.setPadding(dp(8),dp(6),dp(8),dp(6));return t;}
    EditText inp(String h){EditText e=new EditText(this);e.setHint(h);e.setBackground(cardBg(false,false));e.setPadding(dp(12),dp(10),dp(12),dp(10));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(4),dp(6),dp(4),dp(6));e.setLayoutParams(p);return e;}
    Button btn(String s,Runnable r,int color){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));b.setOnClickListener(v->r.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(4),dp(8),dp(4),dp(8));b.setLayoutParams(p);return b;}

    void showCollectors(){
        startPage("🔐 إدارة صناديق المحصّلين");
        mainLayout.addView(txt("➕ إنشاء صندوق محصّل",18));
        EditText name = inp("اسم المحصل");
        EditText username = inp("اسم المستخدم");
        EditText pass = inp("كلمة سر تجريبية");
        mainLayout.addView(name); mainLayout.addView(username); mainLayout.addView(pass);
        CheckBox cb1 = new CheckBox(this); cb1.setText("السماح بتسجيل سند قبض"); cb1.setChecked(true);
        CheckBox cb2 = new CheckBox(this); cb2.setText("السماح بطباعة السند"); cb2.setChecked(true);
        CheckBox cb3 = new CheckBox(this); cb3.setText("السماح بعرض متأخرات المشترك"); cb3.setChecked(true);
        CheckBox cb4 = new CheckBox(this); cb4.setText("السماح بإصدار الفواتير"); cb4.setChecked(false);
        mainLayout.addView(cb1); mainLayout.addView(cb2); mainLayout.addView(cb3); mainLayout.addView(cb4);
        mainLayout.addView(btn("حفظ صندوق المحصّل",()->{
            ContentValues cv=new ContentValues();
            cv.put("name",name.getText().toString().trim());
            cv.put("username",username.getText().toString().trim());
            cv.put("password",pass.getText().toString().trim());
            cv.put("perm_receipt", cb1.isChecked()?1:0);
            db.insert("collectors",null,cv);
            msg("تم حفظ الصندوق"); showCollectors();
        }, Color.rgb(22,163,74)));
        mainLayout.addView(txt("🔑 فتح صندوق محصّل",18));
        EditText loginUser = inp("اسم المستخدم");
        EditText loginPass = inp("كلمة السر");
        mainLayout.addView(loginUser); mainLayout.addView(loginPass);
        mainLayout.addView(btn("فتح الصندوق",()->{
            Cursor c=db.rawQuery("SELECT id,name FROM collectors WHERE username=? AND password=?",new String[]{loginUser.getText().toString().trim(),loginPass.getText().toString().trim()});
            if(c.moveToFirst()){
                db.execSQL("UPDATE collectors SET is_open=0");
                db.execSQL("UPDATE collectors SET is_open=1 WHERE id="+c.getInt(0));
                msg("تم فتح: "+c.getString(1)); showHome();
            }else msg("بيانات خاطئة");
            c.close();
        }, Color.rgb(37,99,235)));
        mainLayout.addView(btn("رجوع",this::showHome, Color.GRAY));
    }

    void showSubscribers(){ /* نفس السابق */ startPage("خانة المشتركين"); EditText name=inp("اسم المشترك");EditText shop=inp("رقم المحل"); mainLayout.addView(name);mainLayout.addView(shop); mainLayout.addView(btn("إضافة",()->{ContentValues cv=new ContentValues();cv.put("name",name.getText().toString());cv.put("shop",shop.getText().toString());db.insert("subscribers",null,cv);msg("تمت الإضافة");}, Color.rgb(33,150,243))); mainLayout.addView(btn("رجوع",this::showHome, Color.GRAY)); }
    void showInvoices(){ startPage("إصدار فواتير"); EditText shop=inp("رقم المحل");EditText oldR=inp("القراءة السابقة");EditText newR=inp("القراءة الحالية"); mainLayout.addView(shop);mainLayout.addView(oldR);mainLayout.addView(newR); mainLayout.addView(btn("إصدار الفاتورة",()->{Cursor c=db.rawQuery("SELECT id FROM subscribers WHERE shop=?",new String[]{shop.getText().toString().trim()});if(!c.moveToFirst()){msg("غير موجود");c.close();return;}int id=c.getInt(0);c.close();int o=readInt(oldR),n=readInt(newR);if(n<o){msg("الحالية أقل");return;}int units=n-o,amount=units*price;ContentValues cv=new ContentValues();cv.put("subscriber_id",id);cv.put("old_reading",o);cv.put("new_reading",n);cv.put("units",units);cv.put("amount",amount);cv.put("date",today());db.insert("invoices",null,cv);msg("تم إصدار الفاتورة: "+amount+" ريال");}, Color.rgb(76,175,80))); mainLayout.addView(btn("رجوع",this::showHome, Color.GRAY)); }
    void showReceipts(){ startPage("سند قبض"); EditText shop=inp("رقم المحل");EditText amount=inp("المبلغ"); mainLayout.addView(shop);mainLayout.addView(amount); mainLayout.addView(btn("حفظ",()->{Cursor c=db.rawQuery("SELECT id FROM subscribers WHERE shop=?",new String[]{shop.getText().toString().trim()});if(!c.moveToFirst()){msg("غير موجود");c.close();return;}int id=c.getInt(0);c.close();ContentValues cv=new ContentValues();cv.put("subscriber_id",id);cv.put("amount",readInt(amount));cv.put("date",today());db.insert("receipts",null,cv);msg("تم حفظ القبض");}, Color.rgb(76,175,80))); mainLayout.addView(btn("رجوع",this::showHome, Color.GRAY)); }
    void showStatement(){ startPage("كشف حساب"); EditText shop=inp("رقم المحل");mainLayout.addView(shop); LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);mainLayout.addView(list); mainLayout.addView(btn("عرض الكشف",()->{list.removeAllViews();Cursor c=db.rawQuery("SELECT id,name,previous FROM subscribers WHERE shop=?",new String[]{shop.getText().toString().trim()});if(!c.moveToFirst()){list.addView(txt("غير موجود",16));c.close();return;}int sid=c.getInt(0);String name=c.getString(1);int prev=c.getInt(2);c.close();int totalInv=0,totalRec=0;Cursor ci=db.rawQuery("SELECT SUM(amount) FROM invoices WHERE subscriber_id=?",new String[]{String.valueOf(sid)}); if(ci.moveToFirst()) totalInv=ci.getInt(0); ci.close();Cursor cr=db.rawQuery("SELECT SUM(amount) FROM receipts WHERE subscriber_id=?",new String[]{String.valueOf(sid)}); if(cr.moveToFirst()) totalRec=cr.getInt(0); cr.close();int balance=prev+totalInv-totalRec;list.addView(txt(name+" - المتبقي: "+balance+" ريال",18));Cursor inv=db.rawQuery("SELECT amount,date FROM invoices WHERE subscriber_id=? ORDER BY id DESC",new String[]{String.valueOf(sid)});while(inv.moveToNext()) list.addView(txt("فاتورة: "+inv.getInt(0)+" ريال - "+inv.getString(1),13));inv.close();Cursor rec=db.rawQuery("SELECT amount,date FROM receipts WHERE subscriber_id=? ORDER BY id DESC",new String[]{String.valueOf(sid)});while(rec.moveToNext()) list.addView(txt("قبض: "+rec.getInt(0)+" ريال - "+rec.getString(1),13));rec.close();}, Color.rgb(33,150,243))); mainLayout.addView(btn("رجوع",this::showHome, Color.GRAY)); }
    void showRecords(){ startPage("آخر السجلات"); LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);mainLayout.addView(list); Cursor c=db.rawQuery("SELECT i.amount,i.date,s.name,s.shop FROM invoices i JOIN subscribers s ON i.subscriber_id=s.id ORDER BY i.id DESC LIMIT 100",null);while(c.moveToNext()) list.addView(txt(c.getString(2)+" - محل "+c.getString(3)+": "+c.getInt(0)+" ريال - "+c.getString(1),13));c.close(); mainLayout.addView(btn("رجوع",this::showHome, Color.GRAY)); }
    void showReports(){ startPage("تقارير الحسابات"); int totalSub=0,totalInv=0,totalRec=0; Cursor a=db.rawQuery("SELECT COUNT(*) FROM subscribers",null); if(a.moveToFirst()) totalSub=a.getInt(0); a.close(); Cursor b=db.rawQuery("SELECT SUM(amount) FROM invoices",null); if(b.moveToFirst()) totalInv=b.getInt(0); b.close(); Cursor c=db.rawQuery("SELECT SUM(amount) FROM receipts",null); if(c.moveToFirst()) totalRec=c.getInt(0); c.close(); mainLayout.addView(txt("إجمالي المشتركين: "+totalSub,16)); mainLayout.addView(txt("إجمالي الفواتير: "+totalInv+" ريال",16)); mainLayout.addView(txt("إجمالي المقبوضات: "+totalRec+" ريال",16)); mainLayout.addView(txt("المتبقي: "+(totalInv-totalRec)+" ريال",18)); mainLayout.addView(btn("رجوع",this::showHome, Color.GRAY)); }
    void showSettings(){ startPage("⚙️ الإعدادات"); mainLayout.addView(txt("سعر الوحدة",14)); EditText priceInput = inp(""); priceInput.setText(String.valueOf(price)); mainLayout.addView(priceInput); mainLayout.addView(btn("حفظ",()->{price=readInt(priceInput);getSharedPreferences("settings",MODE_PRIVATE).edit().putInt("price",price).apply();msg("تم حفظ السعر");}, Color.rgb(76,175,80))); mainLayout.addView(btn("رجوع",this::showHome, Color.GRAY)); }
}