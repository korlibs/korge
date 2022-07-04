#!/bin/bash
#
# unpack the luaj test archive, compile and run it locally, and repack the results

# unzip existing archive
unzip -n luaj3.0-tests.zip
rm *.lc *.out */*.lc */*.out

# compile tests for compiler and save binary files
for DIR in "lua5.2.1-tests" "regressions"; do
	cd ${DIR}
	FILES=`ls -1 *.lua | awk 'BEGIN { FS="." } ; { print $1 }'`
	for FILE in $FILES ; do
		echo 'compiling' `pwd` $FILE
   		luac ${FILE}.lua
		mv luac.out ${FILE}.lc
	done
	cd ..
done

# run test lua scripts and save output
for DIR in "errors" "perf" "."; do
	cd ${DIR}
	FILES=`ls -1 *.lua | awk 'BEGIN { FS="." } ; { print $1 }'`
	for FILE in $FILES ; do
		echo 'executing' `pwd` $FILE
   		lua ${FILE}.lua JSE > ${FILE}.out
	done
	cd ..
done
cd lua

# create new zipfile
rm -f luaj3.0-tests.zip regressions
zip luaj3.0-tests.zip *.lua *.lc *.out */*.lua */*.lc */*.out

# cleanup
rm *.out */*.lc */*.out
rm -r lua5.2.1-tests
