@echo off
cd /d %~dp0

echo %1 | ^
.\open_jtalk\open_jtalk.exe ^
-m .\open_jtalk\takumi_normal.htsvoice ^
-x .\open_jtalk\sjis_dic ^
-ow %2 ^
-g 15 ^
-fm 1 ^
