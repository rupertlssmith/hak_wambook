#!/bin/sh
find . -name 'pom.xml' -exec sed -i -e '
s/<!-- JALOPY_COMMENT_OUT_START -->/<!-- JALOPY_COMMENT_OUT_START/g
s/<!-- JALOPY_COMMENT_OUT_END -->/JALOPY_COMMENT_OUT_END -->/g
' {} \;
