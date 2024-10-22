#!/system/bin/sh
src=$1
dest=$2

# Check if the destination partition exists
if [ ! -d "/product" ]; then
  echo "Partition /product does not exist"
  exit 1
else
  echo "Partition /product exists"
fi

# Remount /product as read-write
mount -o rw,remount /product
if [ $? -ne 0 ]; then
  echo "Failed to remount /product as read-write"
  exit 1
else
  echo "Successfully remounted /product as read-write"
fi

# Create necessary directories
mkdir -p $(dirname "$dest")
if [ $? -ne 0 ]; then
  echo "Failed to create directory"
  exit 1
else
  echo "Directory $dest created successfully"
fi

# Copy the file
cp "$src" "$dest"
if [ $? -ne 0 ]; then
  echo "Failed to copy file"
  exit 1
else
  echo "File copied successfully"
fi
