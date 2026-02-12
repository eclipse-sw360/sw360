#!/bin/bash

# Fix Thrift 0.22.0 generated code to be compatible with older versions
# This script fixes the ProcessFunction type arguments from 3 to 2
# AsyncProcessFunction should keep 3 arguments, ProcessFunction should have 2

echo "Fixing Thrift generated files for compatibility..."

# Fix ProcessFunction to use exactly 2 type arguments (not AsyncProcessFunction)
find libraries/datahandler/target/generated-sources/thrift -name "*.java" -exec sed -i '' 's/ProcessFunction<I, \? extends org\.apache\.thrift\.TBase>/ProcessFunction<I, org.apache.thrift.TBase>/g' {} \;

# Fix inner class declarations that extend ProcessFunction with 3 type arguments (not AsyncProcessFunction)
find libraries/datahandler/target/generated-sources/thrift -name "*.java" -exec sed -i '' 's/extends org\.apache\.thrift\.ProcessFunction<I, [^,>]*, [^>]*>/extends org.apache.thrift.ProcessFunction<I, org.apache.thrift.TBase>/g' {} \;

echo "Fixed Thrift generated files for compatibility"
