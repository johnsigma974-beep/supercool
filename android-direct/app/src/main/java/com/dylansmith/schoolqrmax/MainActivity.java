package com.dylansmith.schoolqrmax;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_QR = 1107;
    private static final int BLUE = Color.rgb(77,163,255);
    private static final int BG = Color.rgb(7,11,20);
    private static final int CARD = Color.rgb(18,25,40);
    private static final int MUTED = Color.rgb(160,174,192);
    private static final String QR_FILE = "custom_qr.png";
    private static final String PREFS = "schoolqr";

    private LinearLayout root, timeline, weekBox;
    private TextView clock, date, currentTitle, currentMeta, nextTitle, nextMeta, dismissText, noteView;
    private ImageView qr;
    private Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable(){ public void run(){ refreshTime(); handler.postDelayed(this, 1000); }};

    static class Block {
        String name, room, start, end;
        Block(String n,String r,String s,String e){name=n;room=r;start=s;end=e;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        loadQr();
        loadNote();
        renderToday();
    }

    @Override protected void onResume(){ super.onResume(); handler.removeCallbacks(tick); handler.post(tick); }
    @Override protected void onPause(){ super.onPause(); handler.removeCallbacks(tick); }

    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    private TextView tv(String text,int sp,int color,boolean bold){
        TextView t=new TextView(this); t.setText(text); t.setTextSize(sp); t.setTextColor(color); t.setPadding(0,0,0,0); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private GradientDrawable bg(int color,int radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private View gap(int h){ Space s=new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h))); return s; }
    private LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18),dp(16),dp(18),dp(16)); c.setBackground(bg(CARD,20)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(12)); c.setLayoutParams(lp); return c; }
    private Button button(String label, boolean primary){ Button b=new Button(this); b.setAllCaps(false); b.setText(label); b.setTextSize(15); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setTextColor(primary?Color.rgb(5,15,28):Color.WHITE); b.setBackground(bg(primary?BLUE:Color.rgb(29,39,58),16)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(50),1); lp.setMargins(0,0,dp(8),0); b.setLayoutParams(lp); return b; }

    private void buildUi(){
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); sv.setBackgroundColor(BG);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(18),dp(16),dp(28)); sv.addView(root,new ScrollView.LayoutParams(-1,-2)); setContentView(sv);

        TextView brand=tv("SCHOOLQR",13,BLUE,true); root.addView(brand);
        root.addView(gap(4));
        TextView name=tv("Dylan Smith",30,Color.WHITE,true); root.addView(name);
        TextView school=tv("Anderson New Technology High School",16,MUTED,false); root.addView(school);
        root.addView(gap(16));

        LinearLayout hero=card();
        clock=tv("--:--",42,Color.WHITE,true); hero.addView(clock);
        date=tv("",15,MUTED,false); hero.addView(date); root.addView(hero);

        LinearLayout status=card();
        status.addView(tv("CURRENT",12,BLUE,true)); status.addView(gap(6));
        currentTitle=tv("Checking schedule…",24,Color.WHITE,true); currentMeta=tv("",15,MUTED,false); status.addView(currentTitle); status.addView(currentMeta);
        status.addView(gap(16)); status.addView(tv("NEXT",12,BLUE,true)); status.addView(gap(4));
        nextTitle=tv("—",18,Color.WHITE,true); nextMeta=tv("",14,MUTED,false); status.addView(nextTitle); status.addView(nextMeta);
        status.addView(gap(14)); dismissText=tv("",15,BLUE,true); status.addView(dismissText); root.addView(status);

        LinearLayout qrCard=card(); qrCard.addView(tv("CLASS QR",12,BLUE,true)); qrCard.addView(gap(12));
        FrameLayout qf=new FrameLayout(this); qf.setPadding(dp(14),dp(14),dp(14),dp(14)); qf.setBackground(bg(Color.WHITE,18));
        qr=new ImageView(this); qr.setAdjustViewBounds(true); qr.setScaleType(ImageView.ScaleType.FIT_CENTER); qf.addView(qr,new FrameLayout.LayoutParams(-1,dp(280))); qrCard.addView(qf);
        TextView hint=tv("Tap QR for fullscreen scan mode",13,MUTED,false); hint.setGravity(Gravity.CENTER); hint.setPadding(0,dp(10),0,0); qrCard.addView(hint);
        qr.setOnClickListener(v->showFullscreenQr());
        root.addView(qrCard);

        LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-1,-2); alp.setMargins(0,0,0,dp(12)); actions.setLayoutParams(alp);
        Button replace=button("Replace QR",true), reset=button("Reset",false), edit=button("Edit note",false);
        actions.addView(replace); actions.addView(reset); actions.addView(edit); root.addView(actions);
        replace.setOnClickListener(v->pickQr()); reset.setOnClickListener(v->confirmReset()); edit.setOnClickListener(v->editNote());

        LinearLayout note=card(); note.addView(tv("QUICK NOTE",12,BLUE,true)); note.addView(gap(6)); noteView=tv("No note set",16,Color.WHITE,false); note.addView(noteView); root.addView(note);

        LinearLayout today=card(); today.addView(tv("TODAY'S TIMELINE",12,BLUE,true)); today.addView(gap(10)); timeline=new LinearLayout(this); timeline.setOrientation(LinearLayout.VERTICAL); today.addView(timeline); root.addView(today);

        LinearLayout week=card(); week.addView(tv("WEEKLY OVERVIEW",12,BLUE,true)); week.addView(gap(10));
        weekBox=new LinearLayout(this); weekBox.setOrientation(LinearLayout.VERTICAL); week.addView(weekBox); root.addView(week); renderWeek();

        LinearLayout privacy=card(); privacy.addView(tv("OFFLINE + PRIVATE",12,BLUE,true)); privacy.addView(gap(6)); privacy.addView(tv("No internet permission. Your replacement QR and note stay on this phone.",14,MUTED,false)); root.addView(privacy);
    }

    private void refreshTime(){
        Date now=new Date();
        clock.setText(new SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(this)?"HH:mm:ss":"h:mm:ss a",Locale.getDefault()).format(now));
        date.setText(new SimpleDateFormat("EEEE, MMMM d",Locale.getDefault()).format(now));
        renderStatusOnly();
    }

    private List<Block> todayBlocks(){
        Calendar c=Calendar.getInstance(); int d=c.get(Calendar.DAY_OF_WEEK); ArrayList<Block> a=new ArrayList<>();
        if(d==Calendar.MONDAY){
            a.add(new Block("Advisory","Bird, S · Room 129","09:00","10:15"));
            a.add(new Block("Nutrition Break","","10:15","10:25"));
            a.add(new Block("Dismissal / Early Release","","11:45","11:46"));
        } else if(d==Calendar.TUESDAY || d==Calendar.THURSDAY){
            a.add(new Block("Algebra 1","Room 122A","09:00","09:53"));
            a.add(new Block("English 1","Room 105","09:56","10:49"));
            a.add(new Block("Soc Sci 9 H","Room 105","10:52","11:45"));
            a.add(new Block("Lunch","","11:45","12:15"));
            a.add(new Block("Free / Study · Periods 4 & 5","","12:15","14:07"));
            a.add(new Block("Physics","Room 106","14:07","15:00"));
        } else if(d==Calendar.WEDNESDAY){
            a.add(new Block("3D Mod and Man","Room 106","09:00","10:20"));
            a.add(new Block("New Tech 101","Room 106","10:25","11:45"));
            a.add(new Block("Lunch","","11:45","12:15"));
            a.add(new Block("Phys Ed","Café","12:20","13:40"));
        }
        return a;
    }

    private long minuteOfDay(String hhmm){ String[] p=hhmm.split(":"); return Integer.parseInt(p[0])*60L+Integer.parseInt(p[1]); }
    private long nowMinute(){ Calendar c=Calendar.getInstance(); return c.get(Calendar.HOUR_OF_DAY)*60L+c.get(Calendar.MINUTE); }
    private String fmt(String hhmm){ try{ Date d=new SimpleDateFormat("HH:mm",Locale.US).parse(hhmm); return new SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(this)?"HH:mm":"h:mm a",Locale.getDefault()).format(d);}catch(Exception e){return hhmm;} }

    private void renderToday(){ timeline.removeAllViews(); List<Block> list=todayBlocks(); if(list.isEmpty()){ timeline.addView(tv("No normal classes today.",16,MUTED,false)); } else {
        for(Block b:list){ LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(12),dp(10),dp(12),dp(10)); row.setBackground(bg(Color.rgb(12,18,31),14)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(8)); row.setLayoutParams(lp); row.addView(tv(b.name,16,Color.WHITE,true)); String sub=fmt(b.start)+" – "+fmt(b.end)+(b.room.isEmpty()?"":" · "+b.room); row.addView(tv(sub,13,MUTED,false)); timeline.addView(row); }
        }
        renderStatusOnly();
    }

    private void renderStatusOnly(){
        if(currentTitle==null)return; List<Block> list=todayBlocks(); long n=nowMinute(); Block cur=null,next=null;
        for(Block b:list){ long s=minuteOfDay(b.start),e=minuteOfDay(b.end); if(n>=s && n<e)cur=b; if(n<s && next==null)next=b; }
        if(list.isEmpty()){ currentTitle.setText("No normal classes"); currentMeta.setText("You're off the regular schedule today."); nextTitle.setText("—"); nextMeta.setText(""); dismissText.setText(""); return; }
        if(cur!=null){ currentTitle.setText(cur.name); currentMeta.setText((cur.room.isEmpty()?"":cur.room+" · ")+fmt(cur.start)+"–"+fmt(cur.end)); }
        else { currentTitle.setText("No scheduled block"); currentMeta.setText("You're between listed schedule blocks."); }
        if(next!=null){ nextTitle.setText(next.name); long mins=minuteOfDay(next.start)-n; nextMeta.setText(fmt(next.start)+(next.room.isEmpty()?"":" · "+next.room)+" · starts in "+mins+" min"); } else { nextTitle.setText("Done for today"); nextMeta.setText("No more listed blocks."); }
        Calendar c=Calendar.getInstance(); int d=c.get(Calendar.DAY_OF_WEEK); String end=null; if(d==Calendar.MONDAY)end="11:45"; else if(d==Calendar.TUESDAY||d==Calendar.THURSDAY)end="15:00"; else if(d==Calendar.WEDNESDAY)end="13:40";
        if(end!=null){ long left=minuteOfDay(end)-n; if(left>0) dismissText.setText("Dismissal in "+(left/60>0?(left/60)+"h ":"")+(left%60)+"m"); else dismissText.setText("Dismissed for today"); }
    }

    private void renderWeek(){
        String[] rows={
            "Monday\n9:00–10:15 Advisory · Room 129\n10:15–10:25 Nutrition Break\n11:45 Early Release",
            "Tuesday\n9:00 Algebra 1 · 122A\n9:56 English 1 · 105\n10:52 Soc Sci 9 H · 105\n11:45 Lunch · Study periods · 2:07 Physics · 106\n3:00 Dismissal",
            "Wednesday\n9:00 3D Mod and Man · 106\n10:25 New Tech 101 · 106\n11:45 Lunch\n12:20 Phys Ed · Café\n1:40 Dismissal",
            "Thursday\nSame schedule as Tuesday",
            "Friday\nNo normal classes"
        };
        for(String s:rows){ TextView t=tv(s,14,Color.WHITE,false); t.setPadding(dp(12),dp(10),dp(12),dp(10)); t.setBackground(bg(Color.rgb(12,18,31),14)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(8)); t.setLayoutParams(lp); weekBox.addView(t); }
    }

    private void pickQr(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); startActivityForResult(i,PICK_QR); }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){ super.onActivityResult(requestCode,resultCode,data); if(requestCode==PICK_QR && resultCode==RESULT_OK && data!=null && data.getData()!=null){ savePickedImage(data.getData()); }}

    private void savePickedImage(Uri uri){
        try(InputStream in=getContentResolver().openInputStream(uri); FileOutputStream out=openFileOutput(QR_FILE,MODE_PRIVATE)){
            if(in==null)throw new IOException("No image stream"); byte[] buf=new byte[8192]; int r; long total=0; while((r=in.read(buf))!=-1){ total+=r; if(total>8_000_000L)throw new IOException("Image too large"); out.write(buf,0,r); }
        }catch(Exception e){ deleteFile(QR_FILE); toast("Couldn't use that image"); return; }
        Bitmap bm=BitmapFactory.decodeFile(new File(getFilesDir(),QR_FILE).getAbsolutePath()); if(bm==null){ deleteFile(QR_FILE); toast("That file isn't a valid image"); return; } qr.setImageBitmap(bm); toast("QR replaced");
    }

    private void loadQr(){ File f=new File(getFilesDir(),QR_FILE); Bitmap bm=f.exists()?BitmapFactory.decodeFile(f.getAbsolutePath()):null; if(bm!=null){qr.setImageBitmap(bm);return;} loadDefaultQr(); }
    private void loadDefaultQr(){
        String b64="iVBORw0KGgoAAAANSUhEUgAAAVYAAAFWAQAAAAB9c0EwAAABn0lEQVR4nO2bQWrjMBBFX40JtKD0FlxCxVv0eu+pXkA6Qxu0WnAJ9brrS2u5gZWseBYDcxYtP8CgT7z0AhH5/jPzzwIkSZIkSZLkpPN1nA8A+BOAf6b7fA7wPwA+SZIkSZIkSVIK4O9xPmxJKY6H4vznP8e5LwA+SZIkSZIkSUVw/u0PrwD8YwCPJEmSJEmSJKUA/nOcDwD4rQCPJEmSJEmSJKUA/rY/jPMvAfw9gEeSJEmSJEmSUgB/FefDsv7vq3GXJUnSOknqfPbjt8d7f1mAp5IkSZIkSZLkPnPvAfyPJEmSJEmSJMl9BgCfJEmSJEmSJEnaC+B/K85327jLkiR1ktS5dsf5l4u7LEmSJEmSJEmS1APwfwL4m1mSJEmSJEmSlAH4rzi/GRfZeMiSJOmL5f0d5/M57rIkSZIkSZIkSVIH4L+O85m/sEuSJEmSJEmSpA7Afx7nHyt/X/wowCNJkiRJkiRJUr+D4nxmd1mSJEmSJEmSJHUQ5zdf/L74UYBHkiRJkiRJkqQOwP8W52e5y8JdliRJkiRJkiRJaUpHf9jDvxXgkSRJkiRJkiSpAfBPUmb7/C3OI0mSJEmSJElSf4JvVuChlCQFGNQAAAAASUVORK5CYII=";
        byte[] bytes=android.util.Base64.decode(b64,android.util.Base64.DEFAULT); qr.setImageBitmap(BitmapFactory.decodeByteArray(bytes,0,bytes.length));
    }

    private void confirmReset(){ new AlertDialog.Builder(this).setTitle("Reset QR?").setMessage("Restore the bundled school QR code?").setNegativeButton("Cancel",null).setPositiveButton("Reset",(d,w)->{deleteFile(QR_FILE);loadDefaultQr();toast("QR reset");}).show(); }

    private void showFullscreenQr(){
        final Dialog d=new Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen); LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(18),dp(18),dp(18),dp(18)); box.setBackgroundColor(Color.WHITE);
        ImageView img=new ImageView(this); img.setAdjustViewBounds(true); img.setScaleType(ImageView.ScaleType.FIT_CENTER); img.setImageDrawable(qr.getDrawable()); box.addView(img,new LinearLayout.LayoutParams(-1,0,1));
        TextView close=tv("Tap anywhere to close",15,Color.DKGRAY,true); close.setGravity(Gravity.CENTER); close.setPadding(0,dp(12),0,dp(12)); box.addView(close); box.setOnClickListener(v->d.dismiss()); d.setContentView(box);
        d.setOnShowListener(x->{ Window w=d.getWindow(); if(w!=null){ w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); WindowManager.LayoutParams p=w.getAttributes(); p.screenBrightness=1f; w.setAttributes(p);} }); d.show();
    }

    private void editNote(){ final EditText e=new EditText(this); e.setText(getPreferences(MODE_PRIVATE).getString("note","")); e.setHint("Example: Bring Chromebook"); e.setSingleLine(false); e.setMinLines(3); new AlertDialog.Builder(this).setTitle("Quick note").setView(e).setNegativeButton("Cancel",null).setNeutralButton("Clear",(d,w)->{getPreferences(MODE_PRIVATE).edit().remove("note").apply();loadNote();}).setPositiveButton("Save",(d,w)->{getPreferences(MODE_PRIVATE).edit().putString("note",e.getText().toString().trim()).apply();loadNote();}).show(); }
    private void loadNote(){ if(noteView==null)return; String s=getPreferences(MODE_PRIVATE).getString("note",""); noteView.setText(s.isEmpty()?"No note set":s); }
    private void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }
}
