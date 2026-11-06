import importlib.util
from pathlib import Path
import tempfile
import json
import unittest
spec=importlib.util.spec_from_file_location("runner",Path(__file__).resolve().parents[1]/"termux/runner.py")
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class Tests(unittest.TestCase):
 def test_validation(self):
  self.assertEqual(m.validate_users(["a","a","b"]),["a","b"])
  for u in ([],["bad/name"],["a"*65],["x"]*51):
   with self.assertRaises(ValueError):m.validate_users(u)
 def test_command(self):
  c=m.command("alice","http://127.0.0.1:8080",Path("/tmp/out"))
  self.assertEqual(c[-2:],["--proxy","http://127.0.0.1:8080"])
 def test_exports(self):
  class R: returncode=0;stdout="ok";stderr=""
  with tempfile.TemporaryDirectory() as t:
   p=Path(t)
   self.assertEqual(m.execute(["alice","bob"],2,"",{"txt","csv","json"},p,runner=lambda *a,**kw:R()),0)
   self.assertEqual(len(json.loads((p/"summary.json").read_text())),2)
   for f in ("summary.txt","summary.csv","run.log"):self.assertTrue((p/f).exists())
 def test_failure(self):
  class R: returncode=1;stdout="";stderr="error"
  with tempfile.TemporaryDirectory() as t:
   self.assertEqual(m.execute(["alice"],1,"",{"json"},Path(t),runner=lambda *a,**kw:R()),1)
if __name__=="__main__":unittest.main()
