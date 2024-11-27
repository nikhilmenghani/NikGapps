#!/system/bin/sh

log_message() {
  echo "$(date +'%Y-%m-%d %H:%M:%S') - $1"
}

check_partitions() {
  partition=$1
  if [ ! -d "$partition" ]; then
    log_message "Partition :$partition: does not exist"
    exit 1
  else
    log_message "Partition :$partition: exists"
  fi
}

remount_partitions() {
  partition=$1
  mount -o rw,remount $partition
  if [ $? -eq 0 ]; then
    log_message "Successfully remounted $partition as read-write"
  else
    log_message "Failed to remount $partition as read-write"
  fi
}

test_writability() {
  partition=$1
  touch "$partition/.rw" && rm "$partition/.rw"
  if [ $? -eq 0 ]; then
    log_message "$partition is writable"
  else
    log_message "$partition is not writable"
  fi
}

partitions="/product /system_ext /"

for partition in $partitions; do
  check_partitions $partition
  remount_partitions $partition
  test_writability $partition
done