#!/system/bin/sh


. $(dirname "$0")/variables.sh

# Utility function for logging
log_message() {
  echo "$1" >> "$LOG_FILE"
}

echo_message() {
  log_message "$1"
  echo "$1"
}

rotate_logs() {
  mkdir -p "$(dirname "$LOG_FILE")"
  rm -rf "$FLAG_FILE"
  max_size=102400 # 100 KB
    if [ -f "$LOG_FILE" ] && [ "$(stat -c%s "$LOG_FILE")" -gt "$max_size" ]; then
      mv "$LOG_FILE" "$(dirname "$LOG_FILE")/$(basename "$LOG_FILE" .log)_$CURRENT_DATE_TIME.log" 2>/dev/null
      log_message "Log file rotated"
    fi
}

detect_system_mount_point() {
  log_message "- Detecting system partition mount point"

  while read device mount_point fs_type options rest; do
    case "$mount_point" in
      /system | /system_root)
        log_message "- Found $mount_point mount point"
        if [ -d "$mount_point/bin" ]; then
          log_message "- Valid $mount_point directory found"
          echo "$device $mount_point $mount_point"
          return
        elif [ -d "$mount_point/system/bin" ]; then
          log_message "- Valid $mount_point/system/bin directory found"
          echo "$device $mount_point $mount_point/system"
          return
        fi
        ;;
    esac
  done < /proc/mounts

  # If no explicit /system mounts, check for root (/) and directories
  while read device mount_point fs_type options rest; do
    case "$mount_point" in
      /)
        log_message "- Found root filesystem mounted at /"
        if [ -d "/system/bin" ]; then
          log_message "- /system directory exists under root filesystem"
          echo "$device / /system"
          return
        elif [ -d "/system_root/bin" ]; then
          log_message "- /system_root/bin directory exists under root filesystem"
          echo "$device / /system_root"
          return
        elif [ -d "/system_root/system/bin" ]; then
          log_message "- /system_root/system directory exists under root filesystem"
          echo "$device / /system_root/system"
          return
        else
          log_message "- Detected system-as-root: system partition is part of /"
          echo "$device / /"
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
  ls /dev/block/mapper/"$partition_name$ACTIVE_SLOT" 2>/dev/null || ls /dev/block/by-name/"$partition_name$ACTIVE_SLOT" 2>/dev/null
}

store_partition_info() {
  partition_name="$1"
  mount_point="$2"
  derived_path="$3"
  write_to_flags "${partition_name}_mount_point" "$mount_point"
  write_to_flags "${partition_name}_derived_path" "$derived_path"
}


check_and_remount_partition() {
  mount_point="$1"
  derived_path="$2"
  partition_name="$3"
  log_message "- Attempting to remount $mount_point"

  # Remount the mount point as read-write
  mount -o rw,remount "$mount_point"
  if [ $? -ne 0 ]; then
    log_message "- Failed to remount $mount_point"
    return 1
  fi

  # Check if the derived path is writable
  if [ -d "$derived_path" ]; then
    touch "$derived_path/.rw" && rm "$derived_path/.rw"
    if [ $? -eq 0 ]; then
      store_partition_info "$partition_name" "$mount_point" "$derived_path"
      echo_message "- $derived_path is writable"
    else
      echo_message "- $derived_path is not writable"
      return 1
    fi
  else
    log_message "- $derived_path does not exist. Cannot check writability."
    return 1
  fi

  log_message "- Successfully remounted and verified $mount_point with derived path $derived_path"
  return 0
}

mount_partition() {
  partition_name="$1"
  mount_point="$2"
  system_mount_point="$3"
  fallback_path="/system/$partition_name"
  derived_path="$mount_point"

  log_message "- Attempting to find and mount $partition_name partition"

  # Detect device path or symlink
  device_path=`get_device_path "$partition_name"`
  log_message "- Device path: $device_path"
  if [ -z "$device_path" ]; then
    log_message "- Unable to find $partition_name partition. Checking for symlink."
    if [ -L "$mount_point" ]; then
      target=$(readlink "$mount_point")
      log_message "- $mount_point is a symlink to $target. Proceeding with $target."
      if [ ! -d "$target" ]; then
        echlog_messageo_message "- Target $target does not exist. Skipping."
        return 1
      fi
    else
      log_message "- $mount_point is not a symlink. Checking fallback path $fallback_path."

      # Check if fallback path exists
      if [ -d "$fallback_path" ] && [ -d "$fallback_path/app" ]; then
        log_message "- Fallback path $fallback_path exists. Using fallback with $system_mount_point."
        mount_point=$system_mount_point
        derived_path=$fallback_path
      else
        log_message "- Neither $partition_name partition nor fallback path found. Skipping."
        return 1
      fi
    fi
  fi

  # If /partition is not in /proc/mounts, adjust mount_point and derived_path
  if ! grep -q "[[:blank:]]$mount_point[[:blank:]]" /proc/mounts; then
    log_message "- $mount_point is not mounted. Using $system_mount_point as mount point for fallback."
    mount_point=$system_mount_point
    derived_path=$fallback_path
  fi

  # Remount the mount point (or fallback) and check writability
  check_and_remount_partition "$mount_point" "$derived_path" "$partition_name"
}

mount_additional_partitions() {
  for partition in product system_ext; do
    mount_partition "$partition" "/$partition" "$1"
  done
}

# Function to check for dynamic partitions
check_dynamic_partitions() {
  DYNAMIC_PARTITIONS=$(getprop ro.boot.dynamic_partitions)
  if [ "$DYNAMIC_PARTITIONS" = "true" ]; then
    log_message "- Dynamic partitions detected."
  else
    log_message "- Dynamic partitions not detected."
  fi
}

# Function to fetch the active slot
fetch_active_slot() {
  ACTIVE_SLOT=$(getprop ro.boot.slot_suffix)
  if [ -z "$ACTIVE_SLOT" ]; then
    ACTIVE_SLOT=$(getprop ro.boot.slot)
  fi
  log_message "- Active slot: $ACTIVE_SLOT"
}

write_to_flags() {
  key="$1"
  value="$2"
  echo "$key=$value" >> "$FLAG_FILE"
}

main() {
  rotate_logs
  log_message "   "
  log_message "$CURRENT_DATE_TIME"
  log_message "   "
  log_message "- Starting partition mounting process"
  check_dynamic_partitions
  fetch_active_slot
  # Detect the system partition
  system_info=`detect_system_mount_point`
  if [ "$system_info" = "ERROR" ]; then
    log_message "- Failed to detect system partition. Exiting."
    exit 1
  fi

  # Parse the detected system info
  system_device=`echo "$system_info" | awk '{print $1}'`
  system_mount_point=`echo "$system_info" | awk '{print $2}'`
  system_derived_path=`echo "$system_info" | awk '{print $3}'`

  # Remount the system partition and check writability of the derived path
  check_and_remount_partition "$system_mount_point" "$system_derived_path" "system"

  # Handle additional partitions
  mount_additional_partitions "$system_mount_point"
  log_message "- Partition mounting process complete"
}

main
