#! /bin/sh
echo "Building basic.asm"
java -jar ./build/libs/GGASM-1.0.jar asmFiles/basic.asm asmFiles/basic.ulx
echo "Building model.asm"
java -jar ./build/libs/GGASM-1.0.jar asmFiles/model.asm asmFiles/model.ulx
