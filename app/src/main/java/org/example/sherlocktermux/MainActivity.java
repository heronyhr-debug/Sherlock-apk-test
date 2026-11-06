package org.example.sherlocktermux;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.view.View;
import java.util.ArrayList;
public class MainActivity extends Activity {
 EditText users,workers,proxy; CheckBox txt,csv,json; TextView status;
 EditText field(LinearLayout box,String title,String hint,String value) {
  TextView t=new TextView(this);t.setText(title);t.setTextSize(16);box.addView(t);
  EditText e=new EditText(this);e.setHint(hint);e.setText(value);box.addView(e);return e;
 }
 void msg(String s){status.setText(s);Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 public void onCreate(Bundle state){
  super.onCreate(state);
  ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);
  box.setOrientation(1);box.setPadding(28,30,28,30);scroll.addView(box);
  TextView title=new TextView(this);title.setText("Sherlock · 公开用户名检索");title.setTextSize(23);box.addView(title);
  users=field(box,"用户名（逗号、空格或换行分隔）","alice, bob","");
  workers=field(box,"并发任务数 1–10","3","3");
  proxy=field(box,"代理（可留空）","http://127.0.0.1:8080","");
  txt=new CheckBox(this);txt.setText("TXT");txt.setChecked(true);box.addView(txt);
  csv=new CheckBox(this);csv.setText("CSV");csv.setChecked(true);box.addView(csv);
  json=new CheckBox(this);json.setText("JSON");json.setChecked(true);box.addView(json);
  Button run=new Button(this);run.setText("开始查询");box.addView(run);run.setOnClickListener(v->launch());
  Button files=new Button(this);files.setText("打开导出文件");box.addView(files);
  files.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);});
  status=new TextView(this);status.setText("先按 README 在 Termux 安装 Sherlock。");box.addView(status);
  setContentView(scroll);
 }
 void launch(){
  ArrayList<String> names=new ArrayList<>();
  for(String s:users.getText().toString().split("[,\\s]+")){
   if(s.isEmpty())continue;
   if(!s.matches("[A-Za-z0-9_.-]{1,64}")){msg("用户名格式无效");return;}
   if(!names.contains(s))names.add(s);
  }
  if(names.size()<1||names.size()>50){msg("用户名数量须为 1–50");return;}
  int n;try{n=Integer.parseInt(workers.getText().toString());}catch(Exception e){msg("并发数必须为整数");return;}
  if(n<1||n>10){msg("并发数须为 1–10");return;}
  String p=proxy.getText().toString().trim();
  if(!p.isEmpty()&&!p.startsWith("http://")&&!p.startsWith("https://")&&!p.startsWith("socks5://")){msg("代理协议无效");return;}
  String formats=(txt.isChecked()?"txt,":"")+(csv.isChecked()?"csv,":"")+(json.isChecked()?"json,":"");
  if(formats.isEmpty()){msg("请选择导出格式");return;}
  ArrayList<String> args=new ArrayList<>();
  args.add("/data/data/com.termux/files/home/sherlock-android/runner.py");
  args.add("--users");args.addAll(names);args.add("--workers");args.add(""+n);
  args.add("--formats");args.add(formats.substring(0,formats.length()-1));
  if(!p.isEmpty()){args.add("--proxy");args.add(p);}
  Intent i=new Intent("com.termux.RUN_COMMAND");
  i.setClassName("com.termux","com.termux.app.RunCommandService");
  i.putExtra("com.termux.RUN_COMMAND_PATH","/data/data/com.termux/files/usr/bin/python");
  i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",args.toArray(new String[0]));
  i.putExtra("com.termux.RUN_COMMAND_WORKDIR","/data/data/com.termux/files/home");
  i.putExtra("com.termux.RUN_COMMAND_BACKGROUND",false);
  i.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION","0");
  try{startService(i);msg("已请求 Termux 启动，请在 Termux 查看进度及日志。");}
  catch(Exception e){msg("启动失败，请检查 Termux 权限和外部命令配置："+e.getMessage());}
 }
}
