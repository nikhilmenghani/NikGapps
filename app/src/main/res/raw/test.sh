#!/system/bin/sh

# Utility function for logging
log_message() {
  message="$1"
  log_file="/data/local/tmp/mount_script.log"
  echo "$message" >> "$log_file"
}

echo_message() {
  message="$1"
  echo "$message"
}

rotate_logs() {
  log_file="/data/local/tmp/mount_script.log"
  max_size=102400 # 100 KB
  if [ -f "$log_file" ] && [ "$(stat -c%s "$log_file")" -gt "$max_size" ]; then
    mv "$log_file" "${log_file}.old"
    echo_message "Log file rotated"
  fi
}

detect_system_mount_point() {
  log_message "- Detecting system partition mount point"
  while read device mount_point fs_type options rest; do
    case "$mount_point" in
      /)
        log_message "- Found root filesystem mounted at /"
        if [ -d "$mount_point/system" ] || [ -d "$mount_point/bin" ]; then
          log_message "- Detected system-as-root: system partition is part of /"
          echo "$device /"
          return
        fi
        ;;
      /system)
        log_message "- Found /system mount point"
        if [ -d "/system/bin" ]; then
          log_message "- Valid /system directory found"
          echo "$device /system"
          return
        fi
        ;;
      /system_root)
        log_message "- Found /system_root mount point"
        if [ -d "/system_root/system/bin" ]; then
          log_message "- Valid /system_root directory found"
          echo "$device /system_root"
          return
        fi
        ;;
    esac
  done < /proc/mounts
  log_message "- Unable to detect system partition"
  echo "ERROR"
}

get_device_path() {
  partition_name="$1"
  ls /dev/block/mapper/"$partition_name" 2>/dev/null || ls /dev/block/by-name/"$partition_name" 2>/dev/null
}

check_and_remount_partition() {
  mount_point="$1"
  echo_message "- Attempting to remount and check $mount_point"

  mount -o rw,remount "$mount_point"
  if [ $? -ne 0 ]; then
    echo_message "- Failed to remount $mount_point"
    return 1
  fi

  touch "$mount_point/.rw" && rm "$mount_point/.rw"
  if [ $? -eq 0 ]; then
    echo_message "$mount_point is writable"
  else
    echo_message "$mount_point is not writable"
    return 1
  fi
  echo_message "- Successfully remounted and verified $mount_point"
  return 0
}

mount_partition() {
  partition_name="$1"
  mount_point="$2"

  echo_message "- Attempting to find and mount $partition_name partition"
  device_path=`get_device_path "$partition_name"`
  if [ -z "$device_path" ]; then
    echo_message "- Unable to find $partition_name partition. Skipping."
    return 1
  fi

  check_and_remount_partition "$mount_point"
}

mount_additional_partitions() {
  for partition in product system_ext; do
    mount_partition "$partition" "/$partition"
  done
}

main() {
  rotate_logs
  echo_message "Starting partition mounting process"

  system_info=`detect_system_mount_point`
  if [ "$system_info" = "ERROR" ]; then
    echo_message "- Failed to detect system partition. Exiting."
    exit 1
  fi

  system_device=`echo "$system_info" | awk '{print $1}'`
  system_mount_point=`echo "$system_info" | awk '{print $2}'`

  check_and_remount_partition "$system_mount_point"
  mount_additional_partitions
  echo_message "Partition mounting process complete"
}

main
