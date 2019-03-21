#!/bin/sh
cd `dirname $0`
echo $@ \
| open_jtalk \
-m ./open_jtalk/nitech_jp_atr503_m001.htsvoice \
-x ./open_jtalk/utf8_dic \
-ow /dev/stdout \
-r 1.0 \
-g 15 \
| aplay --quiet
