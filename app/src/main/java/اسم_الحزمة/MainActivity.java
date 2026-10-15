package com.example.myapplication;

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

        price = getSharedPreferences("settings", MODE_PRIVATE)
                .getInt("price", 600);

        db = openOrCreateDatabase(
                "WaterSystem.db", MODE_PRIVATE, null);

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS subscribers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "shop TEXT," +
                "phone TEXT," +
                "previous INTEGER DEFAULT 0)");

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS invoices (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subscriber_id INTEGER," +
                "old_reading INTEGER," +
                "new_reading INTEGER," +
                "units INTEGER," +
                "amount INTEGER," +
                "date TEXT)");

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS receipts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subscriber_id INTEGER," +
                "amount INTEGER," +
                "date TEXT)");

        showHome();
    }

    int dp(int value) {
        return (int) (
                value * getResources()
                .getDisplayMetrics().density);
    }

    TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(30, 45, 55));
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(8), dp(8), dp(8));
        return t;
    }

    Button button(String title, Runnable action) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);

        b.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.rgb(0, 119, 170)));

        b.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(dp(4), dp(5), dp(4), dp(5));
        b.setLayoutParams(p);

        return b;
    }

    EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(dp(4), dp(3), dp(4), dp(3));
        e.setLayoutParams(p);

        return e;
    }

    void startPage(String title) {
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(
                LinearLayout.VERTICAL);

        mainLayout.setPadding(
                dp(12), dp(8), dp(12), dp(12));

        mainLayout.setBackgroundColor(
                Color.rgb(245, 248, 250));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(mainLayout);

        setContentView(scroll);

        mainLayout.addView(text(title, 23));

        mainLayout.addView(
                button("الرئيسية", this::showHome));
    }

    void msg(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("موافق", null)
                .show();
    }

    String today() {
        return new java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault())
                .format(new java.util.Date());
    }

    int readInt(EditText e) {
        return Integer.parseInt(
                e.getText().toString().trim());
    }

    // =========================
    // الرئيسية
    // =========================

    void showHome() {
        startPage("نظام المياه");

        mainLayout.addView(
                text("إدارة المشتركين والفواتير", 16));

        mainLayout.addView(
                button("المشتركون",
                        this::showSubscribers));

        mainLayout.addView(
                button("إصدار الفواتير وقراءة العدادات",
                        this::showInvoices));

        mainLayout.addView(
                button("سند قبض",
                        this::showReceipts));

        mainLayout.addView(
                button("كشف الحساب",
                        this::showStatement));

        mainLayout.addView(
                button("آخر السجلات",
                        this::showRecords));

        mainLayout.addView(
                button("الإعدادات",
                        this::showSettings));

        mainLayout.addView(
                text("سعر الوحدة: " + price + " ريال", 16));
    }

    // =========================
    // إدارة المشتركين
    // =========================

    void showSubscribers() {
        startPage("إدارة المشتركين");

        EditText name = input("اسم المشترك");
        EditText shop = input("رقم المحل");
        EditText phone = input("رقم الهاتف");

        EditText arrears = input(
                "المتأخرات السابقة بالريال");

        mainLayout.addView(name);
        mainLayout.addView(shop);
        mainLayout.addView(phone);
        mainLayout.addView(arrears);

        mainLayout.addView(button("إضافة مشترك", () -> {
            String n = name.getText().toString().trim();
            String s = shop.getText().toString().trim();
            String p = phone.getText().toString().trim();

            if (n.isEmpty() || s.isEmpty()) {
                msg("أدخل اسم المشترك ورقم المحل");
                return;
            }

            int oldArrears;

            try {
                String value = arrears.getText()
                        .toString().trim();

                oldArrears = value.isEmpty()
                        ? 0 : Integer.parseInt(value);

                if (oldArrears < 0) {
                    msg("المتأخرات لا يمكن أن تكون سالبة");
                    return;
                }

            } catch (Exception e) {
                msg("أدخل قيمة صحيحة للمتأخرات");
                return;
            }

            ContentValues cv = new ContentValues();
            cv.put("name", n);
            cv.put("shop", s);
            cv.put("phone", p);
            cv.put("previous", oldArrears);

            long result = db.insert(
                    "subscribers", null, cv);

            if (result != -1) {
                msg("تمت إضافة المشترك بنجاح");
                showSubscribers();
            } else {
                msg("تعذرت إضافة المشترك");
            }
        }));

        EditText search = input(
                "بحث بالاسم أو رقم المحل");

        mainLayout.addView(search);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(list);

        Runnable refresh = () -> {
            list.removeAllViews();

            String q = search.getText()
                    .toString().trim();

            Cursor c = db.rawQuery(
                    "SELECT id,name,shop,phone,previous " +
                    "FROM subscribers " +
                    "WHERE name LIKE ? OR shop LIKE ? " +
                    "ORDER BY id DESC",
                    new String[]{"%" + q + "%",
                                 "%" + q + "%"});

            while (c.moveToNext()) {
                final int id = c.getInt(0);
                final String n = c.getString(1);
                final String s = c.getString(2);
                final String p = c.getString(3);
                final int a = c.getInt(4);

                list.addView(text(
                        "المشترك: " + n +
                        "\nرقم المحل: " + s +
                        "\nالهاتف: " + p +
                        "\nالمتأخرات السابقة: " +
                        a + " ريال" +
                        "\nإجمالي المتبقي: " +
                        invoiceBalance(id) + " ريال",
                        16));

                list.addView(button(
                        "تعديل بيانات المشترك",
                        () -> editSubscriber(
                                id, n, s, p, a)));

                list.addView(button(
                        "حذف المشترك",
                        () -> confirmDelete(id, n)));
            }

            c.close();
        };

        search.addTextChangedListener(
                new TextWatcher() {
            public void beforeTextChanged(
                    CharSequence s, int st,
                    int count, int after) {}

            public void onTextChanged(
                    CharSequence s, int st,
                    int before, int count) {
                refresh.run();
            }

            public void afterTextChanged(Editable e) {}
        });

        refresh.run();
    }

    void editSubscriber(
            int id, String oldName,
            String oldShop, String oldPhone,
            int oldArrears) {

        LinearLayout box = new LinearLayout(this);
        box.setPadding(
                dp(16), dp(8), dp(16), dp(8));

        box.setOrientation(LinearLayout.VERTICAL);

        EditText name = input("اسم المشترك");
        EditText shop = input("رقم المحل");
        EditText phone = input("رقم الهاتف");
        EditText arrears = input(
                "المتأخرات السابقة بالريال");

        name.setText(oldName);
        shop.setText(oldShop);
        phone.setText(oldPhone);
        arrears.setText(String.valueOf(oldArrears));

        box.addView(name);
        box.addView(shop);
        box.addView(phone);
        box.addView(arrears);

        new AlertDialog.Builder(this)
                .setTitle("تعديل بيانات المشترك")
                .setView(box)
                .setPositiveButton("حفظ", (d, w) -> {
                    try {
                        String n = name.getText()
                                .toString().trim();

                        String s = shop.getText()
                                .toString().trim();

                        int a = readInt(arrears);

                        if (n.isEmpty() ||
                                s.isEmpty() || a < 0) {
                            msg("تحقق من البيانات المدخلة");
                            return;
                        }

                        ContentValues cv =
                                new ContentValues();

                        cv.put("name", n);
                        cv.put("shop", s);
                        cv.put("phone", phone.getText()
                                .toString().trim());
                        cv.put("previous", a);

                        db.update(
                                "subscribers", cv, "id=?",
                                new String[]{
                                    String.valueOf(id)
                                });

                        msg("تم تحديث البيانات");
                        showSubscribers();

                    } catch (Exception e) {
                        msg("أدخل المتأخرات بشكل صحيح");
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    void confirmDelete(int id, String name) {
        new AlertDialog.Builder(this)
                .setTitle("حذف المشترك")
                .setMessage(
                        "هل تريد حذف المشترك " + name +
                        "؟ سيتم حذف فواتيره وسندات قبضه أيضاً.")
                .setPositiveButton("حذف", (a, b) -> {
                    db.beginTransaction();

                    try {
                        String sid = String.valueOf(id);

                        db.delete("invoices",
                                "subscriber_id=?",
                                new String[]{sid});

                        db.delete("receipts",
                                "subscriber_id=?",
                                new String[]{sid});

                        db.delete("subscribers",
                                "id=?",
                                new String[]{sid});

                        db.setTransactionSuccessful();

                    } finally {
                        db.endTransaction();
                    }

                    msg("تم حذف المشترك وسجلاته");
                    showSubscribers();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    // =========================
    // إصدار الفواتير
    // =========================

    void showInvoices() {
        startPage("إصدار الفواتير");

        EditText search = input(
                "ابحث بالاسم أو رقم المحل");

        mainLayout.addView(search);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(list);

        Runnable refresh = () -> {
            list.removeAllViews();

            String q = search.getText()
                    .toString().trim();

            Cursor c = db.rawQuery(
                    "SELECT id,name,shop FROM subscribers " +
                    "WHERE name LIKE ? OR shop LIKE ? " +
                    "ORDER BY name",
                    new String[]{"%" + q + "%",
                                 "%" + q + "%"});

            while (c.moveToNext()) {
                final int id = c.getInt(0);
                final String name = c.getString(1);
                final String shop = c.getString(2);

                list.addView(button(
                        name + " - محل رقم " + shop,
                        () -> issueInvoice(
                                id, name, shop)));
            }

            c.close();
        };

        search.addTextChangedListener(
                new TextWatcher() {
            public void beforeTextChanged(
                    CharSequence s, int st,
                    int count, int after) {}

            public void onTextChanged(
                    CharSequence s, int st,
                    int before, int count) {
                refresh.run();
            }

            public void afterTextChanged(Editable e) {}
        });

        refresh.run();
    }

    void issueInvoice(
            int subscriberId,
            String name, String shop) {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(
                dp(16), dp(8), dp(16), dp(8));

        EditText oldReading =
                input("القراءة السابقة للعداد");

        EditText newReading =
                input("القراءة الحالية للعداد");

        box.addView(oldReading);
        box.addView(newReading);

        int arrears = subscriberArrears(subscriberId);

        int balance = invoiceBalance(subscriberId);

        TextView info = text(
                "المتأخرات السابقة: " +
                arrears + " ريال" +
                "\nإجمالي المستحقات الحالية: " +
                balance + " ريال" +
                "\nسعر الوحدة: " + price + " ريال",
                16);

        box.addView(info);

        new AlertDialog.Builder(this)
                .setTitle("إصدار فاتورة: " + name)
                .setView(box)
                .setPositiveButton(
                        "إصدار الفاتورة", (d, w) -> {
                    try {
                        int oldValue = readInt(oldReading);
                        int newValue = readInt(newReading);

                        if (oldValue < 0 ||
                                newValue < oldValue) {
                            msg("القراءة الحالية يجب أن تكون " +
                                    "أكبر من أو تساوي القراءة السابقة");
                            return;
                        }

                        int units = newValue - oldValue;

                        long amountLong =
                                (long) units * price;

                        if (amountLong > Integer.MAX_VALUE) {
                            msg("قيمة الفاتورة كبيرة جداً");
                            return;
                        }

                        int amount = (int) amountLong;

                        ContentValues cv =
                                new ContentValues();

                        cv.put("subscriber_id", subscriberId);
                        cv.put("old_reading", oldValue);
                        cv.put("new_reading", newValue);
                        cv.put("units", units);
                        cv.put("amount", amount);
                        cv.put("date", today());

                        long result = db.insert(
                                "invoices", null, cv);

                        if (result != -1) {
                            int totalBalance =
                                    invoiceBalance(subscriberId);

                            msg(
                                    "تم إصدار الفاتورة بنجاح" +
                                    "\nعدد الوحدات: " + units +
                                    "\nسعر الوحدة: " + price +
                                    " ريال" +
                                    "\nقيمة الفاتورة: " +
                                    amount + " ريال" +
                                    "\nالمتأخرات السابقة: " +
                                    arrears + " ريال" +
                                    "\nإجمالي المستحقات المتبقية: " +
                                    totalBalance + " ريال");
                        } else {
                            msg("تعذر حفظ الفاتورة");
                        }

                    } catch (Exception e) {
                        msg("أدخل قراءات صحيحة للعداد");
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    // =========================
    // حساب المتأخرات والمستحقات
    // =========================

    int subscriberArrears(int subscriberId) {
        Cursor c = db.rawQuery(
                "SELECT COALESCE(previous,0) " +
                "FROM subscribers WHERE id=?",
                new String[]{
                    String.valueOf(subscriberId)
                });

        int result = 0;

        if (c.moveToFirst()) {
            result = c.getInt(0);
        }

        c.close();
        return result;
    }

    int invoiceBalance(int subscriberId) {
        Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(amount),0) " +
                "FROM invoices WHERE subscriber_id=?",
                new String[]{
                    String.valueOf(subscriberId)
                });

        c.moveToFirst();
        int invoices = c.getInt(0);
        c.close();

        Cursor r = db.rawQuery(
                "SELECT COALESCE(SUM(amount),0) " +
                "FROM receipts WHERE subscriber_id=?",
                new String[]{
                    String.valueOf(subscriberId)
                });

        r.moveToFirst();
        int paid = r.getInt(0);
        r.close();

        int arrears = subscriberArrears(subscriberId);

        return invoices + arrears - paid;
    }

    // =========================
    // سند قبض
    // =========================

    void showReceipts() {
        startPage("سند قبض");

        EditText search = input(
                "ابحث بالاسم أو رقم المحل");

        mainLayout.addView(search);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayou