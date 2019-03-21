@echo off
cd /d %~dp0

echo %1 | ^
.\open_jtalk\open_jtalk.exe ^
-m .\open_jtalk\nitech_jp_atr503_m001.htsvoice ^
-x .\open_jtalk\sjis_dic ^
-ow %2 ^
-g 1
