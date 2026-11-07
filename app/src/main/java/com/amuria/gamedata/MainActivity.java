package com.amuria.gamedata;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText input;
    private TextView output;
    private static final int PICK_FILE = 42;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF0B0F14);
        getWindow().setNavigationBarColor(0xFF0B0F14);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 22, 18, 12);
        root.setBackgroundColor(0xFF0B0F14);

        TextView title = new TextView(this);
        title.setText("AMURIA  •  DATA ANALYZER");
        title.setTextColor(0xFF7EE787); title.setTextSize(20); title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Local file inspection · JSON · Base64 · Protobuf · PCAP");
        sub.setTextColor(0xFF9DA7B3); sub.setTextSize(12);
        root.addView(sub);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        addButton(actions, "OPEN FILE", v -> pickFile());
        addButton(actions, "JSON", v -> formatJson());
        addButton(actions, "BASE64", v -> decodeBase64());
        addButton(actions, "PROTO", v -> decodeProto());
        root.addView(actions);

        input = new EditText(this);
        input.setHint("Paste JSON, Base64 text, protobuf hex, or inspect a local file…");
        input.setHintTextColor(0xFF687482); input.setTextColor(0xFFE6EDF3);
        input.setTextSize(13); input.setGravity(Gravity.TOP);
        input.setBackgroundColor(0xFF161B22); input.setPadding(12,12,12,12);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, 0, 1);
        ip.setMargins(0,14,0,10); root.addView(input, ip);

        TextView outLabel = new TextView(this);
        outLabel.setText("ANALYSIS OUTPUT"); outLabel.setTextColor(0xFF79C0FF);
        outLabel.setTypeface(Typeface.DEFAULT_BOLD); root.addView(outLabel);

        ScrollView scroll = new ScrollView(this);
        output = new TextView(this); output.setTextColor(0xFFC9D1D9);
        output.setTextSize(12); output.setTypeface(Typeface.MONOSPACE);
        output.setPadding(12,12,12,12); output.setText("Ready. Choose a local file or paste data above.");
        output.setBackgroundColor(0xFF161B22);
        scroll.addView(output);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, 0, 1);
        op.setMargins(0,8,0,0); root.addView(scroll, op);
        setContentView(root);
    }

    private void addButton(LinearLayout row, String label, View.OnClickListener click) {
        Button b = new Button(this); b.setText(label); b.setTextSize(10);
        b.setOnClickListener(click);
        row.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
    }
    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, PICK_FILE);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data);
        if (request != PICK_FILE || result != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        try (InputStream in = getContentResolver().openInputStream(uri); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            if (in == null) throw new IOException("Cannot open selected file");
            byte[] buf = new byte[8192]; int n; while ((n=in.read(buf))!=-1) {
                if (bytes.size()+n > 25*1024*1024) throw new IOException("File exceeds 25 MiB safety limit");
                bytes.write(buf,0,n);
            }
            byte[] raw = bytes.toByteArray();
            String name = uri.getLastPathSegment();
            if (isPcap(raw)) output.setText(analyzePcap(raw));
            else {
                input.setText(new String(raw, StandardCharsets.UTF_8));
                output.setText("Loaded: " + name + "\nBytes: " + raw.length + "\nSelect JSON, BASE64 or PROTO to inspect the content.");
            }
        } catch (Exception e) { output.setText("File error: " + e.getMessage()); }
    }
    private void formatJson() {
        String s=input.getText().toString().trim();
        try {
            if (s.startsWith("{")) output.setText(new JSONObject(s).toString(2));
            else if (s.startsWith("[")) output.setText(new JSONArray(s).toString(2));
            else output.setText("Input is not valid JSON object/array.");
        } catch(Exception e) { output.setText("JSON parse error: "+e.getMessage()); }
    }
    private void decodeBase64() {
        try {
            byte[] b=android.util.Base64.decode(input.getText().toString().trim(), android.util.Base64.DEFAULT);
            String text=new String(b,StandardCharsets.UTF_8);
            output.setText("Decoded bytes: "+b.length+"\nUTF-8 preview:\n"+text+"\n\nHex:\n"+hex(b,512));
        } catch(Exception e) { output.setText("Base64 decode error: "+e.getMessage()); }
    }
    private void decodeProto() {
        try {
            String s=input.getText().toString().replaceAll("\\s+","");
            if (s.length()%2!=0 || !s.matches("(?i)[0-9a-f]*")) throw new IllegalArgumentException("Enter even-length hexadecimal bytes.");
            byte[] b=new byte[s.length()/2]; for(int i=0;i<b.length;i++) b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);
            StringBuilder out=new StringBuilder("PROTOBUF WIRE FIELDS\n");
            int[] pos={0}; int count=0;
            while(pos[0]<b.length && count++<1000) {
                long tag=varint(b,pos); int field=(int)(tag>>>3), wire=(int)(tag&7);
                if(field<=0) throw new IOException("Invalid field tag");
                out.append("\nfield ").append(field).append(" (wire ").append(wire).append("): ");
                if(wire==0) out.append(varint(b,pos));
                else if(wire==1) { require(b,pos[0],8); long v=ByteBuffer.wrap(b,pos[0],8).order(ByteOrder.LITTLE_ENDIAN).getLong(); pos[0]+=8; out.append(String.format(Locale.US,"0x%016X",v)); }
                else if(wire==2) { long len=varint(b,pos); if(len<0||len> b.length-pos[0]) throw new IOException("Invalid length"); int l=(int)len; byte[] v=new byte[l]; System.arraycopy(b,pos[0],v,0,l); pos[0]+=l; out.append("len=").append(l).append(" hex=").append(hex(v,128)); String t=new String(v,StandardCharsets.UTF_8); if(isPrintable(t)) out.append(" text=").append(t); }
                else if(wire==5) { require(b,pos[0],4); int v=ByteBuffer.wrap(b,pos[0],4).order(ByteOrder.LITTLE_ENDIAN).getInt(); pos[0]+=4; out.append(String.format(Locale.US,"0x%08X",v)); }
                else throw new IOException("Unsupported/invalid wire type "+wire);
            }
            output.setText(out.toString());
        } catch(Exception e) { output.setText("Protobuf decode error: "+e.getMessage()); }
    }
    private static long varint(byte[] b,int[] p) throws IOException {
        long v=0; int shift=0;
        while(shift<64) { require(b,p[0],1); int x=b[p[0]++]&255; v|=((long)(x&127))<<shift; if((x&128)==0)return v; shift+=7; }
        throw new IOException("Malformed varint");
    }
    private static void require(byte[] b,int p,int n)throws IOException { if(p<0||n<0||p>b.length-n)throw new IOException("Truncated input"); }
    private static boolean isPrintable(String s) { if(s.isEmpty())return false; for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c<32 && c!='\n'&&c!='\r'&&c!='\t')return false;}return true; }
    private static String hex(byte[] b,int max) { StringBuilder s=new StringBuilder(); for(int i=0;i<Math.min(b.length,max);i++){s.append(String.format(Locale.US,"%02X ",b[i]&255));} if(b.length>max)s.append("…"); return s.toString(); }

    private boolean isPcap(byte[] b) { return b.length>=24 && ((b[0]&255)==0xD4&&(b[1]&255)==0xC3&&(b[2]&255)==0xB2&&(b[3]&255)==0xA1 || (b[0]&255)==0xA1&&(b[1]&255)==0xB2&&(b[2]&255)==0xC3&&(b[3]&255)==0xD4); }
    private String analyzePcap(byte[] b) {
        try {
            boolean little=(b[0]&255)==0xD4;
            ByteOrder order=little?ByteOrder.LITTLE_ENDIAN:ByteOrder.BIG_ENDIAN;
            ByteBuffer x=ByteBuffer.wrap(b).order(order);
            int link=x.getInt(20);
            if(link!=1)return "PCAP detected, but link type "+link+" is not Ethernet (1).";
            int p=24, packets=0; StringBuilder out=new StringBuilder("PCAP ANALYSIS\nEthernet / IPv4 packet metadata only\n");
            while(p+16<=b.length && packets<10000) {
                long sec=Integer.toUnsignedLong(x.getInt(p)); int incl=x.getInt(p+8); p+=16;
                if(incl<0||incl>b.length-p) throw new IOException("Invalid packet length");
                int end=p+incl; packets++;
                if(incl>=34 && (b[p+12]&255)==8 && (b[p+13]&255)==0) {
                    int ihl=(b[p+14]&15)*4; int proto=b[p+23]&255;
                    String src=ip(b,p+26), dst=ip(b,p+30); int l4=p+14+ihl;
                    out.append("\n#").append(packets).append(" t=").append(sec).append(" ").append(src).append(" -> ").append(dst);
                    if(proto==6 && l4+4<=end) out.append(" TCP ").append(u16(b,l4)).append(" -> ").append(u16(b,l4+2));
                    else if(proto==17 && l4+4<=end) out.append(" UDP ").append(u16(b,l4)).append(" -> ").append(u16(b,l4+2));
                    else out.append(" IP proto=").append(proto);
                } else out.append("\n#").append(packets).append(" non-IPv4/short frame (").append(incl).append(" bytes)");
                p=end;
            }
            out.append("\n\nPackets parsed: ").append(packets).append("\nNote: metadata only; no TLS decryption or live interception.");
            return out.toString();
        } catch(Exception e) { return "PCAP parse error: "+e.getMessage(); }
    }
    private static int u16(byte[] b,int p){return ((b[p]&255)<<8)|(b[p+1]&255);}
    private static String ip(byte[] b,int p){return (b[p]&255)+"."+(b[p+1]&255)+"."+(b[p+2]&255)+"."+(b[p+3]&255);}
}
