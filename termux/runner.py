#!/usr/bin/env python3
"""Run upstream Sherlock in bounded parallel subprocesses and export diagnostics."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import csv
import json
from pathlib import Path
import re
import subprocess
import sys
from datetime import datetime, timezone

def validate_users(users):
    if not 1 <= len(users) <= 50 or any(not re.fullmatch(r"[A-Za-z0-9_.-]{1,64}", u) for u in users):
        raise ValueError("用户名数量或格式无效")
    return list(dict.fromkeys(users))

def command(user, proxy, output):
    args=[sys.executable,"-m","sherlock_project",user,"--print-found","--folderoutput",str(output)]
    if proxy: args.extend(["--proxy",proxy])
    return args

def execute(users,workers,proxy,formats,output,runner=subprocess.run):
    output.mkdir(parents=True,exist_ok=True)
    def job(user):
        try:
            r=runner(command(user,proxy,output),capture_output=True,text=True,timeout=900,check=False)
            return {"username":user,"exit_code":r.returncode,"stdout":r.stdout or "","stderr":r.stderr or ""}
        except subprocess.TimeoutExpired:
            return {"username":user,"exit_code":124,"stdout":"","stderr":"timeout"}
        except OSError as e:
            return {"username":user,"exit_code":127,"stdout":"","stderr":str(e)}
    with ThreadPoolExecutor(max_workers=workers) as pool:
        rows=list(pool.map(job,users))
    with (output/"run.log").open("a",encoding="utf-8") as f:
        f.write("RUN "+datetime.now(timezone.utc).isoformat()+"\n")
        for row in rows:
            f.write(f"{row['username']} exit={row['exit_code']}\n{row['stdout'][-30000:]}\n{row['stderr'][-10000:]}\n")
    if "json" in formats:
        (output/"summary.json").write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding="utf-8")
    if "csv" in formats:
        with (output/"summary.csv").open("w",encoding="utf-8",newline="") as f:
            w=csv.DictWriter(f,fieldnames=["username","exit_code","stdout","stderr"]);w.writeheader();w.writerows(rows)
    if "txt" in formats:
        (output/"summary.txt").write_text("".join(f"=== {r['username']} exit={r['exit_code']} ===\n{r['stdout']}\n{r['stderr']}\n" for r in rows),encoding="utf-8")
    for r in rows: print(r["username"],"OK" if r["exit_code"]==0 else "ERROR",flush=True)
    return 0 if all(r["exit_code"]==0 for r in rows) else 1

def main(argv=None):
    p=argparse.ArgumentParser()
    p.add_argument("--users",nargs="+",required=True)
    p.add_argument("--workers",type=int,default=3)
    p.add_argument("--proxy",default="")
    p.add_argument("--formats",default="txt,csv,json")
    p.add_argument("--output",default=str(Path.home()/"storage/downloads/SherlockResults"))
    a=p.parse_args(argv)
    try:
        users=validate_users(a.users)
        if not 1<=a.workers<=10:raise ValueError("并发数须为1–10")
        formats=set(a.formats.split(","))
        if not formats or not formats<={"txt","csv","json"}:raise ValueError("导出格式无效")
        if a.proxy and not a.proxy.startswith(("http://","https://","socks5://")):raise ValueError("代理格式无效")
        return execute(users,a.workers,a.proxy,formats,Path(a.output))
    except (ValueError,OSError) as e:
        print("错误:",e,file=sys.stderr);return 2
if __name__=="__main__":raise SystemExit(main())
