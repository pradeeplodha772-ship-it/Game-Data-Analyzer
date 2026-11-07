package com.amuria.gamedata;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.net.ConnectivityManager;
import android.os.Process;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_FILE=42;
    private String selectedPackage="", selectedLabel="";
    private LinearLayout root, content, header, bottom;
    private TextView selectedSummary;
    private EditText toolInput;
    private TextView toolOutput;
    private TextView usageSummary;
    private String currentTab="Home";
    private final int BG=0xFF070A12, PANEL=0xFF111827, PANEL2=0xFF151D2D;
    private final int PURPLE=0xFFB66BFF, CYAN=0xFF35D8E8, TEXT=0xFFF4F5FA, MUTED=0xFF9AA8BD;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        selectedPackage=getSharedPreferences("prni",MODE_PRIVATE).getString("pkg","");
        selectedLabel=getSharedPreferences("prni",MODE_PRIVATE).getString("label","");
        buildShell(); showTab("Home");
    }
    private GradientDrawable shape(int color,int stroke,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius);
        if(stroke!=0)d.setStroke(1,stroke); return d;
    }
    private TextView text(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private void pad(View v,int a){v.setPadding(a,a,a,a);}
    private void buildShell() {
        root=vertical(); root.setBackgroundColor(BG);
        header=vertical(); header.setPadding(18,12,18,10); header.setBackgroundColor(0xFF090D18);
        LinearLayout brand=horizontal();
        TextView logo=text("PR",17,0xFFFFFFFF,true); logo.setGravity(Gravity.CENTER);
        logo.setBackground(shape(0xFF281446,PURPLE,18)); brand.addView(logo,lp(42,42));
        LinearLayout names=vertical(); names.setPadding(12,0,0,0);
        names.addView(text("P.R Network Intelligence",18,TEXT,true));
        names.addView(text("NETWORK DIAGNOSTICS  •  PRO",9,CYAN,true)); brand.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView menu=text("⋮",26,TEXT,true); brand.addView(menu);
        header.addView(brand);
        header.addView(text("Advanced per-app traffic analysis",11,MUTED,false));
        root.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        content=vertical(); content.setPadding(16,14,16,18); scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        bottom=horizontal(); bottom.setPadding(4,6,4,8); bottom.setBackgroundColor(0xFF090D18);
        root.addView(bottom,lp(-1,62)); setContentView(root);
    }
    private void showTab(String tab) {
        currentTab=tab; content.removeAllViews(); bottom.removeAllViews();
        if("Home".equals(tab)) dashboard();
        else if("Sessions".equals(tab)) sessions();
        else if("Hosts".equals(tab)) hosts();
        else if("Tools".equals(tab)) tools();
        else if("Logs".equals(tab)) logs();
        else settings();
        String[] tabs={"Home","Sessions","Hosts","Tools","Logs","More"};
        for(String name:tabs){
            TextView b=text(icon(name)+"\n"+name,10,name.equals(tab)?PURPLE:MUTED,true);
            b.setGravity(Gravity.CENTER); b.setPadding(2,4,2,2);
            b.setOnClickListener(v->showTab(name)); bottom.addView(b,new LinearLayout.LayoutParams(0,-1,1));
        }
    }
    private String icon(String t){switch(t){case "Home":return "⌂";case "Sessions":return "▤";case "Hosts":return "◎";case "Tools":return "⌘";case "Logs":return "▣";default:return "•••";}}
    private void section(String title,String subtitle) {
        content.addView(text(title,18,TEXT,true));
        if(subtitle!=null)content.addView(text(subtitle,11,MUTED,false));
        View gap=new View(this);content.addView(gap,lp(1,10));
    }
    private LinearLayout card() {
        LinearLayout c=vertical(); c.setPadding(14,14,14,14); c.setBackground(shape(PANEL,0xFF27334A,18));
        LinearLayout.LayoutParams p=lp(-1,-2);p.setMargins(0,0,0,12);content.addView(c,p);return c;
    }
    private TextView action(String label,Runnable r) {
        TextView b=text(label,12,Color.WHITE,true);b.setGravity(Gravity.CENTER);b.setPadding(12,13,12,13);
        b.setBackground(shape(0xFF48217A,PURPLE,14));b.setOnClickListener(v->r.run());return b;
    }
    private void dashboard() {
        LinearLayout hero=card(); hero.setBackground(shape(0xFF10162A,0xFF6542A5,22));
        TextView state=text("●  ENGINE NOT CONNECTED",12,0xFFFFC66D,true);hero.addView(state);
        TextView big=text("READY TO CONFIGURE",21,TEXT,true);big.setPadding(0,10,0,4);hero.addView(big);
        hero.addView(text("Choose one installed app. Live capture becomes available after the VPN forwarding engine is installed.",11,MUTED,false));
        LinearLayout row=horizontal(); row.setPadding(0,14,0,0);
        row.addView(action("SELECT TARGET APP",()->showAppPicker()),new LinearLayout.LayoutParams(0,-2,1));
        TextView gap=text(" ",8,TEXT,false);row.addView(gap,lp(8,1));
        row.addView(action("FLOATING WINDOW",()->openFloating()),new LinearLayout.LayoutParams(0,-2,1));hero.addView(row);
        if(!selectedPackage.isEmpty()){
            LinearLayout selected=card();selected.addView(text("MONITORED APP TARGET",10,CYAN,true));
            selected.addView(text(selectedLabel,17,TEXT,true));selected.addView(text(selectedPackage,10,MUTED,false));
            TextView clear=action("CHANGE APP",()->showAppPicker());clear.setPadding(10,10,10,10);selected.addView(clear);
        }
        section("NETWORK OVERVIEW","Live counters will populate when capture engine is active.");
        LinearLayout usage=card();
        usage.addView(text("ANDROID NETWORK USAGE",12,CYAN,true));
        usageSummary=text("Choose an app, grant Usage Access, then refresh OS-reported Wi-Fi bytes.",11,MUTED,false);
        usageSummary.setPadding(0,8,0,10);usage.addView(usageSummary);
        usage.addView(action("REFRESH SELECTED APP USAGE",()->loadSelectedAppUsage()));
        usage.addView(action("GRANT USAGE ACCESS",()->startActivity(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        usage.addView(text("Android reports per-app byte totals with system-dependent delay. This is not packet capture or HTTP content inspection.",10,MUTED,false));
        LinearLayout quick=card();quick.addView(text("QUICK ACCESS",13,TEXT,true));
        String[][] items={{"Sessions","View HTTP / HTTPS session metadata"},{"Hosts","DNS, hosts and connection summaries"},{"Tools","JSON, Base64, Protobuf, Hex and PCAP"},{"Logs","Capture diagnostics and app events"}};
        for(String[] it:items){TextView b=text(it[0]+"   ›   "+it[1],12,CYAN,true);b.setPadding(2,12,2,12);b.setOnClickListener(v->showTab(it[0]));quick.addView(b);}
        LinearLayout note=card();note.addView(text("PRIVACY & CAPTURE STATUS",12,CYAN,true));
        note.addView(text("No traffic is being captured in this build. The VPN/TUN forwarding engine must be implemented before sessions, hosts and live speed can display real values. No simulated traffic counters are shown.",11,MUTED,false));
    }

    private boolean hasUsageAccess(){
        AppOpsManager ops=(AppOpsManager)getSystemService(APP_OPS_SERVICE);
        int mode=ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),getPackageName());
        return mode==AppOpsManager.MODE_ALLOWED;
    }
    private void loadSelectedAppUsage(){
        if(selectedPackage.isEmpty()){usageSummary.setText("Select an installed app first.");return;}
        if(!hasUsageAccess()){
            usageSummary.setText("Usage Access permission is required. Tap GRANT USAGE ACCESS, enable P.R Network Intelligence, return and refresh.");
            return;
        }
        try{
            int uid=getPackageManager().getApplicationInfo(selectedPackage,0).uid;
            java.util.Calendar cal=java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY,0);cal.set(java.util.Calendar.MINUTE,0);
            cal.set(java.util.Calendar.SECOND,0);cal.set(java.util.Calendar.MILLISECOND,0);
            long start=cal.getTimeInMillis(),end=System.currentTimeMillis();
            NetworkStatsManager manager=(NetworkStatsManager)getSystemService(NETWORK_STATS_SERVICE);
            NetworkStats stats=manager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI,null,start,end,uid);
            long rx=0,tx=0;NetworkStats.Bucket bucket=new NetworkStats.Bucket();
            while(stats.hasNextBucket()){stats.getNextBucket(bucket);rx+=bucket.getRxBytes();tx+=bucket.getTxBytes();}
            stats.close();
            usageSummary.setText("APP: "+selectedLabel+"\nPACKAGE: "+selectedPackage+
                "\nTODAY WI-FI DOWNLOAD: "+formatBytes(rx)+
                "\nTODAY WI-FI UPLOAD: "+formatBytes(tx)+
                "\nUPDATED: "+new java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(new java.util.Date()));
        }catch(Exception e){usageSummary.setText("Unable to read usage stats: "+e.getClass().getSimpleName()+" — "+e.getMessage());}
    }
    private String formatBytes(long n){
        if(n<1024)return n+" B";
        if(n<1024*1024)return String.format(java.util.Locale.US,"%.1f KB",n/1024.0);
        if(n<1024L*1024*1024)return String.format(java.util.Locale.US,"%.2f MB",n/(1024.0*1024));
        return String.format(java.util.Locale.US,"%.2f GB",n/(1024.0*1024*1024));
    }
    private View stat(String label,String value){LinearLayout c=vertical();c.setPadding(10,12,10,12);c.setBackground(shape(PANEL2,0xFF28344A,14));c.addView(text(label,9,MUTED,true));c.addView(text(value,19,TEXT,true));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(0,0,6,0);c.setLayoutParams(p);return c;}
    private void sessions() {
        section("SESSIONS","HTTP · HTTPS metadata · WebSocket metadata · TCP · UDP");
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Search URL, host or method");search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setBackground(shape(PANEL,0xFF33415A,14));content.addView(search,lp(-1,48));
        LinearLayout filters=horizontal();for(String f:new String[]{"ALL","HTTP","HTTPS","WS","DNS","TCP"}){TextView t=text(f,10,CYAN,true);t.setPadding(9,12,9,12);filters.addView(t);}content.addView(filters);
        LinearLayout empty=card();empty.setGravity(Gravity.CENTER);TextView glyph=text("◌",42,PURPLE,true);empty.addView(glyph);empty.addView(text("No sessions captured yet",16,TEXT,true));empty.addView(text("Select an app on Home. Live session rows will appear here after the VPN capture engine is connected.",11,MUTED,false));
    }
    private void hosts() {
        section("HOSTS & CONNECTIONS","DNS lookups · Remote endpoints · TLS metadata");
        LinearLayout c=card();c.addView(text("Waiting for capture engine",16,TEXT,true));c.addView(text("Hostnames, IP addresses, ports, protocol and connection counts will be listed here from actual captured metadata.",11,MUTED,false));
    }
    private void tools() {
        section("TOOLS LAB","Offline analyzers and safe diagnostics");
        LinearLayout grid=vertical();
        String[][] names={{"JSON FORMAT","BASE64 DECODE"},{"PROTOBUF HEX","HEX VIEWER"},{"PCAP INSPECT","OPEN FILE"}};
        for(String[] pair:names){LinearLayout r=horizontal();for(String n:pair){TextView b=action(n,()->{if(n.equals("OPEN FILE")||n.equals("PCAP INSPECT"))pickFile();else if(n.equals("JSON FORMAT"))runJson();else if(n.equals("BASE64 DECODE"))runBase64();else if(n.equals("PROTOBUF HEX"))runProto();else runHex();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(0,0,8,8);r.addView(b,p);}grid.addView(r);}
        content.addView(grid);
        toolInput=new EditText(this);toolInput.setGravity(Gravity.TOP);toolInput.setMinLines(5);toolInput.setHint("Paste JSON, Base64, protobuf hex or bytes…");toolInput.setTextColor(TEXT);toolInput.setHintTextColor(MUTED);toolInput.setPadding(12,12,12,12);toolInput.setBackground(shape(PANEL,0xFF33415A,14));content.addView(toolInput,lp(-1,150));
        toolOutput=text("Output will appear here.",11,0xFFB8C5D8,false);toolOutput.setTypeface(Typeface.MONOSPACE);toolOutput.setPadding(12,12,12,12);toolOutput.setBackground(shape(PANEL2,0,12));LinearLayout.LayoutParams op=lp(-1,-2);op.setMargins(0,10,0,0);content.addView(toolOutput,op);
    }
    private void logs(){section("DIAGNOSTICS","App and capture engine status");LinearLayout c=card();c.addView(text("APP UI: READY",12,CYAN,true));c.addView(text("APP PICKER: AVAILABLE",12,CYAN,true));c.addView(text("FLOATING OVERLAY: ON-DEMAND",12,CYAN,true));c.addView(text("VPN ENGINE: NOT IMPLEMENTED",12,0xFFFFC66D,true));c.addView(text("LIVE SESSION STORE: NOT CONNECTED",12,0xFFFFC66D,true));}
    private void settings(){section("MORE & SETTINGS","Privacy, permissions and export");LinearLayout c=card();c.addView(text("Selected app",12,CYAN,true));c.addView(text(selectedPackage.isEmpty()?"None":selectedLabel+"\n"+selectedPackage,12,TEXT,false));c.addView(action("SELECT APP",()->showAppPicker()));c.addView(action("OPEN FLOATING CONTROLS",()->openFloating()));c.addView(text("HTTPS plaintext inspection requires an owned debug app configured to trust a test CA. This app does not bypass certificate pinning or app security controls.",11,MUTED,false));}
    private void openFloating(){
        if(android.os.Build.VERSION.SDK_INT>=23&&!android.provider.Settings.canDrawOverlays(this)){
            Intent i=new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);return;
        }
        Intent i=new Intent(this,FloatingControlService.class);if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
    }
    private void showAppPicker(){
        ArrayList<android.content.pm.ApplicationInfo> all=new ArrayList<>();android.content.pm.PackageManager pm=getPackageManager();
        for(android.content.pm.ApplicationInfo a:pm.getInstalledApplications(0))if(!a.packageName.equals(getPackageName()))all.add(a);
        Collections.sort(all,(a,b)->pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));
        LinearLayout box=vertical();EditText q=new EditText(this);q.setSingleLine(true);q.setHint("Search installed apps");box.addView(q);
        ListView list=new ListView(this);box.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        ArrayList<android.content.pm.ApplicationInfo> shown=new ArrayList<>(all);
        BaseAdapter adapter=new BaseAdapter(){public int getCount(){return shown.size();}public Object getItem(int p){return shown.get(p);}public long getItemId(int p){return p;}
            public View getView(int p,View old,android.view.ViewGroup parent){android.content.pm.ApplicationInfo a=shown.get(p);LinearLayout r=horizontal();r.setPadding(10,10,10,10);ImageView icon=new ImageView(MainActivity.this);try{icon.setImageDrawable(pm.getApplicationIcon(a));}catch(Exception ignored){}r.addView(icon,lp(42,42));LinearLayout n=vertical();n.setPadding(12,0,0,0);n.addView(text(pm.getApplicationLabel(a).toString(),14,TEXT,true));n.addView(text(a.packageName,10,MUTED,false));r.addView(n);return r;}};
        list.setAdapter(adapter);
        q.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){String f=s.toString().toLowerCase(Locale.ROOT);shown.clear();for(android.content.pm.ApplicationInfo a:all)if(pm.getApplicationLabel(a).toString().toLowerCase(Locale.ROOT).contains(f)||a.packageName.toLowerCase(Locale.ROOT).contains(f))shown.add(a);adapter.notifyDataSetChanged();}public void afterTextChanged(android.text.Editable e){}});
        android.app.AlertDialog d=new android.app.AlertDialog.Builder(this).setTitle("Select target app").setView(box).setNegativeButton("CANCEL",null).create();
        list.setOnItemClickListener((p,v,pos,id)->{android.content.pm.ApplicationInfo a=shown.get(pos);selectedPackage=a.packageName;selectedLabel=pm.getApplicationLabel(a).toString();getSharedPreferences("prni",MODE_PRIVATE).edit().putString("pkg",selectedPackage).putString("label",selectedLabel).apply();d.dismiss();showTab("Home");});
        d.show();d.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94),(int)(getResources().getDisplayMetrics().heightPixels*.78));
    }
    private void pickFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_FILE);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK_FILE&&c==RESULT_OK&&d!=null){try(InputStream in=getContentResolver().openInputStream(d.getData());ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0){if(b.size()+n>25*1024*1024)throw new IOException("25 MiB limit");b.write(buf,0,n);}showTab("Tools");if(toolInput!=null)toolInput.setText(new String(b.toByteArray(),StandardCharsets.UTF_8));}catch(Exception e){new android.app.AlertDialog.Builder(this).setMessage("File error: "+e.getMessage()).setPositiveButton("OK",null).show();}}}
    private String val(){return toolInput==null?"":toolInput.getText().toString().trim();}
    private void out(String s){if(toolOutput!=null)toolOutput.setText(s);}
    private void runJson(){try{String s=val();out(s.startsWith("[")?new JSONArray(s).toString(2):new JSONObject(s).toString(2));}catch(Exception e){out("JSON error: "+e.getMessage());}}
    private void runBase64(){try{byte[] b=android.util.Base64.decode(val(),android.util.Base64.DEFAULT);out("Bytes: "+b.length+"\n"+new String(b,StandardCharsets.UTF_8)+"\nHEX: "+hex(b));}catch(Exception e){out("Base64 error: "+e.getMessage());}}
    private void runHex(){try{String s=val().replaceAll("\\s+","");if(s.length()%2!=0||!s.matches("(?i)[0-9a-f]*"))throw new Exception("Enter even-length hex.");byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(2*i,2*i+2),16);out("Bytes: "+b.length+"\nUTF-8: "+new String(b,StandardCharsets.UTF_8)+"\nHEX: "+hex(b));}catch(Exception e){out("Hex error: "+e.getMessage());}}
    private void runProto(){try{String s=val().replaceAll("\\s+","");if(s.length()%2!=0||!s.matches("(?i)[0-9a-f]*"))throw new Exception("Enter even-length protobuf hex.");byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);StringBuilder o=new StringBuilder("PROTOBUF WIRE FIELDS\n");int[] p={0};while(p[0]<b.length){long tag=varint(b,p);int f=(int)(tag>>>3),w=(int)(tag&7);o.append("\nField ").append(f).append(" wire ").append(w).append(": ");if(w==0)o.append(varint(b,p));else if(w==1){need(b,p[0],8);o.append(ByteBuffer.wrap(b,p[0],8).order(ByteOrder.LITTLE_ENDIAN).getLong());p[0]+=8;}else if(w==2){long z=varint(b,p);if(z<0||z>b.length-p[0])throw new Exception("Bad length");int len=(int)z;o.append("len=").append(len).append(" hex=");byte[] v=Arrays.copyOfRange(b,p[0],p[0]+len);o.append(hex(v));p[0]+=len;}else if(w==5){need(b,p[0],4);o.append(ByteBuffer.wrap(b,p[0],4).order(ByteOrder.LITTLE_ENDIAN).getInt());p[0]+=4;}else throw new Exception("Unsupported wire type "+w);}out(o.toString());}catch(Exception e){out("Protobuf error: "+e.getMessage());}}
    private static long varint(byte[] b,int[] p)throws Exception{long v=0;for(int sh=0;sh<64;sh+=7){need(b,p[0],1);int x=b[p[0]++]&255;v|=(long)(x&127)<<sh;if((x&128)==0)return v;}throw new Exception("Bad varint");}
    private static void need(byte[] b,int p,int n)throws Exception{if(p<0||p+n>b.length)throw new Exception("Truncated bytes");}
    private String hex(byte[] b){StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(b.length,512);i++)s.append(String.format(Locale.US,"%02X ",b[i]&255));return s.toString();}
}
