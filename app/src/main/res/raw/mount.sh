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
  ls /dev/block/mapper/"$partition_name" 2>/dev/null || ls /dev/block/by-name/"$partition_name" 2>/dev/null
}

check_and_remount_partition() {
  mount_point="$1"
  derived_path="$2"
  echo_message "- Attempting to remount $mount_point"

  mount -o rw,remount "$mount_point"
  if [ $? -ne 0 ]; then
    echo_message "- Failed to remount $mount_point"
    return 1
  fi

  if [ -d "$derived_path" ]; then
    touch "$derived_path/.rw" && rm "$derived_path/.rw"
    if [ $? -eq 0 ]; then
      echo_message "- $derived_path is writable"
    else
      echo_message "- $derived_path is not writable"
      return 1
    fi
  else
    echo_message "- $derived_path does not exist. Cannot check writability."
    return 1
  fi

  echo_message "- Successfully remounted and verified $mount_point with derived path $derived_path"
  return 0
}

mount_partition() {
  partition_name="$1"
  mount_point="$2"
  fallback_path="/system/$partition_name"
  derived_path="$mount_point"

  echo_message "- Attempting to find and mount $partition_name partition"

  device_path=$(get_device_path "$partition_name")

  if [ -z "$device_path" ]; then
    echo_message "- Unable to find $partition_name partition. Checking for symlink."
    if [ -L "$mount_point" ]; then
      target=$(readlink "$mount_point")
      echo_message "- $mount_point is a symlink to $target. Proceeding with target."
      if [ ! -d "$target" ]; then
        echo_message "- Target $target does not exist. Skipping."
        return 1
      fi
    else
      echo_message "- $mount_point is not a symlink. Checking fallback path $fallback_path."
      if [ -d "$fallback_path" ] && [ -d "$fallback_path/app" ]; then
        echo_message "- Fallback path $fallback_path exists. Using fallback."
        mount_point="/system"
        derived_path="$fallback_path"
      else
        echo_message "- Neither $partition_name partition nor fallback path found. Skipping."
        return 1
      fi
    fi
  fi

  if ! grep -q "[[:blank:]]$mount_point[[:blank:]]" /proc/mounts; then
    echo_message "- $mount_point is not mounted. Using /system as mount point for fallback."
    mount_point="/system"
    derived_path="$fallback_path"
  fi

  check_and_remount_partition "$mount_point" "$derived_path"
}

mount_additional_partitions() {
  for partition in product system_ext; do
    mount_partition "$partition" "/$partition"
  done
}

main() {
  rotate_logs
  echo_message "Starting partition mounting process"

  system_info=$(detect_system_mount_point)
  if [ "$system_info" = "ERROR" ]; then
    echo_message "- Failed to detect system partition. Exiting."
    exit 1
  fi

  system_device=$(echo "$system_info" | awk '{print $1}')
  system_mount_point=$(echo "$system_info" | awk '{print $2}')
  system_derived_path=$(echo "$system_info" | awk '{print $3}')

  check_and_remount_partition "$system_mount_point" "$system_derived_path"
  mount_additional_partitions

  echo_message "Partition mounting process complete"
}

main