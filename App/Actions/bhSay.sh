#!/bin/sh
cd `dirname $0`
echo $1 \
| ./open_jtalk/open_jtalk \
-m ./open_jtalk/takumi_normal.htsvoice \
-x ./open_jtalk/naist-jdic \
-ow $2 \
-g 15 \
-fm 1
