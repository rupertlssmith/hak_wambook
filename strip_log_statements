#!/bin/sh
find . -name '*.java' -exec sed -i -e '
s/private static final java\.util\.logging\.Logger log = java\.util\.logging\.Logger\.getLogger(\(.*\));/\/* private static final Logger log = Logger.getLogger(\1); *\//
/private static final java\.util\.logging\.Logger log =/{
/\/\*/!{
N
s/private static final java\.util\.logging\.Logger log =.* java\.util\.logging\.Logger\.getLogger(\(.*\));/\/* private static final Logger log = Logger.getLogger(\1); *\//
}
}
s/private static final boolean DEBUG = log.isLoggable(java.util.logging.Level.FINE);/private static final boolean DEBUG = false; \/\/log.isLoggable(Level.FINE)/
/\/\*/! s/\(log\.fine.*;\)/\/*\1*\//
/log\.fine/{
/\/\*/!{
N
s/\(log\.fine.*;\)/\/*\1*\//
}
}
/\/\*/! s/\(log\.log.*;\)/\/*\1*\//
/log\.log/{
/\/\*/!{
N
s/\(log\.log.*;\)/\/*\1*\//
}
}' {} \;
