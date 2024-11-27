#!/system/bin/sh

partition="/product"
# Test writability
touch "$partition/.rw" && rm "$partition/.rw"
if [ $? -eq 0 ]; then
  echo "$partition is writable"
else
  echo "$partition is not writable"
fi
