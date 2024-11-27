#!/system/bin/sh

# Utility function for logging
log_message() {
  echo "$1"
}

# Detect system mount point dynamically
detect_system_mount_point() {
  if grep -q "[[:blank:]]/[[:blank:]]" /proc/mounts | grep -q "system"; then
    echo "/"
  elif grep -q "[[:blank:]]/system_root[[:blank:]]" /proc/mounts; then
    echo "/system_root"
  elif grep -q "[[:blank:]]/system/system_root[[:blank:]]" /proc/mounts; then
    echo "/system/system_root"
  else
    echo "/system"
  fi
}

# Mount a partition if it exists or is symlinked
mount_partition() {
  local partition_name=$1
  local mount_point=$2
  local device_path

  log_message "- Attempting to find and mount the $partition_name partition"

  # Check if the mount point exists
  if [ ! -d "$mount_point" ]; then
    log_message "- Mount point $mount_point does not exist. Checking for symlinks."
    # Check if the mount point is a symlink
    if [ -L "$mount_point" ]; then
      local target=$(readlink "$mount_point")
      log_message "- $mount_point is a symlink to $target. Proceeding with target."
      mount_point=$target
    else
      log_message "- Mount point $mount_point is not a symlink. Skipping."
      return 1
    fi
  fi

  # Check if the partition is already mounted
  if grep -q "[[:blank:]]$mount_point[[:blank:]]" /proc/mounts; then
    log_message "- $partition_name is already mounted on $mount_point"
    return 0
  fi

  # Detect the partition device
  device_path=$(ls /dev/block/mapper/$partition_name 2>/dev/null)
  if [ -z "$device_path" ]; then
    device_path=$(ls /dev/block/by-name/$partition_name 2>/dev/null)
  fi

  if [ -z "$device_path" ]; then
    log_message "- Unable to find $partition_name partition or symlink. Skipping."
    return 1
  fi

  # Attempt to mount the partition
  mount -o rw,remount "$device_path" "$mount_point"
  if [ $? -ne 0 ]; then
    log_message "- Failed to mount $partition_name partition at $mount_point"
    return 1
  fi

  log_message "- Successfully mounted $partition_name partition at $mount_point"
  return 0
}

# Main function to handle partitions
main() {
  log_message "Starting partition mounting process"

  # Detect and handle the system partition
  local system_mount_point
  system_mount_point=$(detect_system_mount_point)
  log_message "- Detected system mount point: $system_mount_point"

  if [ "$system_mount_point" = "/" ]; then
    log_message "- System partition is root. Attempting to remount root filesystem."
    mount -o rw,remount /
    if [ $? -eq 0 ]; then
      log_message "- Successfully remounted root filesystem as read-write"
    else
      log_message "- Failed to remount root filesystem"
      exit 1
    fi
  else
    mount_partition "system" "$system_mount_point"
  fi

  # Attempt to mount additional partitions
  mount_partition "product" "/product"
  mount_partition "system_ext" "/system_ext"

  log_message "Partition mounting process complete"
}

# Execute the main function
main
